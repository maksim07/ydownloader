package yand.downloader;

import yand.downloader.impl.CompositeDownloadManager;
import yand.downloader.impl.HttpDownloadManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for download manager
 *
 * @author Max Osipov
 */
public class DownloadManagers {

    /**
     * Method creates download manager suitable for most purposes (actually just for downloading http sources)
     *
     * @return download manager
     */
    public static DownloadManager createDefault() {

        // this implementation supports only http scheme
        Map<String, DownloadManager> registry = new HashMap<>();
        try {
            registry.put("http", HttpDownloadManager.create(100));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new CompositeDownloadManager(registry);
    }
}
