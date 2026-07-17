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

public class ForgeLauncher {

    private static final Path MC_DIR    = Paths.get(System.getProperty("user.home"), ".rocketclient", "minecraft");
    private static final Path VERSIONS  = MC_DIR.resolve("versions");
    private static final Path LIBRARIES = MC_DIR.resolve("libraries");
    private static final Path ASSETS    = MC_DIR.resolve("assets");
    private static final Path MODS_DIR  = MC_DIR.resolve("mods");
    private static final Gson GSON      = new Gson();

    private static final String FORGE_VERSION     = "1.8.9-11.15.1.2318-1.8.9";
    private static final String FORGE_INSTALLER   = "https://maven.minecraftforge.net/net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9-installer.jar";
    private static final String OPTIFINE_URL      = "https://optifine.net/adloadx?f=OptiFine_1.8.9_HD_U_H8.jar";
    private static final String OPTIFINE_FILENAME = "OptiFine_1.8.9_HD_U_H8.jar";

    public static void launch(AccountManager.Account account, SettingsManager settings, Consumer<String> log) throws Exception {
        Files.createDirectories(VERSIONS);
        Files.createDirectories(LIBRARIES);
        Files.createDirectories(ASSETS);
        Files.createDirectories(MODS_DIR);

        log.accept("Setting up Forge 1.8.9...");
        Path forgeDir = VERSIONS.resolve("1.8.9-forge-" + FORGE_VERSION);
        Files.createDirectories(forgeDir);

        Path forgeJson = forgeDir.resolve("1.8.9-forge-" + FORGE_VERSION + ".json");
        Path forgeJar  = forgeDir.resolve("1.8.9-forge-" + FORGE_VERSION + ".jar");

        if (!Files.exists(forgeJson)) {
            log.accept("Downloading Forge installer...");
            Path installer = forgeDir.resolve("forge-installer.jar");
            downloadFile(FORGE_INSTALLER, installer, log);

            log.accept("Installing Forge...");
            installForge(installer, forgeDir, log);
            Files.deleteIfExists(installer);
        } else {
            log.accept("Forge already installed, skipping.");
        }

        log.accept("Downloading vanilla 1.8.9 JAR...");
        Path vanillaJar = downloadVanillaJar(log);

        log.accept("Downloading Forge libraries...");
        List<Path> libs = downloadForgeLibraries(forgeJson, log);

        log.accept("Downloading assets...");
        downloadVanillaAssets(log);

        log.accept("Checking OptiFine...");
        Path optifine = MODS_DIR.resolve(OPTIFINE_FILENAME);
        if (!Files.exists(optifine)) {
            log.accept("Downloading OptiFine...");
            downloadFile(OPTIFINE_URL, optifine, log);
            log.accept("OptiFine installed.");
        } else {
            log.accept("OptiFine already installed.");
        }

        log.accept("Launching Minecraft 1.8.9 Forge...");
        startForge(account, settings, vanillaJar, forgeJar, forgeJson, libs, log);
    }

