package yand.downloader.impl;

import yand.downloader.DownloadRequest;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

/**
 * @author Max Osipov
 */
public class HttpDownloadTask {

    /**
     * Original request
     */
    private final DownloadRequest request;

    /**
     * Http url to download
     */
    private final URL url;

    /**
     * Output file channel
     */
    private volatile FileChannel fchannel;

    /**
     * If the request was performed
     */
    private volatile boolean requested;


    HttpDownloadTask(DownloadRequest request, URL url) throws IOException {
        this.request = request;
        this.url = url;
    }

    void readable(SocketChannel channel) throws IOException {

        // file channel lazy initialization
        if (fchannel == null) {
            File tmpFile = File.createTempFile("yd_", ".data");
            System.out.println("File created " + tmpFile.getAbsolutePath());
            RandomAccessFile file = new RandomAccessFile(tmpFile, "rw");
            this.fchannel = file.getChannel();
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(2 * 1024 * 1024);

        int bytesRead = channel.read(buffer);
        while (bytesRead > 0) {
            buffer.flip();
            fchannel.write(buffer);
            buffer.clear();

            bytesRead = channel.read(buffer);
        }

        if (bytesRead == -1) {
            channel.close();
            fchannel.close();
            System.out.println("Channel is closed");
        }

    }

    boolean writable(SocketChannel channel) throws IOException {

        if (!requested) {

            ByteBuffer buffer = ByteBuffer.allocate(1000);
            String req = "GET /" + url.getPath() + " HTTP/1.1\r\n" +
                    "Host: " + url.getHost() + "\r\n" +
                    "Connection: close\r\n\r\n";
            for (char c : req.toCharArray())
                buffer.put((byte) c);

            buffer.flip();
            channel.write(buffer);
            requested = true;

        }

        return true;
    }


    public URL getUrl() {
        return url;
    }

    public DownloadRequest getRequest() {
        return request;
    }
}
