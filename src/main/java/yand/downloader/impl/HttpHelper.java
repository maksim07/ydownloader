package yand.downloader.impl;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/**
 * @author Max Osipov
 */
public class HttpHelper {
    public static final byte CR = 13;
    public static final byte LF = 10;
    public static final byte SP = 32;
    public static final byte[] CRLF = new byte[]{CR, LF};

    public static final Charset CHARSET = Charset.forName("utf-8");


    public static final byte[] HTTP_GET_METHOD = new byte[] {'G', 'E', 'T', SP, '/'};
    public static final byte[] HTTP_VERSION = new byte[] {SP, 'H', 'T', 'T', 'P', '/', '1', '.', '0'};

    public static void writeHttpGet(URL url, SocketChannel channel) throws IOException {

        StringBuilder urlBuilder = new StringBuilder();
        if (url.getPath().startsWith("/"))
            urlBuilder.append(url.getPath().substring(1));
        else
            urlBuilder.append(url.getPath());

        if (url.getQuery() != null && url.getQuery().length() > 0) {
            urlBuilder.append('?').append(url.getQuery());
        }

        char[] headerUrl = urlBuilder.toString().toCharArray();

        ByteBuffer buffer = ByteBuffer.allocate(HTTP_GET_METHOD.length + HTTP_VERSION.length + headerUrl.length + 2 * CRLF.length);

        buffer.put(HTTP_GET_METHOD);
        for (char c : headerUrl)
            buffer.put((byte) c);

        buffer.put(HTTP_VERSION);
        buffer.put(CRLF);
        buffer.put(CRLF);

        buffer.flip();
        channel.write(buffer);
    }

    /**
     * Method tries to read line from byte buffer. It returns null if there is no CRLF in the buffer
     *
     * @param buffer buffer to read
     * @return string on null
     */
    public static String readLine(ByteBuffer buffer) {
        int remaining = buffer.remaining();
        if (remaining == 0)
            return null;

        buffer.mark();
        byte prev = buffer.get();
        for (int i = 1; i < remaining; i ++) {
            byte cur = buffer.get();
            if (cur == LF && prev == CR) {
                buffer.reset();
                byte[] array = new byte[i + 1];
                buffer.get(array);
                return new String(array, 0, array.length - CRLF.length, CHARSET);
            }
            prev = cur;
        }

        buffer.reset();
        return null;
    }
}
