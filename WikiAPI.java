import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.stream.Collectors;
import java.io.IOException;
import java.net.http.HttpClient;
import java.util.*;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class WikiAPI {
    private final String content_id = "mw-content-text";
    private static final String base_url = "https://en.wikipedia.org";
    HttpClient client = HttpClient.newBuilder().build();

    // Finish this method to return a list of links
    public List<urlLink> getLinks(String url) {
        List<urlLink> links = new ArrayList<>();
        Document document;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(base_url + url))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            document = Jsoup.parse(response.body());
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        if (document.getElementById("mw-hidden-catlinks") != null) {
            document.getElementById("mw-hidden-catlinks").remove();
        }
        document.getElementsByClass("vector-body-before-content").remove();
        Elements content = document.getElementById("bodyContent").select("a[href]");

        Set<String> addedUrls = new HashSet<>();

        for (Element element : content) {
            String href = element.attr("href");
            if (href.startsWith("/wiki")) {
                if (!href.contains("/Special:") && !href.contains("/Help:") && !href.contains("/File:")
                        && !addedUrls.contains(href)) {
                    addedUrls.add(href);
                    links.add(new urlLink(href, new urlLink(url)));
                }
            }
        }
        // remove duplicate links

        return links;
    }

    public List<String> findWikiPath(String start, String end) {

        Comparator<urlLink> similarityCompare = Comparator.comparing(urlLink::getSimilarity);
        PriorityQueue<urlLink> pQueue = new PriorityQueue<urlLink>(similarityCompare.reversed());

        List<urlLink> targetLinks = new ArrayList<>(getLinks("/wiki/" + end));
        List<urlLink> startLinks = getLinks("/wiki/" + start);
        urlLink startLink = new urlLink("/wiki/" + start);
        startLink.setSimilarity(startLinks, targetLinks);

        for (urlLink l : startLinks) {
            l.setSimilarity(getLinks(l.getUrl()), targetLinks);
            pQueue.add(l);
            // System.out.println(l);
        }
        System.out.print(pQueue);
        return new ArrayList<String>();

    }

}
