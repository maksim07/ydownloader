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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Http downloader controller implementation
 *
 * @author Max Osipov
 */
public class HttpDownloadController implements DownloadController, Closeable {

    /**
     * Current status of download
     */
    private volatile DownloadingStatus status;

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
    private final CopyOnWriteArraySet<HttpDownloadTask> tasks;

    /**
     * Thrown exception
     */
    private volatile Throwable cause;

    /**
     * Latch to wait for result
     */
    private final CountDownLatch latch;

    /**
     * Counter for uncompleted tasks
     */
    private final AtomicInteger uncomletedTasksCount;


    HttpDownloadController(Selector selector, DownloadRequest request) {
        this.selector = selector;
        this.request = request;
        this.tasks = new CopyOnWriteArraySet<>();
        this.latch = new CountDownLatch(request.getResources().length);
        this.status = DownloadingStatus.STARTING;
        this.uncomletedTasksCount = new AtomicInteger(request.getResources().length);
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
                this.status = DownloadingStatus.RUNNING;
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

        this.cause = cause;
        this.status = DownloadingStatus.ERROR;

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
        return status;
    }

    @Override
    public void pause() {
        if (isDone())
            return;

        // stop listening for reads
        for (HttpDownloadTask task : tasks) {
            SelectionKey key = task.getKey();
            key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
        }
        status = DownloadingStatus.PAUSED;
    }

    @Override
    public void resume() {
        if (isDone())
            return;

        // start listening for reads back
        for (HttpDownloadTask task : tasks) {
            SelectionKey key = task.getKey();
            key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
        }
        status = DownloadingStatus.RUNNING;
        selector.wakeup();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone())
            return false;

        try {
            close();
            this.status = DownloadingStatus.CANCELED;
            return isDone();
        } catch (IOException e) {
            error(e);
            return false;
        }
    }

    @Override
    public boolean isCancelled() {
        return status == DownloadingStatus.CANCELED;
    }

    @Override
    public boolean isDone() {
        DownloadingStatus status = this.status;
        return status == DownloadingStatus.CANCELED || status == DownloadingStatus.ERROR || status == DownloadingStatus.SUCCESS;
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
        if (status == DownloadingStatus.ERROR)
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
        if (uncomletedTasksCount.decrementAndGet() == 0) {
            this.status = DownloadingStatus.SUCCESS;
        }
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
