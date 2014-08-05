package yand.downloader.impl;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import yand.downloader.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

/**
 * @author Max Osipov
 */
public abstract class DownloadManagerTest {

    public static final int PORT = 9870;

    private TestServer server;

    public abstract DownloadManager createManager() throws IOException;

    @Before
    public void startServer() throws Exception {
        server = new TestServer();
        server.start(PORT);
    }

    @After
    public void stopServer() throws Exception {
        server.stop();
    }


    @Test
    public void simpleDownloadTest() throws Exception {

        DownloadManager manager = createManager();
        String checksum = test1checksum();
        try {
            DownloadRequest request = new DownloadRequest(new URL[]{
                    new URL("http://localhost:" + PORT + "/test1.txt")
            });
            DownloadController controller = manager.download(request);

            DownloadResponse response = controller.get();
            for (DownloadResponseItem item : response) {
                assertEquals(checksum, checksum(item.getFile()));
            }
        }
        finally {
            manager.stop();
        }
    }

    public static String test1checksum() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        File testFile = new File(Thread.currentThread().getContextClassLoader().getResource("files/test1.txt").toURI());
        return checksum(testFile);
    }


    public static String checksum(File file) throws NoSuchAlgorithmException, IOException {
        try (FileInputStream stream = new FileInputStream(file)) {
            return DigestUtils.md5Hex(stream);
        }
    }
}
