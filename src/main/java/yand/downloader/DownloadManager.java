package yand.downloader;


import java.io.IOException;

/**
 * Interface of components for asynchronously downloading resources
 *
 * @author Max Osipov
 */
public interface DownloadManager {


    /**
     * Download manager has to be started before usage
     */
    public void start() throws DownloadException;

    /**
     * Download manager has to be stopped after usage
     */
    public void stop() throws DownloadException;

    /**
     * Asynchronously starts downloading of the resources
     *
     * @param request resources loading request
     * @return controller to manage download process
     * @throws yand.downloader.DownloadException exception in case of wrong (unsupported) request
     */
    DownloadController download(DownloadRequest request) throws DownloadException;
}
