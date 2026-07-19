package com.rocketclient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.function.Consumer;
import java.util.zip.*;

public class JavaManager {

    private static final Path JAVA_DIR = Paths.get(System.getProperty("user.home"), ".rocketclient", "java");
    private static final Gson GSON = new Gson();
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");

    public static String getJavaForVersion(String mcVersion, Consumer<String> log) throws Exception {
        int javaVersion = getRequiredJava(mcVersion);
        return ensureJava(javaVersion, log);
    }

    private static int getRequiredJava(String mcVersion) {
        if (mcVersion.startsWith("26.") || mcVersion.startsWith("25.")) return 21;
        String[] parts = mcVersion.split("\\.");
        if (parts.length < 2) return 8;
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            if (major == 1) {
                if (minor <= 8) return 8;
                if (minor <= 16) return 11;
                if (minor == 17) return 16;
                if (minor <= 20) return 17;
                return 21;
            }
        } catch (NumberFormatException ignored) {}
        return 8;
    }

    private static String ensureJava(int version, Consumer<String> log) throws Exception {
        Path javaHome = JAVA_DIR.resolve("jdk-" + version);
        String execName = IS_WINDOWS ? "java.exe" : "java";
        Path javaExec = javaHome.resolve("bin").resolve(execName);

        if (Files.exists(javaExec)) {
            log.accept("Java " + version + " already installed.");
            return javaExec.toAbsolutePath().toString();
        }

        log.accept("Downloading Java " + version + "...");
        Files.createDirectories(JAVA_DIR);

        String os = IS_WINDOWS ? "windows" : IS_MAC ? "mac" : "linux";
        String arch = System.getProperty("os.arch").contains("aarch64") ? "aarch64" : "x64";
        String ext  = IS_WINDOWS ? "zip" : "tar.gz";

        String apiUrl = "https://api.adoptium.net/v3/assets/latest/" + version +
            "/hotspot?architecture=" + arch + "&image_type=jdk&os=" + os + "&vendor=eclipse";

        String response = fetch(apiUrl);
        JsonArray releases = GSON.fromJson(response, JsonArray.class);
        if (releases.size() == 0) throw new Exception("No Java " + version + " release found");

        JsonObject release = releases.get(0).getAsJsonObject();
        JsonObject binary  = release.getAsJsonObject("binary");
        JsonObject pkg     = binary.getAsJsonObject("package");
        String downloadUrl = pkg.get("link").getAsString();

        Path archive = JAVA_DIR.resolve("jdk-" + version + "." + ext);
        downloadFile(downloadUrl, archive, log);

        log.accept("Extracting Java " + version + "...");
        if (IS_WINDOWS) {
            extractZip(archive, JAVA_DIR, javaHome, log);
        } else {
            extractTarGz(archive, JAVA_DIR, javaHome, log);
        }

        Files.deleteIfExists(archive);

        if (!Files.exists(javaExec)) {
            throw new Exception("Java installation failed ? executable not found at " + javaExec);
        }

        log.accept("Java " + version + " installed successfully.");
        return javaExec.toAbsolutePath().toString();
    }

    private static void extractZip(Path archive, Path destDir, Path javaHome, Consumer<String> log) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                int slash = name.indexOf('/');
                if (slash < 0) { zis.closeEntry(); continue; }
                String relative = name.substring(slash + 1);
                if (relative.isEmpty()) { zis.closeEntry(); continue; }
                Path dest = javaHome.resolve(relative);
                if (entry.isDirectory()) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private static void extractTarGz(Path archive, Path destDir, Path javaHome, Consumer<String> log) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("tar", "-xzf", archive.toAbsolutePath().toString(),
            "-C", destDir.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.waitFor();
        try (var stream = Files.list(destDir)) {
            stream.filter(Files::isDirectory)
                .filter(p2 -> p2.getFileName().toString().startsWith("jdk"))
                .findFirst()
                .ifPresent(extracted -> {
                    try { if (!extracted.equals(javaHome)) Files.move(extracted, javaHome); }
                    catch (Exception ignored) {}
                });
        }
    }

    private static String fetch(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "RocketClient/0.1");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        return new String(conn.getInputStream().readAllBytes());
    }

    private static void downloadFile(String urlStr, Path dest, Consumer<String> log) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "RocketClient/0.1");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(120000);
        conn.setInstanceFollowRedirects(true);
        long total = conn.getContentLengthLong();
        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(dest)) {
            byte[] buf = new byte[8192];
            long downloaded = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                downloaded += n;
                if (total > 0) {
                    int pct = (int)(downloaded * 100 / total);
                    if (pct % 10 == 0) log.accept("Downloading Java: " + pct + "%");
                }
            }
        }
    }
}
