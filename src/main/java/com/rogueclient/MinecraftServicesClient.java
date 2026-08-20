package com.rogueclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Thin wrapper around the official api.minecraftservices.com profile endpoints -
 * reading owned skins/capes and changing which one is active. Only works for
 * Microsoft accounts, since the bearer token has to be a real Xbox-issued
 * Minecraft Services token; Ely.by accounts don't have a profile here at all.
 */
public class MinecraftServicesClient {

    private static final String PROFILE_URL      = "https://api.minecraftservices.com/minecraft/profile";
    private static final String SKINS_URL        = "https://api.minecraftservices.com/minecraft/profile/skins";
    private static final String CAPE_ACTIVE_URL  = "https://api.minecraftservices.com/minecraft/profile/capes/active";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public static class SkinEntry {
        public String id;
        public String state;   // "ACTIVE" or "INACTIVE"
        public String url;
        public String variant; // "CLASSIC" or "SLIM"
        public boolean active() { return "ACTIVE".equalsIgnoreCase(state); }
    }

    public static class CapeEntry {
        public String id;
        public String state;
        public String url;
        public String alias;
        public boolean active() { return "ACTIVE".equalsIgnoreCase(state); }
    }

    public static class Profile {
        public String name;
        public List<SkinEntry> skins = new ArrayList<>();
        public List<CapeEntry> capes = new ArrayList<>();
    }

    /** Fetches the account's current skins + capes. Throws with a readable message on failure. */
    public static Profile fetchProfile(String accessToken) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(PROFILE_URL))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 401) {
            throw new RuntimeException("session expired - log out and back in with Microsoft");
        }
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Mojang returned " + resp.statusCode() + " while fetching profile");
        }

        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
        Profile profile = new Profile();
        profile.name = root.has("name") ? root.get("name").getAsString() : "";

        if (root.has("skins")) {
            for (com.google.gson.JsonElement el : root.getAsJsonArray("skins")) {
                JsonObject o = el.getAsJsonObject();
                SkinEntry s = new SkinEntry();
                s.id = o.get("id").getAsString();
                s.state = o.get("state").getAsString();
                s.url = o.get("url").getAsString();
                s.variant = o.has("variant") ? o.get("variant").getAsString() : "CLASSIC";
                profile.skins.add(s);
            }
        }
        if (root.has("capes")) {
            for (com.google.gson.JsonElement el : root.getAsJsonArray("capes")) {
                JsonObject o = el.getAsJsonObject();
                CapeEntry c = new CapeEntry();
                c.id = o.get("id").getAsString();
                c.state = o.get("state").getAsString();
                c.url = o.get("url").getAsString();
                c.alias = o.has("alias") ? o.get("alias").getAsString() : "Cape";
                profile.capes.add(c);
            }
        }
        return profile;
    }

    /** Re-applies a previously-owned skin by URL (Mojang has no "activate by id" for skins - re-submitting equips it). */
    public static void activateSkinByUrl(String accessToken, String url, String variant) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("variant", variant == null ? "classic" : variant.toLowerCase());
        body.addProperty("url", url);

        HttpRequest req = HttpRequest.newBuilder(URI.create(SKINS_URL))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("Failed to activate skin (HTTP " + resp.statusCode() + "): " + resp.body());
        }
    }

    /** Uploads a local skin file and equips it. */
    public static void uploadSkin(String accessToken, Path file, String variant) throws Exception {
        String boundary = "RogueClient-" + UUID.randomUUID();
        byte[] fileBytes = Files.readAllBytes(file);
        String filename = file.getFileName().toString();

        String header =
            "--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"variant\"\r\n\r\n" +
            (variant == null ? "classic" : variant.toLowerCase()) + "\r\n" +
            "--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n" +
            "Content-Type: image/png\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";

        byte[] headerBytes = header.getBytes("UTF-8");
        byte[] footerBytes = footer.getBytes("UTF-8");
        byte[] payload = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, payload, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, payload, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, payload, headerBytes.length + fileBytes.length, footerBytes.length);

        HttpRequest req = HttpRequest.newBuilder(URI.create(SKINS_URL))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
            .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("Failed to upload skin (HTTP " + resp.statusCode() + "): " + resp.body());
        }
    }

    public static void activateCape(String accessToken, String capeId) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("capeId", capeId);

        HttpRequest req = HttpRequest.newBuilder(URI.create(CAPE_ACTIVE_URL))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("Failed to equip cape (HTTP " + resp.statusCode() + "): " + resp.body());
        }
    }

    public static void removeCape(String accessToken) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(CAPE_ACTIVE_URL))
            .header("Authorization", "Bearer " + accessToken)
            .DELETE()
            .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("Failed to remove cape (HTTP " + resp.statusCode() + "): " + resp.body());
        }
    }
}
