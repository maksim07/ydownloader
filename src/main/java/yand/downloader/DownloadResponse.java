package yand.downloader;

import java.util.*;

/**
 * Interface of responses to download requests.
 *
 * @author Max Osipov
 */
public class DownloadResponse implements Iterable<DownloadResponseItem> {

    /**
     * Original request
     */
    private final DownloadRequest request;

    /**
     * File with downloaded data
     */
    private final Collection<DownloadResponseItem> items;

    public DownloadResponse(DownloadRequest request, Collection<DownloadResponseItem> items) {
        this.request = request;
        this.items = Collections.unmodifiableCollection(items);
    }

    public DownloadRequest getRequest() {
        return request;
    }

    @Override
    public Iterator<DownloadResponseItem> iterator() {
        return items.iterator();
    }
}
