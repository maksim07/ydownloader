package yand.downloader.impl;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author Max Osipov
 */
class HttpDownloadTask implements Closeable {

    private final HttpDownloadController controller;

    private final SocketChannel channel;

    private volatile SelectionKey key;

    /**
     * Http url to download
     */
    private final URL url;

    /**
     * Output file channel
     */
    private volatile FileChannel fchannel;

    /**
     * File with downloaded data
     */
    private volatile File file;

    /**
     * If the request was performed
     */
    private volatile boolean requested;


    HttpDownloadTask(HttpDownloadController controller, SocketChannel channel, URL url) {
        this.controller = controller;
        this.channel = channel;
        this.url = url;
    }

    void registered(SelectionKey key) throws IOException {
        this.key = key;

        this.file = File.createTempFile("yd_", ".data");
        System.out.println("File created " + file.getAbsolutePath());
        RandomAccessFile rfile = new RandomAccessFile(this.file, "rw");
        this.fchannel = rfile.getChannel();
    }

    @Override
    public void close() throws IOException {

        System.out.println("Closing");
        key.cancel();

        IOException first = null;
        try {
            channel.close();
        } catch (IOException e) {
            first = e;
        }

        try {
            fchannel.close();
        } catch (IOException e) {
            if (first != null)
                e.addSuppressed(first);
        }

        if (first != null)
            throw first;
    }

    void connectable() throws IOException {
        if (channel.finishConnect()) {
            key.interestOps(key.interestOps() & (~SelectionKey.OP_CONNECT));
        }
    }

    void readable() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(2 * 1024 * 1024);

        int bytesRead = channel.read(buffer);
        while (bytesRead > 0) {
            buffer.flip();
            fchannel.write(buffer);
            buffer.clear();

            bytesRead = channel.read(buffer);
        }

        if (bytesRead == -1) {
            close();
            controller.onTaskClose(this);
        }
    }


    void writable() throws IOException {

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
    }


    URL getUrl() {
        return url;
    }

    HttpDownloadController getController() {
        return controller;
    }

    SocketChannel getChannel() {
        return channel;
    }

    FileChannel getFchannel() {
        return fchannel;
    }

    SelectionKey getKey() {
        return key;
    }

    File getFile() {
        return file;
    }
}
