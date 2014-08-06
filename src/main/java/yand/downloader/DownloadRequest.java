package yand.downloader;

import java.io.Serializable;
import java.net.URL;

/**
 * Request is a bunch of source url's from which data has to be loaded. It is either successful or not as a whole.
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

    public DownloadRequest(URL resource) {
        this.resources = new URL[]{resource};
    }
    public URL[] getResources() {
        return resources;
    }

    public DownloadRequest add(URL url) {
        URL[] nurls = new URL[resources.length + 1];
        System.arraycopy(resources, 0, nurls, 0, resources.length);
        nurls[resources.length] = url;
        return new DownloadRequest(nurls);
    }
}
