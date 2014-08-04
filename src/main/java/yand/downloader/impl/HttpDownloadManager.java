package yand.downloader.impl;

import yand.downloader.DownloadController;
import yand.downloader.DownloadException;
import yand.downloader.DownloadManager;
import yand.downloader.DownloadRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Manager for loading from http sources
 *
 * @author Max Osipov
 */
public class HttpDownloadManager implements DownloadManager {

    /**
     * NIO selector
     */
    private final Selector selector;

    /**
     * Thread for selecting channel events
     */
    private final Thread thread;

    /**
     * Queue with download requests
     */
    private final ConcurrentLinkedQueue<DownloadRequest> tasks;

    /**
     * Started flag
     */
    private volatile boolean started;

    /**
     * Thread pool to execute tasks
     */
    private ExecutorService executor;


    public static void main(String[] args) throws URISyntaxException, DownloadException, IOException {
        HttpDownloadManager manager = HttpDownloadManager.create();
        URL[] urls = new URL[args.length];
        for (int i = 0; i < args.length; i++)
            urls[i] = new URL(args[i]);

        manager.download(new DownloadRequest(urls));
    }

    private HttpDownloadManager() throws IOException {
        this.executor = new ThreadPoolExecutor(20, 20, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(20),
                new ThreadPoolExecutor.CallerRunsPolicy());
        this.selector = Selector.open();
        this.tasks = new ConcurrentLinkedQueue<DownloadRequest>();
        this.thread = new Thread(new SelectorEventsConsumer());
    }

    public static HttpDownloadManager create() throws IOException {
        HttpDownloadManager ret = new HttpDownloadManager();
        ret.start();
        return ret;
    }

    private void start() {
        started = true;
        thread.start();
    }

    private void stop() {
        started = false;
        selector.wakeup();
    }

    @Override
    public DownloadController download(DownloadRequest request) throws DownloadException {
        tasks.add(request);
        selector.wakeup();
        return null;
    }

    /**
     * Consumer of events
     */
    class SelectorEventsConsumer implements Runnable {

        @Override
        public void run() {
            while (started) {
                try {
                    int channelsCount = selector.select();

                    // poll queue for new registrations
                    DownloadRequest request;
                    while ((request = tasks.poll()) != null) {

                        for (URL url : request.getResources()) {
                            registerURL(request, url);
                        }
                    }

                    // if there are no events
                    if (channelsCount == 0)
                        continue;

                    // iterate over selected keys
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                    while (keyIterator.hasNext()) {
                        processEvent(keyIterator.next());
                        keyIterator.remove();
                    }

                } catch (IOException e) {
                    // TODO: catch
                    e.printStackTrace();
                }
            }
        }

        private void registerURL(DownloadRequest request, URL url) throws IOException {
            HttpDownloadTask task = new HttpDownloadTask(request, url);
            String host = url.getHost();
            int port = url.getPort();

            InetSocketAddress address = new InetSocketAddress(host, port > 0 ? port : 80);
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(address);

            socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE, task);
        }

        private void processEvent(final SelectionKey key) throws IOException {
            final HttpDownloadTask finalTask = (HttpDownloadTask) key.attachment();
            final SocketChannel channel = (SocketChannel) key.channel();

            if (key.isConnectable()) {
                // if connection is established - stop listening for connection events
                if (channel.finishConnect()) {
                    key.interestOps(key.interestOps() & (~SelectionKey.OP_CONNECT));
                }
            } else if (key.isReadable()) {

                // stop listening for read events
                key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));

                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            finalTask.readable(channel);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                            selector.wakeup();
                        }
                    }
                });
            } else if (key.isWritable()) {
                key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            finalTask.writable(channel);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }

    }


}
