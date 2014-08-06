package yand.downloader.downloaders;

import yand.downloader.DownloadManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CompositeDownloadManagerTest extends DownloadManagerTest {

    @Override
    public DownloadManager createManager() throws IOException {
        Map<String, DownloadManager> registry = new HashMap<>();
        registry.put("http", HttpDownloadManager.create(10));

        return new CompositeDownloadManager(registry);
    }
}