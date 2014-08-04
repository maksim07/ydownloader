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

    public static DownloadManager createDefault() {

        // this implementation supports only http scheme
        Map<String, DownloadManager> registry = new HashMap<String, DownloadManager>();
        try {
            registry.put("http", HttpDownloadManager.create());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new CompositeDownloadManager(registry);
    }
}
