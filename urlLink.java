import java.util.*;

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

    public void setSimilarity(List<String> links, List<String> targetLinks) {
        links.retainAll(targetLinks);
        this.similarity = links.size();
    }

    public urlLink getParent() {
        return this.parent;
    }

    public int getSimilarity() {
        return similarity;
    }

    public String toString() {
        return this.getUrl() + " " + this.getSimilarity() + " |PARENT " + this.getParent();
    }

}
