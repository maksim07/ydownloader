package yand.downloader;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * This tool reads download task from file and performs download. Each line of the file either empty or contains url to load.
 * Empty lines separate requests.
 *
 * @author Max Osipov
 */
public class DownloadTool {
    private File jobDescriptor;

    private DownloadManager downloadManager;

    public static void main(String[] args) throws IOException, DownloadException {
        if (args.length == 0) {
            System.out.println("USAGE: java DownloadTool <job-descriptor-file>");
            System.exit(1);
            return ;
        }

        File jobDescriptor = new File(args[0]);
        if (!jobDescriptor.exists() || !jobDescriptor.isFile()) {
            throw new IllegalArgumentException("Argument is either not file or doesn't exist");
        }

        DownloadTool tool = new DownloadTool(jobDescriptor);
        try {
            tool.process();
        }
        finally {
            tool.stop();
        }
    }

    public DownloadTool(File jobDescriptor) {
        this.jobDescriptor = jobDescriptor;
        this.downloadManager = DownloadManagers.createDefault();
    }

    private void process() throws IOException, DownloadException {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jobDescriptor)))) {

            // start tasks
            List<DownloadController> downloads = new ArrayList<>();
            List<URL> urls = new ArrayList<>();
            String line;
            while((line = reader.readLine()) != null) {

                String trimmedLine = line.trim();
                if (trimmedLine.length() > 0)
                    urls.add(new URL(line.trim()));
                else if (urls.size() > 0) {
                    downloads.add(startTask(urls));
                    urls = new ArrayList<>();
                }
            }

            if (urls.size() > 0)
                downloads.add(startTask(urls));

            // wait for result
            for (DownloadController controller : downloads) {

                try {
                    DownloadResponse response = controller.get();
                    processResponse(response);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    System.err.println("Error processing the request:");
                    DownloadRequest request = controller.getRequest();
                    for (URL url : request.getResources())
                        System.err.println("  " + url);
                }
            }
        }
    }

    private void processResponse(DownloadResponse response) {
        for (DownloadResponseItem item : response) {
            String path = item.getUrl().getPath();
            int slashPos = path.lastIndexOf('/');
            String fileName = slashPos == -1 ? path : path.substring(slashPos + 1);
            if (fileName.length() == 0)
                fileName = item.getUrl().getHost() + ".index.html";

            File file = new File(fileName);
            int counter = 0;
            while (file.exists()) {
                file = new File(fileName + "_" + counter);
                counter ++;
            }

            item.getFile().renameTo(file);
        }
    }


    private void stop() throws DownloadException {
        downloadManager.stop();
    }

    private DownloadController startTask(List<URL> urls) throws DownloadException {

        DownloadRequest request = new DownloadRequest(urls.toArray(new URL[urls.size()]));
        return downloadManager.download(request);
    }
}
