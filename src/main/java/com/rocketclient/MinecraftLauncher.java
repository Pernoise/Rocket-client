package com.rocketclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class MinecraftLauncher {

    private static final Path MC_DIR     = Paths.get(System.getProperty("user.home"), ".rocketclient", "minecraft");
    private static final Path VERSIONS   = MC_DIR.resolve("versions");
    private static final Path LIBRARIES  = MC_DIR.resolve("libraries");
    private static final Path ASSETS     = MC_DIR.resolve("assets");
    private static final Path FABRIC_DIR = MC_DIR.resolve("fabric");
    private static final Gson GSON       = new Gson();

    private static final String VERSION_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final String FABRIC_META      = "https://meta.fabricmc.net/v2/versions/loader";

    public static void launch(String mcVersion, AccountManager.Account account, SettingsManager settings) throws Exception {
        Files.createDirectories(VERSIONS);
        Files.createDirectories(LIBRARIES);
        Files.createDirectories(ASSETS);
        Files.createDirectories(FABRIC_DIR);

        System.out.println("Fetching version manifest...");
        String versionUrl = getVersionUrl(mcVersion);

        System.out.println("Downloading version JSON...");
        JsonObject versionJson = fetchJson(versionUrl);

        System.out.println("Downloading client JAR...");
        Path clientJar = downloadClientJar(mcVersion, versionJson);

        System.out.println("Downloading libraries...");
        List<Path> libs = downloadLibraries(versionJson);

        System.out.println("Downloading assets...");
        downloadAssets(versionJson);

        System.out.println("Fetching Fabric loader...");
        Path fabricJar = downloadFabric(mcVersion);

        System.out.println("Launching...");
        startProcess(mcVersion, account, settings, clientJar, fabricJar, libs, versionJson);
    }

    private static String getVersionUrl(String mcVersion) throws Exception {
        JsonObject manifest = fetchJson(VERSION_MANIFEST);
        JsonArray versions = manifest.getAsJsonArray("versions");
        for (var v : versions) {
            JsonObject vo = v.getAsJsonObject();
            if (vo.get("id").getAsString().equals(mcVersion)) {
                return vo.get("url").getAsString();
            }
        }
        throw new Exception("Version not found: " + mcVersion);
    }

    private static Path downloadClientJar(String mcVersion, JsonObject versionJson) throws Exception {
        Path jar = VERSIONS.resolve(mcVersion).resolve(mcVersion + ".jar");
        if (Files.exists(jar)) return jar;
        Files.createDirectories(jar.getParent());
        String url = versionJson.getAsJsonObject("downloads")
            .getAsJsonObject("client").get("url").getAsString();
        downloadFile(url, jar);
        return jar;
    }

    private static List<Path> downloadLibraries(JsonObject versionJson) throws Exception {
        List<Path> paths = new ArrayList<>();
        JsonArray libs = versionJson.getAsJsonArray("libraries");
        for (var lib : libs) {
            JsonObject lo = lib.getAsJsonObject();
            if (lo.has("downloads")) {
                JsonObject artifact = lo.getAsJsonObject("downloads").getAsJsonObject("artifact");
                if (artifact != null) {
                    String path = artifact.get("path").getAsString();
                    String url  = artifact.get("url").getAsString();
                    Path dest   = LIBRARIES.resolve(path);
                    if (!Files.exists(dest)) {
                        Files.createDirectories(dest.getParent());
                        downloadFile(url, dest);
                    }
                    paths.add(dest);
                }
            }
        }
        return paths;
    }

    private static void downloadAssets(JsonObject versionJson) throws Exception {
        JsonObject assetIndex = versionJson.getAsJsonObject("assetIndex");
        String indexId  = assetIndex.get("id").getAsString();
        String indexUrl = assetIndex.get("url").getAsString();

        Path indexFile = ASSETS.resolve("indexes").resolve(indexId + ".json");
        if (!Files.exists(indexFile)) {
            Files.createDirectories(indexFile.getParent());
            downloadFile(indexUrl, indexFile);
        }

        String indexContent = new String(Files.readAllBytes(indexFile));
        JsonObject objects = GSON.fromJson(indexContent, JsonObject.class)
            .getAsJsonObject("objects");

        int total = objects.entrySet().size();
        int count = 0;
        for (var entry : objects.entrySet()) {
            JsonObject obj  = entry.getValue().getAsJsonObject();
            String hash     = obj.get("hash").getAsString();
            String prefix   = hash.substring(0, 2);
            Path dest       = ASSETS.resolve("objects").resolve(prefix).resolve(hash);
            if (!Files.exists(dest)) {
                Files.createDirectories(dest.getParent());
                downloadFile("https://resources.download.minecraft.net/" + prefix + "/" + hash, dest);
            }
            count++;
            if (count % 50 == 0) System.out.println("Assets: " + count + "/" + total);
        }
        System.out.println("Assets done: " + total + " files.");
    }

    private static Path downloadFabric(String mcVersion) throws Exception {
        Path fabricJar = FABRIC_DIR.resolve("fabric-loader-" + mcVersion + ".jar");

        String loaderUrl = FABRIC_META + "/" + mcVersion;
        String loaderResponse = fetch(loaderUrl);
        JsonArray loaders = GSON.fromJson(loaderResponse, JsonArray.class);
        if (loaders.size() == 0) throw new Exception("No Fabric loader found for " + mcVersion);

        JsonObject loader = loaders.get(0).getAsJsonObject();
        String loaderVersion = loader.getAsJsonObject("loader").get("version").getAsString();

        String launchMeta = "https://meta.fabricmc.net/v2/versions/loader/" + mcVersion + "/" + loaderVersion + "/profile/json";
        JsonObject profile = fetchJson(launchMeta);

        Path profilePath = FABRIC_DIR.resolve("fabric-" + mcVersion + ".json");
        Files.write(profilePath, GSON.toJson(profile).getBytes());

        JsonArray fabricLibs = profile.getAsJsonArray("libraries");
        Path lastJar = null;
        for (var lib : fabricLibs) {
            JsonObject lo = lib.getAsJsonObject();
            if (lo.has("url")) {
                String name     = lo.get("name").getAsString();
                String[] parts  = name.split(":");
                String group    = parts[0].replace(".", "/");
                String artifact = parts[1];
                String version  = parts[2];
                String fileName = artifact + "-" + version + ".jar";
                String path     = group + "/" + artifact + "/" + version + "/" + fileName;
                Path dest       = LIBRARIES.resolve(path);
                if (!Files.exists(dest)) {
                    Files.createDirectories(dest.getParent());
                    String baseUrl = lo.get("url").getAsString();
                    downloadFile(baseUrl + path, dest);
                }
                if (fileName.contains("loader")) lastJar = dest;
            }
        }
        return lastJar != null ? lastJar : fabricJar;
    }

    private static void startProcess(String mcVersion, AccountManager.Account account,
        SettingsManager settings, Path clientJar, Path fabricJar,
        List<Path> libs, JsonObject versionJson) throws Exception {

        StringBuilder cp = new StringBuilder();
        for (Path lib : libs) cp.append(lib.toAbsolutePath()).append(File.pathSeparator);
        cp.append(clientJar.toAbsolutePath());
        if (fabricJar != null && Files.exists(fabricJar)) {
            cp.append(File.pathSeparator).append(fabricJar.toAbsolutePath());
        }

        Path profilePath = FABRIC_DIR.resolve("fabric-" + mcVersion + ".json");
        if (Files.exists(profilePath)) {
            String profileContent = new String(Files.readAllBytes(profilePath));
            JsonObject profile = GSON.fromJson(profileContent, JsonObject.class);
            JsonArray fabricLibs = profile.getAsJsonArray("libraries");
            for (var lib : fabricLibs) {
                JsonObject lo = lib.getAsJsonObject();
                if (lo.has("url")) {
                    String name     = lo.get("name").getAsString();
                    String[] parts  = name.split(":");
                    String group    = parts[0].replace(".", "/");
                    String artifact = parts[1];
                    String version  = parts[2];
                    String fileName = artifact + "-" + version + ".jar";
                    String path     = group + "/" + artifact + "/" + version + "/" + fileName;
                    Path dest       = LIBRARIES.resolve(path);
                    if (Files.exists(dest)) {
                        cp.append(File.pathSeparator).append(dest.toAbsolutePath());
                    }
                }
            }
            String mainClass = profile.get("mainClass").getAsString();

            List<String> cmd = new ArrayList<>();
            cmd.add(settings.javaPath);
            cmd.add("-Xmx" + settings.ramMb + "M");
            cmd.add("-Xms512M");

            if (settings.javaArgs != null && !settings.javaArgs.isEmpty()) {
                for (String arg : settings.javaArgs.split(" ")) {
                    if (!arg.isEmpty()) cmd.add(arg);
                }
            }

            cmd.add("-cp"); cmd.add(cp.toString());
            cmd.add(mainClass);
            cmd.add("--username");    cmd.add(account.username);
            cmd.add("--uuid");        cmd.add(account.uuid);
            cmd.add("--accessToken"); cmd.add(account.accessToken);
            cmd.add("--version");     cmd.add(mcVersion);
            cmd.add("--gameDir");     cmd.add(MC_DIR.toAbsolutePath().toString());
            cmd.add("--assetsDir");   cmd.add(ASSETS.toAbsolutePath().toString());
            cmd.add("--assetIndex");  cmd.add(versionJson.getAsJsonObject("assetIndex").get("id").getAsString());
            cmd.add("--userType");    cmd.add(account.type.equals("microsoft") ? "msa" : "legacy");

            System.out.println("Launching: " + String.join(" ", cmd));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.inheritIO();
            Process process = pb.start();
            System.out.println("Minecraft launched!");

            new Thread(() -> {
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    javafx.application.Platform.exit();
                    System.exit(0);
                }
            }).start();
        }
    }

    private static JsonObject fetchJson(String url) throws Exception {
        return GSON.fromJson(fetch(url), JsonObject.class);
    }

    private static String fetch(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "RocketClient/0.1");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        return new String(conn.getInputStream().readAllBytes());
    }

    private static void downloadFile(String urlStr, Path dest) throws Exception {
        System.out.println("Downloading: " + dest.getFileName());
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "RocketClient/0.1");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
