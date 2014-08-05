package yand.downloader.impl;

import yand.downloader.DownloadController;
import yand.downloader.DownloadException;
import yand.downloader.DownloadManager;
import yand.downloader.DownloadRequest;

import java.util.Map;

/**
 * Download manager composed of managers for each distinct protocol scheme.
 * Clients should not send requests with mixed url protocols to this downloader.
 *
 * @author Max Osipov
 */
public class CompositeDownloadManager implements DownloadManager {

    /**
     * Registry of managers
     */
    private final Map<String, DownloadManager> registry;

    public CompositeDownloadManager(Map<String, DownloadManager> registry) {
        this.registry = registry;
    }

    @Override
    public void stop() throws DownloadException {
        for (DownloadManager manager : registry.values())
            manager.stop();
    }

    @Override
    public DownloadController download(DownloadRequest request) throws DownloadException {

        if (request.getResources().length == 0) {
            throw new IllegalArgumentException("At least one url has to be provided");
        }
        // check if all request urls has the same protocol
        String protocol = request.getResources()[0].getProtocol();
        for (int i = 1; i < request.getResources().length; i ++)
            if (!protocol.equals(request.getResources()[i].getProtocol()))
                throw new IllegalArgumentException("All urls of the same request has to have the same protocol scheme");


        DownloadManager target = registry.get(protocol);
        return target.download(request);
    }

}
