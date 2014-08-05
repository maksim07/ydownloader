package yand.downloader;


/**
 * Interface of components for asynchronously downloading resources
 *
 * @author Max Osipov
 */
public interface DownloadManager {

    /**
     * Download manager has to be stopped after usage
     */
    void stop() throws DownloadException;

    /**
     * Asynchronously starts downloading of the resources
     *
     * @param request resources loading request
     * @return controller to manage download process
     */
    DownloadController download(DownloadRequest request) throws DownloadException;
}
