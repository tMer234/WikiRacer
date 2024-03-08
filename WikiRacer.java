
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.net.URLEncoder;
import java.net.URLDecoder;

//THIS CODE REQUIRES THE ORG.JSON LIBRARY FOUND HERE: https://repo1.maven.org/maven2/org/json/json/20240205/
//Download the json-20240205.jar file from this link and install in referenced libraries
public class WikiRacer {
    private static void DisplayPath(List<String> path) {
        for (int i = 0; i < path.size(); i++) {
            System.out.print(URLDecoder.decode(path.get(i).toString().replace("/wiki/", ""), StandardCharsets.UTF_8));
            if (i != path.size() - 1) {
                System.out.print(" --> ");
            }
        }
    }

    public static void main(String[] args) {
        // PLACE START AND END LINKS HERE
        String start = URLEncoder.encode("Grunwald,_Poznań", StandardCharsets.UTF_8);
        String end = URLEncoder.encode("Heliothis_galatheae", StandardCharsets.UTF_8);
        final long startTime = System.currentTimeMillis();
        WikiAPI wiki = new WikiAPI();
        // Every page can always get to the Demonym page within at least 3-4 jumps, so I
        // find the path from the start to demonym using page links and the end to
        // demonym using what links to that page
        if (wiki.APIgetLinks(start, "links", end).contains("/wiki/" + end)) {
            DisplayPath(new ArrayList<String>(Arrays.asList(start, end)));
        } else {
            List<String> path_1 = wiki.findWikiPath("/wiki/" + start, "/wiki/Demonym", "down");
            System.out.println("Path_1 found\n");
            List<String> path_2 = wiki.findWikiPath("/wiki/" + end, "/wiki/Demonym", "up");
            Collections.reverse(path_2);
            path_2.remove(0);
            final long endTime = System
                    .currentTimeMillis();
            System.out.print("\nPath Found in " + +(double) (endTime - startTime) / 1000 + " seconds:\n");

            // see if intersection between two paths occured before the Demonym page, if so
            // path without going through demonym or subsequent middle page exists
            List<String> lastPage1Links = wiki.APIgetLinks("/wiki/" +
                    path_1.get(path_1.size() - 2), "links", null);

            while (lastPage1Links.contains(path_2.get(0))) {
                System.out.println(path_1.get(path_1.size() - 1) + " removed");
                path_1.remove(path_1.size() - 1);
                if (path_1.size() > 1) {
                    lastPage1Links = wiki.APIgetLinks("/wiki/" + path_1.get(path_1.size() - 2),
                            "links", null);
                } else {
                    break;
                }
            }
            path_1.addAll(path_2);
            DisplayPath(path_1);
        }

    }

}