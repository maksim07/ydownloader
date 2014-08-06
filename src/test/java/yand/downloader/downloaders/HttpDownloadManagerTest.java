package yand.downloader.downloaders;

import yand.downloader.*;

import java.io.*;

public class HttpDownloadManagerTest extends DownloadManagerTest {

    @Override
    public DownloadManager createManager() throws IOException {
        return HttpDownloadManager.create(10);
    }
}