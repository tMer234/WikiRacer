import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WikiRacer {

    public static void main(String[] args) {
        final String base_url = "https://en.wikipedia.org/wiki/";
        String start = "Qmusic_TV";
        String end = "Leslie_Dunkling";

        final long startTime = System.currentTimeMillis();
        WikiAPI wiki = new WikiAPI();
        List<String> path = wiki.findWikiPath("/wiki/" + start, "/wiki/" + end);
        final long endTime = System.currentTimeMillis();
        System.out.print("Path Found: ");

        for (int i = 0; i < path.size(); i++) {
            System.out.print(path.get(i));
            if (i != path.size() - 1) {
                System.out.print(" --> ");
            }
        }

        System.out.println("\n" + (double) (endTime - startTime) / 1000 + " seconds");

    }

}
// if link is a category on the target page, break and go to it

// use what links here page
// special character pages don't match
// might be more efficient to choose one category to populate target pages
// change to always search from page with few links to page with more links
// next: when in a category only examine the page that holds the alphabetical
// match