import org.json.JSONArray;
import org.json.JSONObject;
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
    private static final String base = "https://en.wikipedia.org/w/api.php?action=query&format=json";

    HttpClient client = HttpClient.newBuilder().build();

    public List<String> getLinks(String url, boolean catSearch, boolean wlhSearch) {
        List<String> links = new ArrayList<>();
        Document document;
        if (wlhSearch) {
            url = "/wiki/Special:WhatLinksHere/" + url.replace("wiki", "").substring(2);
        }

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
        Element nextPage = null;
        for (Element element : content) {

            // next page examination and category exceptions (avoid getting stuck in birth
            // and death pages)
            if ((element.html().equals("next page") || (element.html().equals("next 50") && wlhSearch))
                    && !catSearch && !element.attr("href").contains("birth")
                    && !element.attr("href").contains("death")
                    && !element.attr("href").contains("Living_people") && !element.attr("href").contains("CS1_maint")
                    && !element.attr("href").contains("Disambiguation")
                    && !element.attr("href").contains("Year_of_death")
                    && !element.attr("href").contains("Possibly_living")
                    && !element.attr("href").contains("Year_of_birth")) {
                nextPage = element;
            }
            String href = element.attr("href");
            if (href.startsWith("/wiki") && !href.contains("birth") && !href.contains("death")
                    && !href.contains("Living_people")) {
                if (catSearch == true) {
                    if (href.contains("Category:") && !href.contains("Tracking_categories")
                            && !href.contains("Hidden_categories") && !href.contains("_authors_list")) {
                        addedUrls.add(href);
                        links.add(href);
                    }
                } else {
                    if (!href.contains("/Special:") && !href.contains("/Help:") && !href.contains("/File:")
                            && !href.contains("/Template:") && !href.contains("/Template_talk:")
                            && !href.contains("/Wikipedia:") && !href.contains("/Talk:")
                            && !href.contains("(identifier)") && !href.contains("Portal:")
                            && !href.contains("Tracking_categories")
                            && !href.contains("Hidden_categories") && !href.contains("Stub_categories")
                            && !href.contains("ISO") && !href.contains("CS1_maint") && !href.contains("User:")) {
                        addedUrls.add(href);
                        links.add(href);
                    }
                }

            }

        }
        String nextPageHref = null;
        if (nextPage != null) {
            nextPageHref = nextPage.attr("href");

        }
        // System.out.println(nextPageHref);
        // remove duplicate links
        if (nextPageHref != null) {

            System.out.println("examining extra links: " + nextPageHref);
            links.addAll(getLinks(nextPageHref, false, false));
        }
        return links;
    }

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
                // } else if (pr.equals("linkshere")) {
                // System.out.println(lObj.toString(2));
                // }

            }
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        // System.out.println(links);
        // System.out.println(links.size());
        return links;
    }

    public List<String> findWikiPath(String start, String end) {

        // add list of visited links to avoid loops
        List<String> path = new ArrayList<>();
        List<String> visitedUrls = new ArrayList<>();

        urlLink target = new urlLink(end);

        // List<String> targetCategories = new ArrayList<>(APIgetLinks(end,
        // "categories", "PLACEHOLDER"));
        // targetCategories.removeIf(t -> (t.contains("births") ||
        // t.contains("deaths"))); // birth and death pages are too
        // // // // long to use
        List<String> targetLinks = APIgetLinks(end, "linkshere", "PLACEHOLDER");
        System.out.println(targetLinks.size());
        // int j = 0;

        // while (targetLinks.size() < 499 && j < targetCategories.size()) {
        // // while (j < targetCategories.size()) {

        // // List<String> targets = new ArrayList<>(getLinks(targetCategories.get(j),
        // // false, false));
        // System.out.println(targetCategories.get(j));
        // List<String> targets = new ArrayList<>(APIgetLinks(targetCategories.get(j),
        // "links", "PLACEHOLDER"));

        // for (String i : targets) {
        // targetLinks.add(i);
        // }

        // }

        System.out.println(targetLinks.size());
        // select the first link found on the target page to compare to
        boolean pathFound = false;
        // current opened link
        urlLink currentLink = new urlLink(start);
        currentLink.setSimilarity(APIgetLinks(start, "links", end), targetLinks);
        double currentBestMatch = 0;
        // boolean examineCat = false;
        // boolean examineAll = false;
        while (!pathFound) {
            currentBestMatch = currentLink.getSimilarity();
            System.out.println("CurrentBestMatch: " + currentBestMatch);
            visitedUrls.add(currentLink.getUrl());
            System.out.println(currentLink.getUrl() + " " + currentLink.getSimilarity());
            // list of raw links on the current page

            // List<String> currentChildren = getLinks(currentLink.getUrl(), false, false);
            List<String> currentChildren = APIgetLinks(currentLink.getUrl(), "links", "PLACEHOLDER");
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

                // List<String> tempChildren = getLinks(currentLink.getUrl(), false, false);
                List<String> tempChildren = APIgetLinks(currentLink.getUrl(), "links", "PLACEHOLDER");
                currentChildren.retainAll(targetLinks); // get all similar links
                // add the categories always
                if (currentChildren.size() == 0) {
                    currentChildren = tempChildren;

                }
                // else if (examineAll) {
                // System.out.println("Examining All");
                // currentChildren = tempChildren;
                // } else if (examineCat) {
                // System.out.println("Examining categories");

                // // currentChildren = getLinks(currentLink.getUrl(), true, false);
                // currentChildren = APIgetLinks(currentLink.getUrl(), "categories",
                // "PLACEHOLDER");
                // } else {

                // boolean b = true;
                // for (String s : currentChildren) {
                // if (!visitedUrls.contains(s)) {
                // b = false;
                // }
                // }
                // // if all similar links have already been visited, examine all links on page
                // if (b) {
                // System.out.println("Examining all links");
                // currentChildren = tempChildren;
                // }
                // }
                // create urlLink objects for the common links
                urlLink curlLink = currentLink;
                List<urlLink> currentChildrenURL = currentChildren.stream().distinct()
                        .map(l -> new urlLink(l, curlLink))
                        .collect(Collectors.toList());

                // make pQueue, find similarities, add to pqueue, select most common link
                Comparator<urlLink> similarityCompare = Comparator.comparing(urlLink::getSimilarity);
                PriorityQueue<urlLink> pQueue = new PriorityQueue<urlLink>(similarityCompare.reversed());

                boolean foundTargetCat = false;
                for (urlLink link : currentChildrenURL) {
                    // link.setSimilarity(getLinks(link.getUrl(), false, false), targetLinks);
                    link.setSimilarity(APIgetLinks(link.getUrl(), "links", "PLACEHOLDER"), targetLinks);

                    System.out.println(" examining: " + link.getUrl() + " " +
                            link.getSimilarity());

                    if (!visitedUrls.contains(link.getUrl())) {
                        pQueue.add(link);

                    }
                    // if (targetCategories.contains(link.getUrl())) {
                    // currentLink = link;
                    // foundTargetCat = true;
                    // currentBestMatch = 0;
                    // break;
                    // }
                    // check if link contains a link to a target category, if so go to that
                    // and the target should be there

                    if (link.getSimilarity() > currentBestMatch) {
                        break;
                    }
                    // if (link.getSimilarity() == target_links_num && string is sufficiently
                    // similar to target.getUrl

                }
                // if (examineAll) {
                // examineAll = false;
                // currentBestMatch = 0;
                // }
                // if (examineCat) {

                // examineAll = true;
                // examineCat = false;
                // }
                // if (!foundTargetCat) {
                // currentLink = pQueue.poll();
                // }
                // if (currentLink.getSimilarity() < currentBestMatch) {
                // currentBestMatch = currentLink.getSimilarity();
                // currentLink = currentLink.getParent();
                // examineCat = true;
                // // currentBestMatch = 0;
                // System.out.println("Reverting to " + currentLink.getUrl());
                // }
                currentLink = pQueue.poll();

                visitedUrls.add(currentLink.getUrl());
                pQueue.clear();

            }
        }

        return path;

    }

    public List<String> findWLHPATH(String start, String end) {

        // add list of visited links to avoid loops
        List<String> path = new ArrayList<>();
        List<String> visitedUrls = new ArrayList<>();

        urlLink target = new urlLink(end);
        // // // long to use
        List<String> targetLinks = APIgetLinks(end, "links", "PLACEHOLDER");
        System.out.println(targetLinks.size());

        // select the first link found on the target page to compare to
        boolean pathFound = false;
        // current opened link
        urlLink currentLink = new urlLink(start);

        currentLink.setSimilarity(APIgetLinks(start, "linkshere", "PLACEHOLDER"), targetLinks);
        double currentBestMatch = 0;
        boolean examineCat = false;
        boolean examineAll = false;
        while (!pathFound) {
            currentBestMatch = currentLink.getSimilarity();
            System.out.println("CurrentBestMatch: " + currentBestMatch);
            System.out.println(currentLink.getUrl() + " " + currentLink.getSimilarity());
            // list of raw links on the current page

            // List<String> currentChildren = getLinks(currentLink.getUrl(), false, false);
            List<String> currentChildren = APIgetLinks(currentLink.getUrl(), "linkshere", "/wiki/Demonym");
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

                // List<String> tempChildren = getLinks(currentLink.getUrl(), false, false);
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

                // make pQueue, find similarities, add to pqueue, select most common link
                Comparator<urlLink> similarityCompare = Comparator.comparing(urlLink::getSimilarity);
                PriorityQueue<urlLink> pQueue = new PriorityQueue<urlLink>(similarityCompare.reversed());

                for (urlLink link : currentChildrenURL) {
                    // link.setSimilarity(getLinks(link.getUrl(), false, false), targetLinks);
                    link.setSimilarity(APIgetLinks(link.getUrl(), "links", "PLACEHOLDER"), targetLinks);

                    System.out.println(" examining: " + link.getUrl() + " " +
                            link.getSimilarity());

                    if (!visitedUrls.contains(link.getUrl())) {
                        pQueue.add(link);

                    }

                    if (link.getSimilarity() > currentBestMatch) {
                        break;
                    }
                    // if (link.getSimilarity() == target_links_num && string is sufficiently
                    // similar to target.getUrl

                }
                currentLink = pQueue.poll();
                visitedUrls.add(curlLink.getUrl());
                pQueue.clear();

            }
        }

        return path;

    }

}
