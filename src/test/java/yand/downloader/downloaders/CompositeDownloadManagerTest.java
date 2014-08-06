package yand.downloader.downloaders;

import yand.downloader.DownloadManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CompositeDownloadManagerTest extends DownloadManagerTest {

    private static final int DEFAULT_POOL_SIZE = 20;

    private static final int DEFAULT_BUFFERS_POOL_SIZE = 20;

    private static final int DEFAULT_BUFFER_SIZE = 2 * 1024 * 1024;

    @Override
    public DownloadManager createManager() throws IOException {
        Map<String, DownloadManager> registry = new HashMap<>();
        registry.put("http", HttpDownloadManager.create(DEFAULT_POOL_SIZE, DEFAULT_BUFFERS_POOL_SIZE, DEFAULT_BUFFER_SIZE));

        return new CompositeDownloadManager(registry);
    }
}