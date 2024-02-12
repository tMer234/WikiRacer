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

    public List<String> findWikiPath(String start, String end) {

        // add list of visited links to avoid loops
        List<String> path = new ArrayList<>();
        List<String> visitedUrls = new ArrayList<>();

        urlLink target = new urlLink(end);
        List<String> targetCategories = new ArrayList<>(getLinks(end, true, false));

        // links on target page
        targetCategories.removeIf(t -> (t.contains("births") ||
                t.contains("deaths"))); // birth and death pages are too
        // // // long to use
        List<String> targetLinks = getLinks(end, false, true);

        // List<String> extraLinks = new ArrayList<>();
        // if (targetLinks.size() < 30) {
        // for (String t : targetLinks) {
        // if (getLinks(t, false, true).size() < 20) {
        // extraLinks.addAll((getLinks(end, false, true)));
        // }
        // }
        // }
        // targetLinks.addAll(extraLinks);

        System.out.println(targetLinks.size());
        if (targetLinks.size() < 100) {

            List<String> targets = new ArrayList<>(getLinks(targetCategories.get(0), false, false));
            for (String i : targets) {
                targetLinks.add(i);
            }

        }

        System.out.println(targetLinks.size());
        // select the first link found on the target page to compare to
        boolean pathFound = false;
        // boolean examineAll = false;
        // current opened link
        urlLink currentLink = new urlLink(start);
        int currentBestMatch = 0;
        boolean examineCat = false;
        boolean examineAll = false;
        while (!pathFound) {
            currentBestMatch = currentLink.getSimilarity();
            visitedUrls.add(currentLink.getUrl());
            System.out.println(currentLink.getUrl() + " " + currentLink.getSimilarity());
            // list of raw links on the current page
            List<String> currentChildren = getLinks(currentLink.getUrl(), false, false);
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

                List<String> tempChildren = getLinks(currentLink.getUrl(), false, false);

                currentChildren.retainAll(targetLinks); // get all similar links
                // add the categories always
                if (currentChildren.size() == 0) {
                    currentChildren = tempChildren;

                } else if (examineAll) {
                    currentChildren = tempChildren;
                } else if (examineCat) {
                    System.out.println("Examining categories");

                    currentChildren = getLinks(currentLink.getUrl(), true, false);

                } else {

                    boolean b = true;
                    for (String s : currentChildren) {
                        if (!visitedUrls.contains(s)) {
                            b = false;
                        }
                    }
                    // if all similar links have already been visited, examine all links on page
                    if (b) {
                        System.out.println("Examining all links");
                        currentChildren = tempChildren;
                    }
                }
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
                    link.setSimilarity(getLinks(link.getUrl(), false, false), targetLinks);
                    System.out.println(" examining: " + link.getUrl() + " " +
                            link.getSimilarity());

                    if (!visitedUrls.contains(link.getUrl())) {
                        pQueue.add(link);

                    }
                    if (targetCategories.contains(link.getUrl())) {
                        currentLink = link;
                        foundTargetCat = true;
                        currentBestMatch = 0;
                        break;
                    }
                    // check if link contains a link to a target category, if so go to that
                    // and the target should be there

                    if (link.getSimilarity() > currentBestMatch) {
                        break;
                    }

                }
                if (examineAll) {
                    examineAll = false;
                    currentBestMatch = 0;
                }
                if (examineCat) {

                    examineAll = true;
                    examineCat = false;
                }
                if (!foundTargetCat) {
                    currentLink = pQueue.poll();

                }
                if (currentLink.getSimilarity() < currentBestMatch) {
                    currentBestMatch = currentLink.getSimilarity();

                    currentLink = currentLink.getParent();
                    examineCat = true;
                    currentBestMatch = 0;
                    System.out.println("Reverting to " + currentLink.getUrl());
                }

                // for (urlLink l : pQueue) {
                // visitedUrls.add(l.getUrl());
                // }
                pQueue.clear();

            }
        }

        return path;

    }

}
