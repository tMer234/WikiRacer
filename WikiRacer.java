import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WikiRacer {

    public static void DisplayPath(List<String> p) {
        for (int i = 0; i < p.size(); i++) {
            System.out.print(path.get(i));
            if (i != p.size() - 1) {
                System.out.print(" --> ");
            }
        }
    }

    public static void main(String[] args) {
        // final String base_url = "https://en.wikipedia.org/wiki/";
        String start = "Hang_Chineh";
        String end = "2001_Irish_Masters";

        final long startTime = System.currentTimeMillis();
        WikiAPI wiki = new WikiAPI();

        // find path to demonynm (always roughly < 10 jumps)
        // find path from demonym
        List<String> path_1 = wiki.findWikiPath("/wiki/" + start, "/wiki/Demonym");
        List<String> path_2 = wiki.findWikiPath("/wiki/Demonym", "/wiki/" + end);
        final long endTime = System.currentTimeMillis();
        System.out.print("Path Found: ");
        for (int i = 0; i < path_1.size(); i++) {
            System.out.print(path_1.get(i));
            if (i != path_1.size() - 1) {
                System.out.print(" --> ");
            }
        }
        System.out.print(" --> ");
        for (int i = 1; i < path_2.size(); i++) {
            System.out.print(path_2.get(i));
            if (i != path_2.size() - 1) {
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