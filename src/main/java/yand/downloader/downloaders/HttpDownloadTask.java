package yand.downloader.downloaders;

import yand.downloader.util.HttpHelper;

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
 * Class represents individual url downloading tasks.
 * The particular instance of the class can be used in just one thread at the moment. But it can be used
 * by different threads in different moments of time. For the purpose of
 * changes visibility here all non-final variables are declared as volatile.
 *
 * @author Max Osipov
 */
class HttpDownloadTask implements Closeable {

    /**
     * Link to parent controller
     */
    private final HttpDownloadController controller;

    /**
     * Channel to read data from
     */
    private final SocketChannel channel;

    /**
     * Selection key with which channel was registered in selector
     */
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

    /**
     * If set to true, than http header has been already read
     */
    private volatile boolean body;



    HttpDownloadTask(HttpDownloadController controller, SocketChannel channel, URL url) {
        this.controller = controller;
        this.channel = channel;
        this.url = url;
    }

    /**
     * Callback from controller which means that the task has been registered in selector
     *
     * @param key registration key
     * @throws IOException
     */
    void registered(SelectionKey key) throws IOException {
        this.key = key;

        this.file = File.createTempFile("yd_", ".data");
        RandomAccessFile rfile = new RandomAccessFile(this.file, "rw");
        this.fchannel = rfile.getChannel();
    }

    @Override
    public void close() throws IOException {

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

    void readable() throws IOException {

        ByteBuffer buffer = controller.getPool().start();

        try {
            int bytesRead = channel.read(buffer);
            while (bytesRead > 0) {
                buffer.flip();

                // if we have not read header yet than read it
                while (!body && bytesRead > 0) {
                    String line;
                    do {
                        line = HttpHelper.readLine(buffer);
                    } while (line != null && line.length() > 0);
                    body = line != null && line.length() == 0;

                    // if the header is too large to fit in buffer
                    if (!body) {
                        buffer.clear();
                        bytesRead = channel.read(buffer);
                        buffer.flip();
                    }
                }

                if (bytesRead <= 0)
                    break;

                fchannel.write(buffer);
                buffer.clear();

                bytesRead = channel.read(buffer);
            }


            if (bytesRead == -1) {
                close();
                controller.onTaskClose(this);
            }
        }
        finally {
            controller.getPool().finish(buffer);
        }
    }


    void writable() throws IOException {
        // write request only once
        if (!requested) {
            HttpHelper.writeHttpGet(url, channel);
            requested = true;
        }
    }


    URL getUrl() {
        return url;
    }

    HttpDownloadController getController() {
        return controller;
    }

    SelectionKey getKey() {
        return key;
    }

    File getFile() {
        return file;
    }
}
