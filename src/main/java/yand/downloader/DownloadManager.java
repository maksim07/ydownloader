package yand.downloader;


/**
 * Interface of components for asynchronously downloading resources
 *
 * @author Max Osipov
 */
public interface DownloadManager {

    /**
     * Asynchronously starts downloading of the resources
     *
     * @param request resources loading request
     * @return controller to manage download process
     * @throws yand.downloader.DownloadException exception in case of wrong (unsupported) request
     */
    DownloadController download(DownloadRequest request) throws DownloadException;
}
