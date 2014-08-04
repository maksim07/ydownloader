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
     * File containing downloaded data
     */
    private final File file;

    public DownloadResponseItem(URL uri, File file) {
        this.uri = uri;
        this.file = file;
    }

    public URL getUrl() {
        return uri;
    }


    public File getFile() {
        return file;
    }
}
