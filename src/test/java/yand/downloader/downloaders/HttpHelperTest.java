package yand.downloader.downloaders;

import org.junit.Test;
import yand.downloader.util.HttpHelper;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class HttpHelperTest {

    @Test
    public void testReadLine() throws Exception {
        byte[] test1 = new byte[]{'t', '1'};
        byte[] test2 = new byte[]{'t', '2'};
        ByteBuffer buffer = ByteBuffer.allocate(test1.length + test2.length + 2 * HttpHelper.CRLF.length);

        buffer.put(test1);
        buffer.put(HttpHelper.CRLF);
        buffer.put(test2);
        buffer.put(HttpHelper.CRLF);

        buffer.flip();

        String t1 = HttpHelper.readLine(buffer);
        String t2 = HttpHelper.readLine(buffer);
        String t3 = HttpHelper.readLine(buffer);

        assertEquals(t1, "t1");
        assertEquals(t2, "t2");
        assertNull(t3);
    }
}