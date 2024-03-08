import org.json.JSONArray;
import org.json.JSONObject;

import java.util.stream.Collectors;
import java.util.*;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;

public class WikiAPI {
    private static final String base = "https://en.wikipedia.org/w/api.php?action=query&format=json";

    HttpClient client = HttpClient.newBuilder().build();

    public List<String> APIgetLinks(String url, String pr, String stopUrl) {
        url = url.replace("/wiki/", "");
        List<String> links = new ArrayList<>();
        if (url.equals("Demonym")) {
            File f = new File("Demonym_" + pr + ".txt");
            try {
                Scanner sc = new Scanner(f);
                String[] linksArray = sc.nextLine().split(", ");
                Collections.addAll(links, linksArray);
                sc.close();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

        } else {
            JSONObject responseObj;
            String cont = null;
            try {

                while (links.size() < 10000) {
                    String re = "";
                    if (url.contains("Category:") || url.contains("category:")) {
                        re = base + "&list=categorymembers&cmtitle=" + url
                                + "&cmprop=title&cmtype=page%7Csubcat&cmlimit=500";
                    } else {
                        re = base + "&titles=" + url + "&prop=" + pr + "&";
                        if (pr.equals("links")) {
                            re += "pllimit=500";
                        } else if (pr.equals("linkshere")) {
                            re += "lhprop=title&lhlimit=500";
                        } else if (pr.equals("categories")) {
                            re += "clshow=!hidden&cllimit=500";
                        }
                    }

                    URI reqUri = null;
                    if (cont == null) {
                        reqUri = URI.create(re);
                    } else {
                        // make the specific type of continue request
                        cont = cont.replace("|", "%7C");
                        if (url.contains("Category:") || url.contains("category:")) {
                            reqUri = URI.create(re + "&cmcontinue=" + cont);
                        } else {
                            if (pr.equals("links")) {
                                reqUri = URI.create(re + "&plcontinue=" + cont);
                            } else if (pr.equals("linkshere")) {
                                reqUri = URI.create(re + "&lhcontinue=" + cont);
                            } else if (pr.equals("categories")) {
                                reqUri = URI.create(re + "&clcontinue=" + cont);
                            }
                        }

                    }
                    // send the request
                    HttpRequest request = HttpRequest.newBuilder()
                            .GET()
                            .uri(reqUri)
                            .build();
                    System.out.println(request);
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    responseObj = new JSONObject(response.body());
                    JSONArray lList = new JSONArray();
                    // add contents to list
                    if (url.contains("Category:") || url.contains("category:")) {
                        lList = responseObj.getJSONObject("query").getJSONArray("categorymembers");
                    } else {
                        JSONObject lObj = responseObj.getJSONObject("query").getJSONObject("pages");
                        lList = lObj.getJSONObject(lObj.keys().next()).getJSONArray(pr);
                    }
                    for (Object j : lList) {
                        JSONObject jO = new JSONObject(j.toString());
                        if (!jO.getString("title").contains("User:") && !jO.getString("title").contains("Wikipedia:")
                                && !jO
                                        .getString("title").contains("Talk:")
                                && !jO.getString("title").contains("User_talk:"))
                            links.add("/wiki/" + jO.getString("title").replace(" ", "_"));
                    }
                    // if there is no continue element or if the stopURL has been found, break the
                    // loop and return the links, otherwise
                    if (!responseObj.keySet().contains("continue")) {
                        break;
                    } else if (links.contains(stopUrl)) {
                        break;
                    }
                    // otherwise get the specific type of continue and set it's contents equal to
                    // the cont string
                    else {
                        if (url.contains("Category:") || url.contains("category")) {
                            cont = responseObj.getJSONObject("continue").getString("cmcontinue");
                        } else {
                            if (pr.equals("links")) {
                                cont = responseObj.getJSONObject("continue").getString("plcontinue");
                            } else if (pr.equals("linkshere")) {
                                cont = responseObj.getJSONObject("continue").getString("lhcontinue");
                                // System.out.println(responseObj.getJSONObject("continue"));
                            } else if (pr.equals("categories")) {
                                cont = responseObj.getJSONObject("continue").getString("clcontinue");
                            }
                        }

                    }

                }
            } catch (IOException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }

        return links;
    }

    public List<String> findWikiPath(String start, String end, String method) {
        // add list of visited links to avoid loops
        List<String> path = new ArrayList<>();
        List<String> visitedUrls = new ArrayList<>();
        List<String> targetLinks = new ArrayList<>();
        urlLink target = new urlLink(end);
        if (method.equals("down")) {
            targetLinks = APIgetLinks(end, "linkshere", "PLACEHOLDER");

        } else if (method.equals("up")) {
            targetLinks = APIgetLinks(end, "links", "PLACEHOLDER");

        }
        // select the first link found on the target page to compare to
        boolean pathFound = false;
        // current opened link
        urlLink currentLink = new urlLink(start);

        if (method.equals("down")) {
            currentLink.setSimilarity(APIgetLinks(start, "links", end), targetLinks);

        } else if (method.equals("up")) {
            currentLink.setSimilarity(APIgetLinks(start, "linkshere", end), targetLinks);
        }
        double currentBestMatch = 0;
        List<String> currentChildren = new ArrayList<>();

        while (!pathFound) {
            currentBestMatch = currentLink.getSimilarity();
            System.out.println("CurrentBestMatch: " + currentBestMatch);
            System.out.println(currentLink.getUrl() + " " + currentLink.getSimilarity());

            if (method.equals("down")) {
                currentChildren = APIgetLinks(currentLink.getUrl(), "links", end);

            } else if (method.equals("up")) {
                if (currentLink.getUrl().contains("List")) {
                    currentChildren = APIgetLinks(currentLink.getUrl(), "links", end);
                } else {
                    currentChildren = APIgetLinks(currentLink.getUrl(), "linkshere", "/wiki/Demonym");

                }

            }
            if (currentChildren.contains(end)) { // if the target is on the current page return path
                currentLink = new urlLink(end, currentLink);
                pathFound = true;
                while (currentLink.getParent() != null) {
                    path.add(currentLink.getParent().getUrl());
                    currentLink = currentLink.getParent();
                }
                // reverse the path so it is ordered from start to finish
                Collections.reverse(path);
                path.add(target.getUrl());

            } else {

                List<String> tempChildren = new ArrayList<String>(currentChildren);
                currentChildren.retainAll(targetLinks); // get all similar links
                // if no similar links, examine all
                if (currentChildren.size() == 0) {
                    currentChildren = tempChildren;

                }

                urlLink curlLink = currentLink;
                // map from list of urls to list of urlLinks
                List<urlLink> currentChildrenURL = currentChildren.stream().distinct()
                        .map(l -> new urlLink(l, curlLink))
                        .collect(Collectors.toList());
                currentChildrenURL.removeIf(s -> s.getUrl().contains("User")); // never go into User links
                currentChildrenURL.removeIf(s -> visitedUrls.contains(s.getUrl())); // remove already visited links to
                                                                                    // avoid loops
                currentChildrenURL.removeIf(s -> s.getUrl().contains("Wikipedia") || s.getUrl().contains("talk"));
                currentChildrenURL.removeIf(s -> s.getUrl().contains("January") ||
                        s.getUrl().contains("February") || s.getUrl().contains("March") ||
                        s.getUrl().contains("April") || s.getUrl().contains("May") ||
                        s.getUrl().contains("June") || s.getUrl().contains("July") ||
                        s.getUrl().contains("August") || s.getUrl().contains("September") ||
                        s.getUrl().contains("October") || s.getUrl().contains("November") ||
                        s.getUrl().contains("December")); // avoid getting stuck in
                                                          // dates;
                // make pQueue, find similarities, add to pqueue, select most common link
                Comparator<urlLink> similarityCompare = Comparator.comparing(urlLink::getSimilarity);
                PriorityQueue<urlLink> pQueue = new PriorityQueue<urlLink>(similarityCompare.reversed());

                for (urlLink link : currentChildrenURL) {
                    // set the similarity
                    link.setSimilarity(APIgetLinks(link.getUrl(), "links", "PLACEHOLDER"), targetLinks);

                    System.out.println(" examining: " + link.getUrl() + " " +
                            link.getSimilarity());
                    pQueue.add(link);

                    // if a new better link is found, break without examining the rest, results in
                    // this alogirthm being fast but not finding the shortest possible path
                    if (link.getSimilarity() > currentBestMatch) {
                        break;
                    }
                }
                // set the new link, add it to visitedURLs, clear the priority queue
                currentLink = pQueue.poll();
                visitedUrls.add(currentLink.getUrl());
                pQueue.clear();

            }
        }
        return path;

    }
}
