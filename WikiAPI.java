import org.json.JSONArray;
import org.json.JSONObject;
import java.util.stream.Collectors;
import java.io.IOException;
import java.net.http.HttpClient;
import java.util.*;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class WikiAPI {
    private static final String base = "https://en.wikipedia.org/w/api.php?action=query&format=json";

    HttpClient client = HttpClient.newBuilder().build();

    public List<String> APIgetLinks(String url, String pr, String stopUrl) {
        url = url.replace("/wiki/", "");
        List<String> links = new ArrayList<>();
        JSONObject responseObj;
        String cont = null;
        try {

            while (true) {
                String re = "";
                if (url.contains("Category:")) {
                    re = base + "&list=categorymembers&cmtitle=" + url
                            + "&cmprop=title&cmtype=page%7Csubcat&cmlimit=250";
                } else {
                    re = base + "&titles=" + url + "&prop=" + pr + "&";
                    if (pr.equals("links")) {
                        re += "pllimit=250";
                    } else if (pr.equals("linkshere")) {
                        re += "lhprop=title&lhlimit=250";
                    } else if (pr.equals("categories")) {
                        re += "clshow=!hidden&cllimit=250";
                    }
                }

                URI reqUri = null;
                if (cont == null) {
                    reqUri = URI.create(re);
                } else {
                    cont = cont.replace("|", "%7C");
                    if (url.contains("Category:")) {
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
                HttpRequest request = HttpRequest.newBuilder()
                        .GET()
                        .uri(reqUri)
                        .build();
                System.out.println(request);
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                responseObj = new JSONObject(response.body());
                JSONArray lList = new JSONArray();
                // add contents to list
                if (url.contains("Category:")) {
                    lList = responseObj.getJSONObject("query").getJSONArray("categorymembers");
                } else {
                    JSONObject lObj = responseObj.getJSONObject("query").getJSONObject("pages");
                    lList = lObj.getJSONObject(lObj.keys().next()).getJSONArray(pr);
                }
                for (Object j : lList) {
                    JSONObject jO = new JSONObject(j.toString());
                    if (!jO.getString("title").contains("User:") && !jO.getString("title").contains("Wikipedia:") && !jO
                            .getString("title").contains("Talk:") && !jO.getString("title").contains("User_talk:"))
                        links.add("/wiki/" + jO.getString("title").replace(" ", "_"));
                }
                if (!responseObj.keySet().contains("continue")) {
                    break;
                } else if (links.contains(stopUrl)) {
                    break;
                }

                else {
                    if (url.contains("Category:")) {
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
        System.out.println(targetLinks.size());

        // select the first link found on the target page to compare to
        boolean pathFound = false;
        // current opened link
        urlLink currentLink = new urlLink(start);

        if (method.equals("down")) {
            currentLink.setSimilarity(APIgetLinks(start, "links", end), targetLinks);

        } else if (method.equals("up")) {
            currentLink.setSimilarity(APIgetLinks(start, "linkshere", "PLACEHOLDER"), targetLinks);
        }
        double currentBestMatch = 0;
        List<String> currentChildren = new ArrayList<>();

        while (!pathFound) {
            currentBestMatch = currentLink.getSimilarity();
            System.out.println("CurrentBestMatch: " + currentBestMatch);
            System.out.println(currentLink.getUrl() + " " + currentLink.getSimilarity());
            // list of raw links on the current page

            if (method.equals("down")) {
                currentChildren = APIgetLinks(currentLink.getUrl(), "links", "PLACEHOLDER");

            } else if (method.equals("up")) {
                if (currentLink.getUrl().contains("List")) {
                    currentChildren = APIgetLinks(currentLink.getUrl(), "links", "PLACEHOLDER");
                } else {
                    currentChildren = APIgetLinks(currentLink.getUrl(), "linkshere", "/wiki/Demonym");

                }

            }
            // find all common links
            if (currentChildren.contains(end)) { // if the target is on the current page return path
                currentLink = new urlLink(end, currentLink);
                pathFound = true;
                while (currentLink.getParent() != null) {
                    path.add(currentLink.getParent().getUrl());
                    currentLink = currentLink.getParent();
                }
                Collections.reverse(path);
                path.add(target.getUrl());

            } else {

                List<String> tempChildren = new ArrayList<String>(currentChildren);

                currentChildren.retainAll(targetLinks); // get all similar links
                // add the categories always
                if (currentChildren.size() == 0) {
                    currentChildren = tempChildren;

                }

                // create urlLink objects for the common links
                urlLink curlLink = currentLink;
                List<urlLink> currentChildrenURL = currentChildren.stream().distinct()
                        .map(l -> new urlLink(l, curlLink))
                        .collect(Collectors.toList());
                currentChildrenURL.removeIf(s -> s.getUrl().contains("User_talk"));
                currentChildrenURL.removeIf(s -> visitedUrls.contains(s.getUrl()));
                // make pQueue, find similarities, add to pqueue, select most common link
                Comparator<urlLink> similarityCompare = Comparator.comparing(urlLink::getSimilarity);
                PriorityQueue<urlLink> pQueue = new PriorityQueue<urlLink>(similarityCompare.reversed());

                for (urlLink link : currentChildrenURL) {
                    link.setSimilarity(APIgetLinks(link.getUrl(), "links", "PLACEHOLDER"), targetLinks);

                    System.out.println(" examining: " + link.getUrl() + " " +
                            link.getSimilarity());

                    if (!visitedUrls.contains(link.getUrl())) {
                        pQueue.add(link);

                    }

                    if (link.getSimilarity() > currentBestMatch) {
                        break;
                    }
                }
                currentLink = pQueue.poll();
                visitedUrls.add(currentLink.getUrl());
                pQueue.clear();

            }
        }
        return path;

    }
}
