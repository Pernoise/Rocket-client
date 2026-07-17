package com.rocketclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MinecraftLauncher {

    private static final Path MC_DIR     = Paths.get(System.getProperty("user.home"), ".rocketclient", "minecraft");
    private static final Path VERSIONS   = MC_DIR.resolve("versions");
    private static final Path LIBRARIES  = MC_DIR.resolve("libraries");
    private static final Path ASSETS     = MC_DIR.resolve("assets");
    private static final Path FABRIC_DIR = MC_DIR.resolve("fabric");
    private static final Gson GSON       = new Gson();
    private static final int  DOWNLOAD_THREADS = 16;

    private static final int  JAVA_FEATURE_VERSION = 26;
    private static final Path JRE_DIR = Paths.get(System.getProperty("user.home"), ".rocketclient", "jre", "jdk" + JAVA_FEATURE_VERSION);

    private static final String VERSION_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final String FABRIC_META      = "https://meta.fabricmc.net/v2/versions/loader";

    public static void launch(String mcVersion, AccountManager.Account account, SettingsManager settings, Consumer<String> log) throws Exception {
        Files.createDirectories(VERSIONS);
        Files.createDirectories(LIBRARIES);
        Files.createDirectories(ASSETS);
        Files.createDirectories(FABRIC_DIR);

        log.accept("Fetching version manifest...");
        String versionUrl = getVersionUrl(mcVersion, log);

        log.accept("Downloading version JSON...");
        JsonObject versionJson = fetchJson(versionUrl);

        log.accept("Downloading client JAR...");
        Path clientJar = downloadClientJar(mcVersion, versionJson, log);

        log.accept("Downloading libraries...");
        List<Path> libs = downloadLibraries(versionJson, log);

        log.accept("Downloading assets...");
        downloadAssets(versionJson, log);

        log.accept("Fetching Fabric loader...");
        Path fabricJar = downloadFabric(mcVersion, log);

        log.accept("Checking Java runtime...");
        String javaExec = "java".equals(settings.javaPath) ? ensureJava(log) : settings.javaPath;

        log.accept("Launching Minecraft...");
        startProcess(mcVersion, account, settings, clientJar, fabricJar, libs, versionJson, javaExec, log);
    }

    private static String getVersionUrl(String mcVersion, Consumer<String> log) throws Exception {
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

    private static Path downloadClientJar(String mcVersion, JsonObject versionJson, Consumer<String> log) throws Exception {
        Path jar = VERSIONS.resolve(mcVersion).resolve(mcVersion + ".jar");
        if (Files.exists(jar)) {
            log.accept("Client JAR already exists, skipping.");
            return jar;
        }
        Files.createDirectories(jar.getParent());
        String url = versionJson.getAsJsonObject("downloads")
            .getAsJsonObject("client").get("url").getAsString();
        log.accept("Downloading client JAR...");
        downloadFile(url, jar, log);
        return jar;
    }

    private static List<Path> downloadLibraries(JsonObject versionJson, Consumer<String> log) throws Exception {
        JsonArray libs = versionJson.getAsJsonArray("libraries");
        int total = libs.size();

        List<Path> paths = java.util.Collections.synchronizedList(new ArrayList<>());
        AtomicInteger count = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(DOWNLOAD_THREADS);
        List<Future<?>> futures = new ArrayList<>();

        for (var lib : libs) {
            JsonObject lo = lib.getAsJsonObject();
            futures.add(pool.submit(() -> {
                try {
                    if (lo.has("downloads")) {
                        JsonObject artifact = lo.getAsJsonObject("downloads").getAsJsonObject("artifact");
                        if (artifact != null) {
                            String path = artifact.get("path").getAsString();
                            String url  = artifact.get("url").getAsString();
                            Path dest   = LIBRARIES.resolve(path);
                            if (!Files.exists(dest)) {
                                Files.createDirectories(dest.getParent());
                                downloadFile(url, dest, log);
                            }
                            paths.add(dest);
                        }
                    }
                    int c = count.incrementAndGet();
                    if (c % 10 == 0) log.accept("Libraries: " + c + "/" + total);
                } catch (Exception e) {
                    log.accept("Failed to download library: " + e.getMessage());
                }
            }));
        }

        pool.shutdown();
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                log.accept("Library download error: " + e.getMessage());
            }
        }
        pool.awaitTermination(60, TimeUnit.SECONDS);

        log.accept("Libraries done: " + total + " files.");
        return paths;
    }

    private static void downloadAssets(JsonObject versionJson, Consumer<String> log) throws Exception {
        JsonObject assetIndex = versionJson.getAsJsonObject("assetIndex");
        String indexId  = assetIndex.get("id").getAsString();
        String indexUrl = assetIndex.get("url").getAsString();

        Path indexFile = ASSETS.resolve("indexes").resolve(indexId + ".json");
        if (!Files.exists(indexFile)) {
            Files.createDirectories(indexFile.getParent());
            downloadFile(indexUrl, indexFile, log);
        }

        String indexContent = new String(Files.readAllBytes(indexFile));
        JsonObject objects = GSON.fromJson(indexContent, JsonObject.class)
            .getAsJsonObject("objects");

        int total = objects.entrySet().size();
        AtomicInteger count = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(DOWNLOAD_THREADS);
        List<Future<?>> futures = new ArrayList<>();

        for (var entry : objects.entrySet()) {
            JsonObject obj = entry.getValue().getAsJsonObject();
            futures.add(pool.submit(() -> {
                try {
                    String hash   = obj.get("hash").getAsString();
                    String prefix = hash.substring(0, 2);
                    Path dest     = ASSETS.resolve("objects").resolve(prefix).resolve(hash);
                    if (!Files.exists(dest)) {
                        Files.createDirectories(dest.getParent());
                        downloadFile("https://resources.download.minecraft.net/" + prefix + "/" + hash, dest, log);
                    }
                    int c = count.incrementAndGet();
                    if (c % 50 == 0) log.accept("Assets: " + c + "/" + total);
                } catch (Exception e) {
                    log.accept("Failed to download asset: " + e.getMessage());
                }
            }));
        }

        pool.shutdown();
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                log.accept("Asset download error: " + e.getMessage());
            }
        }
        pool.awaitTermination(120, TimeUnit.SECONDS);

        log.accept("Assets done: " + total + " files.");
    }

    private static Path downloadFabric(String mcVersion, Consumer<String> log) throws Exception {
        Path fabricJar = FABRIC_DIR.resolve("fabric-loader-" + mcVersion + ".jar");

        String loaderUrl = FABRIC_META + "/" + mcVersion;
        String loaderResponse = fetch(loaderUrl);
        JsonArray loaders = GSON.fromJson(loaderResponse, JsonArray.class);
        if (loaders.size() == 0) throw new Exception("No Fabric loader found for " + mcVersion);

        JsonObject loader = loaders.get(0).getAsJsonObject();
        String loaderVersion = loader.getAsJsonObject("loader").get("version").getAsString();
        log.accept("Fabric loader version: " + loaderVersion);

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
                    downloadFile(baseUrl + path, dest, log);
                }
                if (fileName.contains("loader")) lastJar = dest;
            }
        }
        return lastJar != null ? lastJar : fabricJar;
    }

    private static String ensureJava(Consumer<String> log) throws Exception {
        Path javaExec = findJavaExecutable(JRE_DIR);
        if (javaExec != null && Files.exists(javaExec)) {
            log.accept("Java " + JAVA_FEATURE_VERSION + " already installed, skipping download.");
            return javaExec.toAbsolutePath().toString();
        }

        log.accept("Java " + JAVA_FEATURE_VERSION + " not found, downloading runtime...");
        Files.createDirectories(JRE_DIR);

        String os = System.getProperty("os.name").toLowerCase();
        String osKey = os.contains("win") ? "windows" : os.contains("mac") ? "mac" : "linux";
        String archRaw = System.getProperty("os.arch").toLowerCase();
        String archKey = (archRaw.contains("aarch64") || archRaw.contains("arm64")) ? "aarch64" : "x64";
        String ext = osKey.equals("windows") ? "zip" : "tar.gz";

        String downloadUrl = "https://api.adoptium.net/v3/binary/latest/" + JAVA_FEATURE_VERSION
            + "/ga/" + osKey + "/" + archKey + "/jre/hotspot/normal/eclipse";

        Path archive = JRE_DIR.resolve("jre." + ext);
        log.accept("Downloading Java " + JAVA_FEATURE_VERSION + " runtime (" + osKey + "/" + archKey + ")...");
        downloadFile(downloadUrl, archive, log);

        log.accept("Extracting Java runtime...");
        if (ext.equals("zip")) {
            extractZip(archive, JRE_DIR);
        } else {
            extractTarGz(archive, JRE_DIR);
        }
        Files.deleteIfExists(archive);

        javaExec = findJavaExecutable(JRE_DIR);
        if (javaExec == null) {
            throw new Exception("Failed to locate java executable after extracting runtime.");
        }
        if (!osKey.equals("windows")) {
            javaExec.toFile().setExecutable(true);
        }

        log.accept("Java " + JAVA_FEATURE_VERSION + " ready.");
        return javaExec.toAbsolutePath().toString();
    }

    private static Path findJavaExecutable(Path root) throws IOException {
        if (!Files.exists(root)) return null;
        String exeName = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        try (var stream = Files.walk(root)) {
            return stream
                .filter(p -> p.getFileName() != null && p.getFileName().toString().equals(exeName))
                .filter(p -> p.toString().replace('\\', '/').contains("/bin/"))
                .findFirst()
                .orElse(null);
        }
    }

    private static void extractZip(Path archive, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = destDir.resolve(entry.getName()).normalize();
                if (!outPath.startsWith(destDir)) {
                    zis.closeEntry();
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    if (outPath.getParent() != null) Files.createDirectories(outPath.getParent());
                    try (OutputStream os = Files.newOutputStream(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        zis.transferTo(os);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static void extractTarGz(Path archive, Path destDir) throws IOException {
        try (GZIPInputStream gzis = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(archive)));
             BufferedInputStream bis = new BufferedInputStream(gzis)) {
            byte[] header = new byte[512];
            while (true) {
                int read = readFully(bis, header);
                if (read < 512 || isEmptyBlock(header)) break;

                String name = new String(header, 0, 100, StandardCharsets.US_ASCII).trim();
                String prefix = new String(header, 345, 155, StandardCharsets.US_ASCII).trim();
                if (!prefix.isEmpty()) name = prefix + "/" + name;
                name = name.replace("\0", "");

                String sizeField = new String(header, 124, 12, StandardCharsets.US_ASCII).replace("\0", "").trim();
                long size = sizeField.isEmpty() ? 0L : Long.parseLong(sizeField, 8);
                char typeFlag = (char) header[156];

                Path outPath = destDir.resolve(name).normalize();
                if (!outPath.startsWith(destDir)) {
                    skipFully(bis, size + padding(size));
                    continue;
                }

                if (typeFlag == '5') {
                    Files.createDirectories(outPath);
                } else if (typeFlag == '0' || typeFlag == '\0') {
                    if (outPath.getParent() != null) Files.createDirectories(outPath.getParent());
                    try (OutputStream os = Files.newOutputStream(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        long remaining = size;
                        byte[] buf = new byte[8192];
                        while (remaining > 0) {
                            int toRead = (int) Math.min(buf.length, remaining);
                            int r = bis.read(buf, 0, toRead);
                            if (r < 0) break;
                            os.write(buf, 0, r);
                            remaining -= r;
                        }
                    }
                } else {
                    skipFully(bis, size);
                }

                skipFully(bis, padding(size));
            }
        }
    }

    private static long padding(long size) {
        return (512 - (size % 512)) % 512;
    }

    private static int readFully(InputStream in, byte[] buf) throws IOException {
        int total = 0;
        while (total < buf.length) {
            int r = in.read(buf, total, buf.length - total);
            if (r < 0) break;
            total += r;
        }
        return total;
    }

    private static void skipFully(InputStream in, long n) throws IOException {
        long remaining = n;
        byte[] buf = new byte[8192];
        while (remaining > 0) {
            int toRead = (int) Math.min(buf.length, remaining);
            int r = in.read(buf, 0, toRead);
            if (r < 0) break;
            remaining -= r;
        }
    }

    private static boolean isEmptyBlock(byte[] block) {
        for (byte b : block) if (b != 0) return false;
        return true;
    }

    private static void startProcess(String mcVersion, AccountManager.Account account,
        SettingsManager settings, Path clientJar, Path fabricJar,
        List<Path> libs, JsonObject versionJson, String javaExec, Consumer<String> log) throws Exception {

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
            cmd.add(javaExec);
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

            log.accept("Starting Minecraft process...");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            log.accept("Minecraft launched!");

            // Stream Minecraft output to log window
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.accept(line);
                    }
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

    private static void downloadFile(String urlStr, Path dest, Consumer<String> log) throws Exception {
        log.accept("Downloading: " + dest.getFileName());
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
