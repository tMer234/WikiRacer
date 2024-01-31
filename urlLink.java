import java.util.*;
import java.util.stream.Collectors;

public class urlLink {
    private urlLink parent;
    private String url;
    private int similarity;

    public urlLink(String url) {
        this.url = url;
    }

    public urlLink(String url, urlLink parent) {
        this.url = url;
        this.parent = parent;
    }

    public String getUrl() {
        return this.url;
    }

    public void setSimilarity(List<urlLink> links, List<urlLink> targetLinks) {
        Set<String> targetUrls = targetLinks.stream()
                .map(urlLink::getUrl)
                .collect(Collectors.toSet());

        long num = links.stream()
                .map(urlLink::getUrl) // Extracting URLs from the list of links
                .distinct() // Ensuring distinct URLs
                .filter(targetUrls::contains) // Filtering URLs present in targetUrls Set
                .count();

        this.similarity = (int) num;
    }

    public urlLink getParent() {
        return this.parent;
    }

    public int getSimilarity() {
        // number of links on this page also on the target page
        return similarity;
    }

    public String toString() {
        return this.getUrl() + " " + this.getSimilarity() + " |PARENT " + this.getParent();
    }
    // public String getPath() {
    // String path = "";
    // urlLink currentLink = this;
    // while (!currentLink.getParent().getUrl().equals(this.start.getUrl())) {
    // path = path + currentLink + ",";
    // currentLink = currentLink.getParent();
    // }
    // return path;
    // }

}
