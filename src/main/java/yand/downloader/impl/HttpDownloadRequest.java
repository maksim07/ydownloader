package yand.downloader.impl;

import yand.downloader.DownloadController;
import yand.downloader.DownloadRequest;
import yand.downloader.DownloadResponse;

import java.nio.channels.SelectionKey;
import java.util.concurrent.*;

/**
 * @author Max Osipov
 */
public class HttpDownloadRequest implements DownloadController {

    private final DownloadRequest original;

    private final SelectionKey[] keys;

    private volatile boolean paused;

    private volatile boolean calceled;

    private final CountDownLatch latch;

    private final ConcurrentLinkedQueue<HttpDownloadTask> tasks;

    HttpDownloadRequest(DownloadRequest original, SelectionKey[] keys) {
        this.original = original;
        this.keys = keys;
        this.latch = new CountDownLatch(keys.length);
        this.tasks = new ConcurrentLinkedQueue<HttpDownloadTask>();
    }

    public DownloadRequest getOriginal() {
        return original;
    }

    public boolean isPaused() {
        return paused;
    }

    public SelectionKey[] getKeys() {
        return keys;
    }

    @Override
    public void pause() {
        for (SelectionKey key : keys) {
            key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
        }
    }

    @Override
    public void resume() {
        for (SelectionKey key : keys) {
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
        }
    }
    @Override
    public DownloadingStatus status() {
        return null;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        for (SelectionKey key : keys) {
            key.cancel();
        }
        calceled = true;
        return calceled;
    }

    @Override
    public boolean isCancelled() {
        return calceled;
    }

    @Override
    public boolean isDone() {
        for (SelectionKey key : keys)
            if (key.channel().isOpen())
                return false;

        return true;
    }

    @Override
    public DownloadResponse get() throws InterruptedException, ExecutionException {
        latch.await();
        return null;
    }

    @Override
    public DownloadResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        latch.await(timeout, unit);
        return null;
    }

    /**
     * Called by task on closing the connection
     *
     * @param task task
     */
    void loaded(HttpDownloadTask task) {
        tasks.add(task);
        latch.countDown();
    }
}
