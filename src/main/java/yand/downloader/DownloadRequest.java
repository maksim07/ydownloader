package yand.downloader;

import java.io.Serializable;
import java.net.URL;

/**
 * Request is a bunch of source uri's from which data has to be loaded.
 *
 * @author Max Osipov
 */
public final class DownloadRequest implements Serializable {

    /**
     * Resources to load
     */
    private final URL[] resources;

    public DownloadRequest(URL[] resources) {
        this.resources = resources;
    }

    public URL[] getResources() {
        return resources;
    }
}
