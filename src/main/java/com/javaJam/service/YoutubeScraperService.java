package com.javaJam.service;

import com.javaJam.model.Music;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeScraperService {
    private static final Logger LOGGER = Logger.getLogger(YoutubeScraperService.class.getName());

    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public List<Music> searchTracks(String query) {
        List<Music> tracks = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return tracks;
        }

        try {
            List<Music> exactMatchTracks = fetchRawYouTubeResults(query, 1);
            if (!exactMatchTracks.isEmpty()) {
                tracks.add(exactMatchTracks.get(0));
            }

            String artistQuery = extractArtistQuery(query);
            String secondaryQuery = artistQuery + " songs";
            List<Music> artistTracks = fetchRawYouTubeResults(secondaryQuery, 30);
            String firstVideoId = !tracks.isEmpty() ? extractVideoId(tracks.get(0).getUrl()) : "";

            for (Music music : artistTracks) {
                if (tracks.size() >= 20) break;
                String videoId = extractVideoId(music.getUrl());
                if (videoId != null && !videoId.equals(firstVideoId)) {
                    boolean exists = tracks.stream().anyMatch(t -> t.getUrl().equals(music.getUrl()));
                    if (!exists) {
                        tracks.add(music);
                    }
                }
            }

            if (tracks.size() < 20) {
                List<Music> fallbackTracks = fetchRawYouTubeResults(query, 30);
                for (Music music : fallbackTracks) {
                    if (tracks.size() >= 20) break;
                    boolean exists = tracks.stream().anyMatch(t -> t.getUrl().equals(music.getUrl()));
                    if (!exists) {
                        tracks.add(music);
                    }
                }
            }

            LOGGER.info("Successfully scraped " + tracks.size() + " tracks (1 exact match + related artist tracks).");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error scraping YouTube", e);
        }
        return tracks;
    }

    private List<Music> fetchRawYouTubeResults(String searchQuery, int maxResults) {
        List<Music> tracks = new ArrayList<>();
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String url = "https://www.youtube.com/results?search_query=" + encodedQuery;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String html = response.body();

            Pattern videoBlockPattern = Pattern.compile(
                    "\"videoRenderer\":\\{(.*?)(?=,\"(?:trackingParams|menu|thumbnailOverlays)\"|\\}\\s*,\\s*\"videoRenderer\"|$)",
                    Pattern.DOTALL
            );

            Matcher matcher = videoBlockPattern.matcher(html);

            while (matcher.find() && tracks.size() < maxResults) {
                String block = matcher.group(1);

                Matcher idMatcher = Pattern.compile(
                        "\"videoId\":\"([a-zA-Z0-9_-]{11})\""
                ).matcher(block);

                Matcher titleMatcher = Pattern.compile(
                        "\"title\":\\{\"runs\":\\[\\{\"text\":\"((?:\\\\.|[^\"\\\\])*)\""
                ).matcher(block);

                if (idMatcher.find() && titleMatcher.find()) {
                    String videoId = idMatcher.group(1);
                    String title = titleMatcher.group(1);

                    title = title
                            .replace("\\u0026", "&")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\");

                    String videoUrl = "https://www.youtube.com/watch?v=" + videoId;

                    tracks.add(new Music(title, videoUrl));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error fetching raw YouTube results for: " + searchQuery, e);
        }
        return tracks;
    }

    private String extractArtistQuery(String query) {
        String trimmed = query.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2) {
            return parts[parts.length - 2] + " " + parts[parts.length - 1];
        }
        return trimmed;
    }

    private String extractVideoId(String url) {
        if (url == null) return null;
        Pattern pattern = Pattern.compile("(?:v=|/)([a-zA-Z0-9_-]{11})");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}