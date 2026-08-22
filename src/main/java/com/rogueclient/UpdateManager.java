package com.rogueclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UpdateManager {

    // TODO: replace with the real deployed Worker URL for Rogue Client's API,
    // e.g. "https://rogue-client-api.pernoise.workers.dev/api/version"
    private static final String API_URL = "https://rocket-client-api.pernoise.workers.dev/api/version";

    private static final String WEBSITE_URL = "https://rocket.pernoise.workers.dev/";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build();

    public static class UpdateInfo {
        public final String latestVersion;
        public final String websiteUrl;
        public UpdateInfo(String latestVersion, String websiteUrl) {
            this.latestVersion = latestVersion;
            this.websiteUrl = websiteUrl;
        }
    }

    public static UpdateInfo checkForUpdate() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(API_URL))
                .header("User-Agent", "RogueClient/" + AppVersion.CURRENT)
                .GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;

            JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (!json.has("version") || json.get("version").isJsonNull()) {
                return null;
            }
            String latest = json.get("version").getAsString();

            if (isNewer(latest, AppVersion.CURRENT)) {
                return new UpdateInfo(latest, WEBSITE_URL);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    static boolean isNewer(String latest, String current) {
        if (latest == null || current == null) return false;
        if ("dev".equals(current)) return false;

        int[] latestParts = parseVersion(latest);
        int[] currentParts = parseVersion(current);

        int len = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < len; i++) {
            int l = i < latestParts.length ? latestParts[i] : 0;
            int c = i < currentParts.length ? currentParts[i] : 0;
            if (l != c) return l > c;
        }
        return false;
    }

    private static int[] parseVersion(String v) {
        String numeric = v.split("-")[0];
        String[] segs = numeric.split("\\.");
        int[] out = new int[segs.length];
        for (int i = 0; i < segs.length; i++) {
            try {
                out[i] = Integer.parseInt(segs[i].replaceAll("[^0-9]", ""));
            } catch (Exception e) {
                out[i] = 0;
            }
        }
        return out;
    }
}
