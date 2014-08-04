package yand.downloader;

import java.io.File;
import java.io.Serializable;
import java.net.URL;

/**
 * Response part related to particular uri
 *
 * @author Max Osipov
 */
public final class DownloadResponseItem implements Serializable {

    /**
     * Source uri
     */
    private final URL uri;

    /**
     * Flag means that this uri was successfully loaded
     */
    private final boolean success;

    /**
     * If an error occurred
     */
    private final DownloadException cause;

    /**
     * File containing downloaded data
     */
    private final File file;

    protected DownloadResponseItem(URL uri, File file) {
        this.uri = uri;
        this.success = true;
        this.cause = null;
        this.file = file;
    }

    protected DownloadResponseItem(URL uri, DownloadException cause) {
        this.uri = uri;
        this.success = false;
        this.cause = cause;
        this.file = null;
    }

    public URL getUrl() {
        return uri;
    }

    public boolean isSuccess() {
        return success;
    }

    public File getFile() throws DownloadException {
        if (!success)
            throw cause;

        return file;
    }
}
