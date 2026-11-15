package com.javaJam.service;

import javax.swing.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.javaJam.service.Globals.VERSION_NUMBER;

public class UpdateChecker {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/szmartin16/JavaJam/releases";
    private static final String CURRENT_VERSION = VERSION_NUMBER;

    public static void checkForUpdates(Runnable onNoUpdate, Consumer<String> onUpdateAvailable) {
        Thread updateThread = new Thread(() -> {
            try (HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()) {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(GITHUB_API_URL))
                        .header("User-Agent", "JavaJam-MusicPlayer")
                        .header("Accept", "application/vnd.github+json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String json = response.body();
                    String latestTag = extractFirstTagName(json);

                    if (latestTag != null && isNewerVersion(CURRENT_VERSION, latestTag)) {
                        SwingUtilities.invokeLater(() -> onUpdateAvailable.accept(latestTag));
                        return;
                    }
                }
            } catch (Exception e) {
                System.out.println("[UPDATE] Could not check for updates: " + e.getMessage());
            }

            SwingUtilities.invokeLater(onNoUpdate);
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    private static String extractFirstTagName(String json) {
        Pattern pattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static boolean isNewerVersion(String current, String latest) {
        String cleanCurrent = current.replaceAll("^v", "");
        String cleanLatest = latest.replaceAll("^v", "");

        String[] currParts = cleanCurrent.split("\\.");
        String[] latestParts = cleanLatest.split("\\.");

        int length = Math.max(currParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            int currPart = i < currParts.length ? parseVersionPart(currParts[i]) : 0;
            int latestPart = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;

            if (latestPart > currPart) {
                return true;
            } else if (latestPart < currPart) {
                return false;
            }
        }

        return false;
    }

    private static int parseVersionPart(String part) {
        try {
            String digits = part.replaceAll("\\D.*", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}