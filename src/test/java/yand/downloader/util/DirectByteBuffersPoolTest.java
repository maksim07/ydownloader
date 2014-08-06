package yand.downloader.util;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class DirectByteBuffersPoolTest {

    @Test
    public void testSize() throws Exception {

        DirectByteBuffersPool pool = new DirectByteBuffersPool(1024, 10);
        List<ByteBuffer> buffers = new ArrayList<>(20);

        for (int i = 0; i < 20; i ++) {
            ByteBuffer buffer = pool.start();
            assertNotNull(buffer);
            buffers.add(buffer);
        }

        assertEquals(pool.size(), 0);

        for (ByteBuffer buffer : buffers) {
            pool.finish(buffer);
        }

        assertEquals(pool.size(), 10);
    }
}