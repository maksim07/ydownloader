package yand.downloader.impl;

import yand.downloader.DownloadController;
import yand.downloader.DownloadRequest;
import yand.downloader.DownloadResponse;
import yand.downloader.DownloadResponseItem;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Http downloader controller implementation
 *
 * @author Max Osipov
 */
public class HttpDownloadController implements DownloadController, Closeable {

    /**
     * Reference to selector which multiplexing the channels of the request
     */
    private final Selector selector;

    /**
     * Original request
     */
    private final DownloadRequest request;

    /**
     * Set with tasks for each url from original request
     */
    private CopyOnWriteArraySet<HttpDownloadTask> tasks;

    /**
     * Error flag
     */
    private volatile boolean error;

    /**
     * Thrown exception
     */
    private Throwable cause;

    /**
     * Latch to wait for result
     */
    private final CountDownLatch latch;


    HttpDownloadController(Selector selector, DownloadRequest request) {
        this.selector = selector;
        this.request = request;
        this.tasks = new CopyOnWriteArraySet<>();
        this.latch = new CountDownLatch(request.getResources().length);
    }

    void register() {
        try {
            for (URL url : request.getResources()) {
                String host = url.getHost();
                int port = url.getPort();

                InetSocketAddress address = new InetSocketAddress(host, port > 0 ? port : 80);
                SocketChannel socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                socketChannel.connect(address);

                HttpDownloadTask task = new HttpDownloadTask(this, socketChannel, url);
                task.registered(socketChannel.register(selector,
                        SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE, task));

                tasks.add(task);


                System.out.println("Controller is registered for " + url);
            }
        } catch (Exception e) {
            error(e);
        }
    }

    void error(Throwable cause) {
        try {
            close();
        } catch (IOException e) {
            cause.addSuppressed(e);
        }
        //cause.printStackTrace();

        this.error = true;
        this.cause = cause;

        for (int i = 0; i < request.getResources().length; i++)
            latch.countDown();
    }

    @Override
    public void close() throws IOException {
        IOException first = null;
        for (HttpDownloadTask task : tasks) {
            try {
                task.close();
            } catch (IOException e) {
                if (first == null) first = e;
                else first.addSuppressed(e);
            }
        }

        if (first != null)
            throw first;
    }

    @Override
    public DownloadingStatus status() {
        return null;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public DownloadResponse get() throws InterruptedException, ExecutionException {
        latch.await();
        return makeResponse();
    }

    @Override
    public DownloadResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        latch.await(timeout, unit);
        return makeResponse();
    }

    private DownloadResponse makeResponse() throws ExecutionException {
        if (error)
            throw new ExecutionException(cause);
        else {
            Set<DownloadResponseItem> items = new HashSet<>();
            for (HttpDownloadTask task : tasks) {
                items.add(new DownloadResponseItem(task.getUrl(), task.getFile()));
            }
            return new DownloadResponse(request, items);
        }
    }

    void onTaskClose(HttpDownloadTask task) {
        latch.countDown();
    }


    void readable(SelectionKey key) {
        try {
            HttpDownloadTask task = (HttpDownloadTask) key.attachment();
            task.readable();
        } catch (IOException e) {
            error(e);
        }
    }

    void writable(SelectionKey key) {
        try {
            HttpDownloadTask task = (HttpDownloadTask) key.attachment();
            task.writable();
        } catch (IOException e) {
            error(e);
        }
    }

}
