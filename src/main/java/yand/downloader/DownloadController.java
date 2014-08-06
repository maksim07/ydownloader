package yand.downloader;

import java.util.concurrent.Future;

/**
 * Interface with methods to manage downloading process
 *
 * @author Max Osipov
 */
public interface DownloadController extends Future<DownloadResponse> {

    enum DownloadingStatus {
        STARTING, RUNNING, PAUSED, SUCCESS, ERROR, CANCELED
    }

    public DownloadRequest getRequest();

    /**
     * Returns current status of the downloading process
     *
     * @return status
     */
    DownloadingStatus status();

    /**
     * Pauses downloading process
     */
    public void pause();

    /**
     * Resumes downloading process after pause
     */
    public void resume();
}
