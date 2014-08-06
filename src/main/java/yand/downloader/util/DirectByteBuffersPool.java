package yand.downloader.util;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unbounded pool of byte buffers. It allows to reduce cost of pool allocations.
 * <code>
 *     ByteBuffer buffer = pool.start();
 *     try {
 *         // do some work with the buffer
 *     }
 *     finally {
 *         pool.finish(buffer);
 *     }
 *     catch
 * </code>
 *
 * @author Max Osipov
 */
public class DirectByteBuffersPool {

    /**
     * Size of buffers in bytes
     */
    private final int bufferSize;

    /**
     * Size of the poll
     */
    private final int poolSize;

    /**
     * Buffers cache
     */
    private final ConcurrentLinkedQueue<ByteBuffer> buffers;

    /**
     * Current count of pool
     */
    private final AtomicInteger count;


    public DirectByteBuffersPool(int bufferSize, int poolSize) {
        this.bufferSize = bufferSize;
        this.poolSize = poolSize;
        this.buffers = new ConcurrentLinkedQueue<>();
        this.count = new AtomicInteger(poolSize);
    }

    public ByteBuffer start() {

        // decrease count until zero
        int c, nc;
        do {
            c = count.get();
            nc = c - 1;
            if (nc < 0) break;
        } while (!count.compareAndSet(c, nc));

        ByteBuffer ret = buffers.poll();
        if (ret == null)
            return ByteBuffer.allocateDirect(bufferSize);
        else
            return (ByteBuffer) ret.clear();
    }

    public void finish(ByteBuffer buffer) {
        // return buffer to pool if its current size is less then poolSize
        int c, nc;
        do {
            c = count.get();
            nc = c + 1;
            if (nc > poolSize) return;
        } while (!count.compareAndSet(c, nc));
        buffers.add(buffer);
    }

    public int size() {
        return count.get();
    }
}