    private static void installForge(Path installer, Path forgeDir, Consumer<String> log) throws Exception {
    log.accept("Extracting Forge profile...");
    try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
            new java.io.BufferedInputStream(Files.newInputStream(installer)))) {
        java.util.zip.ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            String name = entry.getName();
            if (name.equals("version.json")) {
                Path dest = forgeDir.resolve("1.8.9-forge-" + FORGE_VERSION + ".json");
                Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                log.accept("Forge profile extracted.");
            }
            zis.closeEntry();
        }
    }
}
    private static Path downloadVanillaJar(Consumer<String> log) throws Exception {
        Path jar = VERSIONS.resolve("1.8.9").resolve("1.8.9.jar");
        if (Files.exists(jar)) return jar;
        Files.createDirectories(jar.getParent());

        String manifestUrl = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
        JsonObject manifest = fetchJson(manifestUrl);
        JsonArray versions = manifest.getAsJsonArray("versions");
        String versionUrl = null;
        for (var v : versions) {
            JsonObject vo = v.getAsJsonObject();
            if ("1.8.9".equals(vo.get("id").getAsString())) {
                versionUrl = vo.get("url").getAsString();
                break;
            }
        }
        if (versionUrl == null) throw new Exception("1.8.9 not found in manifest");
        JsonObject versionJson = fetchJson(versionUrl);
        String clientUrl = versionJson.getAsJsonObject("downloads").getAsJsonObject("client").get("url").getAsString();
        downloadFile(clientUrl, jar, log);
        return jar;
    }

    private static List<Path> downloadForgeLibraries(Path forgeJson, Consumer<String> log) throws Exception {
        List<Path> paths = new ArrayList<>();
        if (!Files.exists(forgeJson)) return paths;

        String content = new String(Files.readAllBytes(forgeJson));
        JsonObject profile = GSON.fromJson(content, JsonObject.class);
        if (!profile.has("libraries")) return paths;

        JsonArray libs = profile.getAsJsonArray("libraries");
        for (var lib : libs) {
            JsonObject lo = lib.getAsJsonObject();
            String name = lo.get("name").getAsString();
            String[] parts = name.split(":");
            if (parts.length < 3) continue;
            String group    = parts[0].replace(".", "/");
            String artifact = parts[1];
            String version  = parts[2];
            String fileName = artifact + "-" + version + ".jar";
            String path     = group + "/" + artifact + "/" + version + "/" + fileName;
            Path dest       = LIBRARIES.resolve(path);

            if (!Files.exists(dest)) {
                Files.createDirectories(dest.getParent());
                String url = "https://libraries.minecraft.net/" + path;
                try {
                    downloadFile(url, dest, log);
                } catch (Exception e) {
                    try {
                        downloadFile("https://maven.minecraftforge.net/" + path, dest, log);
                    } catch (Exception e2) {
                        log.accept("Skipping library: " + fileName);
                    }
                }
            }
            paths.add(dest);
        }
        return paths;
    }

    private static void downloadVanillaAssets(Consumer<String> log) throws Exception {
        String manifestUrl = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
        JsonObject manifest = fetchJson(manifestUrl);
        JsonArray versions = manifest.getAsJsonArray("versions");
        String versionUrl = null;
        for (var v : versions) {
            JsonObject vo = v.getAsJsonObject();
            if ("1.8.9".equals(vo.get("id").getAsString())) {
                versionUrl = vo.get("url").getAsString();
                break;
            }
        }
        if (versionUrl == null) return;
        JsonObject versionJson = fetchJson(versionUrl);
        JsonObject assetIndex = versionJson.getAsJsonObject("assetIndex");
        String indexId  = assetIndex.get("id").getAsString();
        String indexUrl = assetIndex.get("url").getAsString();

        Path indexFile = ASSETS.resolve("indexes").resolve(indexId + ".json");
        if (!Files.exists(indexFile)) {
            Files.createDirectories(indexFile.getParent());
            downloadFile(indexUrl, indexFile, log);
        }

        String indexContent = new String(Files.readAllBytes(indexFile));
        JsonObject objects = GSON.fromJson(indexContent, JsonObject.class).getAsJsonObject("objects");
        int total = objects.entrySet().size();
        int count = 0;
        for (var entry : objects.entrySet()) {
            JsonObject obj = entry.getValue().getAsJsonObject();
            String hash   = obj.get("hash").getAsString();
            String prefix = hash.substring(0, 2);
            Path dest     = ASSETS.resolve("objects").resolve(prefix).resolve(hash);
            if (!Files.exists(dest)) {
                Files.createDirectories(dest.getParent());
                downloadFile("https://resources.download.minecraft.net/" + prefix + "/" + hash, dest, log);
            }
            count++;
            if (count % 100 == 0) log.accept("Assets: " + count + "/" + total);
        }
        log.accept("Assets done.");
    }

    private static void startForge(AccountManager.Account account, SettingsManager settings,
        Path vanillaJar, Path forgeJar, Path forgeJson, List<Path> libs, Consumer<String> log) throws Exception {

        String content = new String(Files.readAllBytes(forgeJson));
        JsonObject profile = GSON.fromJson(content, JsonObject.class);
        String mainClass = profile.has("mainClass") ? profile.get("mainClass").getAsString() : "net.minecraft.client.main.Main";

        StringBuilder cp = new StringBuilder();
        for (Path lib : libs) {
            if (Files.exists(lib)) cp.append(lib.toAbsolutePath()).append(File.pathSeparator);
        }
        if (Files.exists(forgeJar)) cp.append(forgeJar.toAbsolutePath()).append(File.pathSeparator);
        cp.append(vanillaJar.toAbsolutePath());

        List<String> cmd = new ArrayList<>();
        cmd.add(settings.javaPath.equals("java") ? "java" : settings.javaPath);
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
        cmd.add(mainClass);
        cmd.add("--username");    cmd.add(account.username);
        cmd.add("--uuid");        cmd.add(account.uuid);
        cmd.add("--accessToken"); cmd.add(account.accessToken);
        cmd.add("--version");     cmd.add("1.8.9-forge-" + FORGE_VERSION);
        cmd.add("--gameDir");     cmd.add(MC_DIR.toAbsolutePath().toString());
        cmd.add("--assetsDir");   cmd.add(ASSETS.toAbsolutePath().toString());
        cmd.add("--assetIndex");  cmd.add("1.8");
        cmd.add("--userType");    cmd.add(account.type.equals("microsoft") ? "msa" : "legacy");
        cmd.add("--tweakClass");  cmd.add("net.minecraftforge.fml.common.launcher.FMLTweaker");

        log.accept("Starting Forge...");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        log.accept("Minecraft 1.8.9 Forge launched!");

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) log.accept(line);
            } catch (Exception e) {
                log.accept("Log stream ended.");
            }
        }).start();

        new Thread(() -> {
            try {
                process.waitFor();
                log.accept("Minecraft exited.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                javafx.application.Platform.exit();
                System.exit(0);
            }
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
