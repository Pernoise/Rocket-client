package com.rocketclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.*;

public class ForgeLauncher {

    private static final Path MC_DIR    = Paths.get(System.getProperty("user.home"), ".rocketclient", "minecraft");
    private static final Path VERSIONS  = MC_DIR.resolve("versions");
    private static final Path LIBRARIES = MC_DIR.resolve("libraries");
    private static final Path ASSETS    = MC_DIR.resolve("assets");
    private static final Path MODS_DIR  = MC_DIR.resolve("mods");
    private static final Gson GSON      = new Gson();

    private static final String FORGE_VERSION   = "1.8.9-11.15.1.2318-1.8.9";
    private static final String FORGE_INSTALLER = "https://maven.minecraftforge.net/net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9-installer.jar";
    private static final String OPTIFINE_URL    = "https://optifine.net/adloadx?f=OptiFine_1.8.9_HD_U_H8.jar";
    private static final String OPTIFINE_FILE   = "OptiFine_1.8.9_HD_U_H8.jar";

    public static void launch(AccountManager.Account account, SettingsManager settings, Consumer<String> log) throws Exception {
        Files.createDirectories(VERSIONS);
        Files.createDirectories(LIBRARIES);
        Files.createDirectories(ASSETS);
        Files.createDirectories(MODS_DIR);

        log.accept("Setting up Forge 1.8.9...");
        Path forgeDir  = VERSIONS.resolve("1.8.9-forge-" + FORGE_VERSION);
        Path forgeJson = forgeDir.resolve("1.8.9-forge-" + FORGE_VERSION + ".json");
        Path forgeJar  = forgeDir.resolve("1.8.9-forge-" + FORGE_VERSION + ".jar");
        Files.createDirectories(forgeDir);

        if (!Files.exists(forgeJson)) {
            log.accept("Downloading Forge installer...");
            Path installer = forgeDir.resolve("forge-installer.jar");
            downloadFile(FORGE_INSTALLER, installer, log);
            log.accept("Extracting Forge profile...");
            extractFromZip(installer, "install_profile.json", forgeJson);
            extractFromZip(installer, "forge-1.8.9-11.15.1.2318-1.8.9-universal.jar", forgeJar);
            Files.deleteIfExists(installer);
            log.accept("Forge installed.");
        } else {
            log.accept("Forge already installed.");
        }

        log.accept("Downloading vanilla 1.8.9 JAR...");
        Path vanillaJar = downloadVanillaJar(log);

        log.accept("Downloading Forge libraries...");
        List<Path> libs = downloadForgeLibraries(forgeJson, log);

        log.accept("Downloading assets...");
        downloadAssets(log);

        log.accept("Downloading required libraries...");
        downloadRequiredLibs(log);

        log.accept("Checking OptiFine...");
        Path optifine = MODS_DIR.resolve(OPTIFINE_FILE);
        if (!Files.exists(optifine)) {
            log.accept("Downloading OptiFine...");
            downloadFile(OPTIFINE_URL, optifine, log);
        } else {
            log.accept("OptiFine already installed.");
        }

        log.accept("Launching Minecraft 1.8.9 Forge...");
        startForge(account, settings, vanillaJar, forgeJar, forgeJson, libs, log);
    }

