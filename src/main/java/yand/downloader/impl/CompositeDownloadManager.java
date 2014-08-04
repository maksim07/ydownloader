package yand.downloader.impl;

import yand.downloader.DownloadController;
import yand.downloader.DownloadException;
import yand.downloader.DownloadManager;
import yand.downloader.DownloadRequest;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Download manager composed of managers for each distinct protocol scheme
 *
 * @author Max Osipov
 */
public class CompositeDownloadManager implements DownloadManager {

    /**
     * Registry of managers
     */
    private final Map<String, DownloadManager> registry;

    public CompositeDownloadManager(Map<String, DownloadManager> registry) {
        this.registry = registry;
    }

    @Override
    public DownloadController download(DownloadRequest request) throws DownloadException {

        // split incoming request to requests correspond to different uri schemes
        Map<String, DownloadRequestSplit> splits = new HashMap<String, DownloadRequestSplit>();
        for(URL url : request.getResources()) {
            String scheme = url.getProtocol();
            DownloadManager dm = registry.get(scheme);
            if (dm == null)
                throw new DownloadException("Such scheme is not supported by the downloader: " + scheme);

            DownloadRequestSplit split = splits.get(url.getProtocol());
            if (split == null) {
                split = new DownloadRequestSplit(dm);
                splits.put(scheme, split);
            }

            split.add(url);
        }

        // launch each split download
        for (DownloadRequestSplit split : splits.values()) {
            List<URL> urls = split.getUrls();
            split.getManager().download(new DownloadRequest(urls.toArray(new URL[urls.size()])));
        }

        return null;
    }

    private static class DownloadRequestSplit {
        private DownloadManager manager;
        private List<URL> urls;

        private DownloadRequestSplit(DownloadManager manager) {
            this.manager = manager;
            this.urls = new ArrayList<URL>();
        }

        private void add(URL uri) {
            this.urls.add(uri);
        }

        public DownloadManager getManager() {
            return manager;
        }

        public List<URL> getUrls() {
            return urls;
        }
    }
}
