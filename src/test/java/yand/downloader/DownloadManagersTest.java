package yand.downloader;

import yand.downloader.downloaders.DownloadManagerTest;

import java.io.IOException;

public class DownloadManagersTest extends DownloadManagerTest {

    @Override
    public DownloadManager createManager() throws IOException {
        return DownloadManagers.createDefault();
    }
}