    private static void extractFromZip(Path zip, String entryName, Path dest) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zip)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
                zis.closeEntry();
            }
        }
    }

    private static void downloadRequiredLibs(Consumer<String> log) throws Exception {
        downloadLib("https://libraries.minecraft.net/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar",
            "net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar", log);
        downloadLib("https://libraries.minecraft.net/net/sf/jopt-simple/jopt-simple/4.6/jopt-simple-4.6.jar",
            "net/sf/jopt-simple/jopt-simple/4.6/jopt-simple-4.6.jar", log);
        downloadLib("https://libraries.minecraft.net/org/apache/logging/log4j/log4j-api/2.0-beta9/log4j-api-2.0-beta9.jar",
            "org/apache/logging/log4j/log4j-api/2.0-beta9/log4j-api-2.0-beta9.jar", log);
        downloadLib("https://libraries.minecraft.net/org/apache/logging/log4j/log4j-core/2.0-beta9/log4j-core-2.0-beta9.jar",
            "org/apache/logging/log4j/log4j-core/2.0-beta9/log4j-core-2.0-beta9.jar", log);
        downloadLib("https://libraries.minecraft.net/org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar",
            "org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar", log);
        downloadLib("https://libraries.minecraft.net/com/google/guava/guava/17.0/guava-17.0.jar",
            "com/google/guava/guava/17.0/guava-17.0.jar", log);
        downloadLib("https://libraries.minecraft.net/com/mojang/authlib/1.5.21/authlib-1.5.21.jar",
            "com/mojang/authlib/1.5.21/authlib-1.5.21.jar", log);
    }

    private static void downloadLib(String url, String path, Consumer<String> log) throws Exception {
        Path dest = LIBRARIES.resolve(path);
        if (Files.exists(dest)) return;
        Files.createDirectories(dest.getParent());
        try {
            downloadFile(url, dest, log);
        } catch (Exception e) {
            log.accept("Could not download: " + dest.getFileName());
        }
    }

    private static Path downloadVanillaJar(Consumer<String> log) throws Exception {
        Path jar = VERSIONS.resolve("1.8.9").resolve("1.8.9.jar");
        if (Files.exists(jar)) return jar;
        Files.createDirectories(jar.getParent());
        JsonObject manifest = fetchJson("https://launchermeta.mojang.com/mc/game/version_manifest.json");
        JsonArray versions = manifest.getAsJsonArray("versions");
        for (var v : versions) {
            JsonObject vo = v.getAsJsonObject();
            if ("1.8.9".equals(vo.get("id").getAsString())) {
                JsonObject vJson = fetchJson(vo.get("url").getAsString());
                String url = vJson.getAsJsonObject("downloads").getAsJsonObject("client").get("url").getAsString();
                downloadFile(url, jar, log);
                return jar;
            }
        }
        throw new Exception("1.8.9 not found");
    }

    private static List<Path> downloadForgeLibraries(Path forgeJson, Consumer<String> log) throws Exception {
        List<Path> paths = new ArrayList<>();
        if (!Files.exists(forgeJson)) return paths;
        JsonObject profile = GSON.fromJson(new String(Files.readAllBytes(forgeJson)), JsonObject.class);
        JsonObject versionInfo = profile.has("versionInfo") ? profile.getAsJsonObject("versionInfo") : profile;
        if (!versionInfo.has("libraries")) return paths;
        JsonArray libs = versionInfo.getAsJsonArray("libraries");
        for (var lib : libs) {
            JsonObject lo = lib.getAsJsonObject();
            String name = lo.get("name").getAsString();
            String[] parts = name.split(":");
            if (parts.length < 3) continue;
            String group    = parts[0].replace(".", "/");
            String artifact = parts[1];
            String version  = parts[2];
            String fileName = artifact + "-" + version + ".jar";
            String relPath  = group + "/" + artifact + "/" + version + "/" + fileName;
            Path dest = LIBRARIES.resolve(relPath);
            if (!Files.exists(dest)) {
                Files.createDirectories(dest.getParent());
                String url = lo.has("url") ? lo.get("url").getAsString() + relPath
                    : "https://libraries.minecraft.net/" + relPath;
                try { downloadFile(url, dest, log); }
                catch (Exception e) {
                    try { downloadFile("https://maven.minecraftforge.net/" + relPath, dest, log); }
                    catch (Exception e2) { log.accept("Skipping: " + fileName); }
                }
            }
            paths.add(dest);
        }
        return paths;
    }

    private static void downloadAssets(Consumer<String> log) throws Exception {
        JsonObject manifest = fetchJson("https://launchermeta.mojang.com/mc/game/version_manifest.json");
        JsonArray versions = manifest.getAsJsonArray("versions");
        for (var v : versions) {
            JsonObject vo = v.getAsJsonObject();
            if ("1.8.9".equals(vo.get("id").getAsString())) {
                JsonObject vJson = fetchJson(vo.get("url").getAsString());
                JsonObject assetIndex = vJson.getAsJsonObject("assetIndex");
                String indexId  = assetIndex.get("id").getAsString();
                String indexUrl = assetIndex.get("url").getAsString();
                Path indexFile = ASSETS.resolve("indexes").resolve(indexId + ".json");
                if (!Files.exists(indexFile)) {
                    Files.createDirectories(indexFile.getParent());
                    downloadFile(indexUrl, indexFile, log);
                }
                JsonObject objects = GSON.fromJson(new String(Files.readAllBytes(indexFile)), JsonObject.class).getAsJsonObject("objects");
                int total = objects.entrySet().size(), count = 0;
                for (var entry : objects.entrySet()) {
                    String hash = entry.getValue().getAsJsonObject().get("hash").getAsString();
                    String prefix = hash.substring(0, 2);
                    Path dest = ASSETS.resolve("objects").resolve(prefix).resolve(hash);
                    if (!Files.exists(dest)) {
                        Files.createDirectories(dest.getParent());
                        downloadFile("https://resources.download.minecraft.net/" + prefix + "/" + hash, dest, log);
                    }
                    if (++count % 100 == 0) log.accept("Assets: " + count + "/" + total);
                }
                log.accept("Assets done.");
                return;
            }
        }
    }

    private static void startForge(AccountManager.Account account, SettingsManager settings,
        Path vanillaJar, Path forgeJar, Path forgeJson, List<Path> libs, Consumer<String> log) throws Exception {

        StringBuilder cp = new StringBuilder();
        for (Path lib : libs) {
            if (Files.exists(lib)) cp.append(lib.toAbsolutePath()).append(File.pathSeparator);
        }

        String[] requiredLibs = {
            "net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar",
            "net/sf/jopt-simple/jopt-simple/4.6/jopt-simple-4.6.jar",
            "org/apache/logging/log4j/log4j-api/2.0-beta9/log4j-api-2.0-beta9.jar",
            "org/apache/logging/log4j/log4j-core/2.0-beta9/log4j-core-2.0-beta9.jar",
            "org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar",
            "com/google/guava/guava/17.0/guava-17.0.jar",
            "com/mojang/authlib/1.5.21/authlib-1.5.21.jar"
        };
        for (String rel : requiredLibs) {
            Path p = LIBRARIES.resolve(rel);
            if (Files.exists(p)) cp.append(p.toAbsolutePath()).append(File.pathSeparator);
        }

        if (Files.exists(forgeJar)) cp.append(forgeJar.toAbsolutePath()).append(File.pathSeparator);
        cp.append(vanillaJar.toAbsolutePath());

        String javaExec;
        try {
            javaExec = JavaManager.getJavaForVersion("1.8.9", log);
        } catch (Exception e) {
            javaExec = "java";
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(javaExec);
        cmd.add("-Xmx" + settings.ramMb + "M");
        cmd.add("-Xms512M");
        cmd.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
        cmd.add("-Dfml.ignorePatchDiscrepancies=true");

        if (settings.javaArgs != null && !settings.javaArgs.isEmpty()) {
            for (String arg : settings.javaArgs.split(" ")) {
                if (!arg.isEmpty()) cmd.add(arg);
            }
        }

        cmd.add("-cp"); cmd.add(cp.toString());
        cmd.add("net.minecraft.launchwrapper.Launch");
        cmd.add("--username");    cmd.add(account.username);
        cmd.add("--uuid");        cmd.add(account.uuid);
        cmd.add("--accessToken"); cmd.add(account.accessToken);
        cmd.add("--version");     cmd.add("1.8.9-forge-" + FORGE_VERSION);
        cmd.add("--gameDir");     cmd.add(MC_DIR.toAbsolutePath().toString());
        cmd.add("--assetsDir");   cmd.add(ASSETS.toAbsolutePath().toString());
        cmd.add("--assetIndex");  cmd.add("1.8");
        cmd.add("--userType");    cmd.add(account.type.equals("microsoft") ? "msa" : "legacy");
        cmd.add("--tweakClass");  cmd.add("net.minecraftforge.fml.common.launcher.FMLTweaker");

        log.accept("Command: " + String.join(" ", cmd));

        Files.createDirectories(MC_DIR.resolve("logs"));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        pb.redirectOutput(MC_DIR.resolve("logs").resolve("forge-latest.log").toFile());
        Process process = pb.start();
        log.accept("Minecraft 1.8.9 Forge launched!");

        new Thread(() -> {
            try { process.waitFor(); log.accept("Minecraft exited."); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { javafx.application.Platform.exit(); System.exit(0); }
        }).start();
    }

    private static JsonObject fetchJson(String url) throws Exception {
        return GSON.fromJson(fetch(url), JsonObject.class);
    }

    private static String fetch(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "RocketClient/0.1");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        return new String(conn.getInputStream().readAllBytes());
    }

    private static void downloadFile(String urlStr, Path dest, Consumer<String> log) throws Exception {
        log.accept("Downloading: " + dest.getFileName());
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "RocketClient/0.1");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setInstanceFollowRedirects(true);
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
