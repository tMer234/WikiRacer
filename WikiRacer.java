
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.net.URLEncoder;

//THIS CODE REQUIRES THE ORG.JSON LIBRARY FOUND HERE: https://repo1.maven.org/maven2/org/json/json/20240205/
//Download the json-20240205.jar file from this link and install in referenced libraries
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
        String start = "Cahya_Supriadi";
        String end = "Oluyoro_Catholic_Hospital";
        final long startTime = System.currentTimeMillis();
        WikiAPI wiki = new WikiAPI();

        // Every page can always get to the Demonym page within at least 4-5 links
        List<String> path_1 = wiki.findWikiPath("/wiki/" + start, "/wiki/Demonym", "down");
        System.out.println("Path_1 found\n");
        List<String> path_2 = wiki.findWikiPath("/wiki/" + end, "/wiki/Demonym", "up");
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
        for (int i = path_2.size() - 2; i >= 0; i--) {
            System.out.print(path_2.get(i).toString().replace("/wiki/", ""));
            if (i != 0) {
                System.out.print(" --> ");
            }
        }

    }

}

// page names with special characters don't match because of the utf-8 encoding
// might be more efficient to choose one category to populate target pages
// next: when in a category only examine the page that holds the alphabetical
// match