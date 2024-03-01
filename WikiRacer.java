
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.net.URLEncoder;

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
        String start = "Masters_School";
        String end = "The_Haunting_Hour:_The_Series";

        // System.out.println("Start: " + start);
        // System.out.println("End: " + end);
        final long startTime = System.currentTimeMillis();
        WikiAPI wiki = new WikiAPI();

        // get to the Demonym page: "A demonym is is a word that identifies a group of
        // people (inhabitants, residents, natives) in relation to a particularplace
        // so from there I can get to anything
        List<String> path_1 = wiki.findWikiPath("/wiki/" + start, "/wiki/Demonym");
        System.out.println("Path_1 found\n");
        List<String> path_2 = wiki.findWikiPath("/wiki/Demonym", "/wiki/" + end);
        final long endTime = System.currentTimeMillis();

        System.out.print("\nPath Found in " + +(double) (endTime - startTime) / 1000
                + " seconds:\n");
        for (int i = 0; i < path_1.size(); i++) {
            System.out.print(path_1.get(i).toString().replace("/wiki/", ""));
            if (i != path_1.size() - 1) {
                System.out.print(" --> ");
            }
        }
        System.out.print(" --> ");
        for (int i = 1; i < path_2.size(); i++) {
            System.out.print(path_2.get(i).toString().replace("/wiki/", ""));
            if (i != path_2.size() - 1) {
                System.out.print(" --> ");
            }
        }

    }

}

// page names with special characters don't match because of the utf-8 encoding
// might be more efficient to choose one category to populate target pages
// next: when in a category only examine the page that holds the alphabetical
// match