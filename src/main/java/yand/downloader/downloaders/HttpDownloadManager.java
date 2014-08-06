package yand.downloader.downloaders;

import yand.downloader.*;
import yand.downloader.util.DirectByteBuffersPool;

import java.io.IOException;
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
    private volatile Selector selector;

    /**
     * Thread for selecting channel events
     */
    private final Thread thread;

    /**
     * Queue with download requests
     */
    private final ConcurrentLinkedQueue<HttpDownloadController> tasks;

    /**
     * Started flag
     */
    private volatile boolean started;

    /**
     * Thread pool to execute tasks
     */
    private final ExecutorService executor;

    private final DirectByteBuffersPool pool;

    public static void main(String[] args) throws URISyntaxException, DownloadException, IOException, ExecutionException, InterruptedException {
        HttpDownloadManager manager = HttpDownloadManager.create(20, 20, 2 * 1024 * 1024);

        URL[] urls = new URL[args.length];
        for (int i = 0; i < args.length; i++)
            urls[i] = new URL(args[i]);

        DownloadController controller = manager.download(new DownloadRequest(urls));
        DownloadResponse response = controller.get();
        manager.stop();
    }

    /**
     * Constructor
     */
    private HttpDownloadManager(int threadPoolSize, int buffersPoolSize, int buffersSize) {

        ThreadFactory tf = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread ret = new Thread(r);
                ret.setDaemon(true);
                return ret;
            }
        };

        this.executor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(threadPoolSize),
                tf, new ThreadPoolExecutor.CallerRunsPolicy());
        this.tasks = new ConcurrentLinkedQueue<>();
        this.thread = new Thread(new SelectorEventsConsumer());
        this.thread.setDaemon(true);
        this.pool = new DirectByteBuffersPool(buffersSize, buffersPoolSize);
    }

    public static HttpDownloadManager create(int threadPoolSize, int buffersPoolSize, int buffersSize) throws IOException {
        HttpDownloadManager ret = new HttpDownloadManager(threadPoolSize, buffersPoolSize, buffersSize);
        ret.selector = Selector.open();
        ret.started = true;
        ret.thread.start();

        return ret;
    }

    public void stop() {
        started = false;
        selector.wakeup();
    }

    @Override
    public DownloadController download(DownloadRequest request) throws DownloadException {

        try (HttpDownloadController ret = new HttpDownloadController(selector, request, pool)) {
            tasks.add(ret);
            selector.wakeup();
            return ret;
        } catch (IOException e) {
            throw new DownloadException("Unable to register", e);
        }
    }

    /**
     * Consumer of events
     */
    class SelectorEventsConsumer implements Runnable {

        @Override
        public void run() {
            while (started) {

                int channelsCount = 0;
                try {
                    channelsCount = selector.select();
                } catch (IOException e) {
                    e.printStackTrace();
                    // TODO: catch it
                }

                // poll queue for new registrations
                HttpDownloadController controller;
                while ((controller = tasks.poll()) != null) {
                    controller.register();
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
            }
        }


        /**
         * Processes event
         *
         * @param key selected key
         */
        private void processEvent(final SelectionKey key) {
            final HttpDownloadTask task = (HttpDownloadTask) key.attachment();
            final SocketChannel channel = (SocketChannel) key.channel();

            try {
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
                                task.getController().readable(key);
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
                            task.getController().writable(key);
                        }
                    });
                }
            }
            catch (IOException e) {
                task.getController().error(e);
            }
        }

    }


}
