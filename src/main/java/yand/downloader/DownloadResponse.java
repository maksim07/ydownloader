package yand.downloader;

import java.util.*;

/**
 * Interface of responses to download requests.
 *
 * @author Max Osipov
 */
public class DownloadResponse implements Iterable<DownloadResponseItem> {

    /**
     * Flag means that download of entire request was successfull
     */
    private final boolean success;

    /**
     * Original request
     */
    private final DownloadRequest request;

    /**
     * File with downloaded data
     */
    private final Collection<DownloadResponseItem> items;

    public DownloadResponse(boolean success, DownloadRequest request, Collection<DownloadResponseItem> items) {
        this.success = success;
        this.request = request;
        this.items = Collections.unmodifiableCollection(items);
    }

    public boolean isSuccess() {
        return success;
    }

    public DownloadRequest getRequest() {
        return request;
    }

    @Override
    public Iterator<DownloadResponseItem> iterator() {
        return items.iterator();
    }
}
