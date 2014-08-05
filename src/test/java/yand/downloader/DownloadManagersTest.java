package yand.downloader;

import yand.downloader.impl.DownloadManagerTest;

import java.io.IOException;

import static org.junit.Assert.*;

public class DownloadManagersTest extends DownloadManagerTest {

    @Override
    public DownloadManager createManager() throws IOException {
        return DownloadManagers.createDefault();
    }
}