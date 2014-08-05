package yand.downloader.impl;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author Max Osipov
 */
public class HttpHelper {
    public static final byte CR = 13;
    public static final byte LF = 10;
    public static final byte SP = 32;
    public static final byte[] CRLF = new byte[]{CR, LF};


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

}
