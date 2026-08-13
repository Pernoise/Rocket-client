package com.rocketclient;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Ships a per-version "default modpack" (mods + config) inside the launcher's own
 * resources at /defaultmodpack/{mcVersion}/mods and /defaultmodpack/{mcVersion}/config,
 * and copies it into the game directory before every launch of that version. Overwrites
 * on every run by design - "always launches with these mods and this config" means the
 * bundled copy is the source of truth, not a one-time seed the player can drift away from.
 *
 * To add a pack: drop files into
 *   src/main/resources/defaultmodpack/<mcVersion>/mods/*.jar
 *   src/main/resources/defaultmodpack/<mcVersion>/config/...
 * and rebuild - there's no separate registration step, presence of the folder is enough.
 */
public class DefaultModpackManager {

    private static final String RESOURCE_ROOT = "defaultmodpack";

    /** No-ops silently if this version has no bundled pack, so it's safe to call for every launch. */
    public static void installFor(String mcVersion, Path gameDir, Consumer<String> log) {
        try {
            String prefix = RESOURCE_ROOT + "/" + mcVersion + "/";
            java.net.URL root = DefaultModpackManager.class.getClassLoader().getResource(prefix);
            if (root == null) return; // no default pack bundled for this version

            log.accept("Syncing default modpack for " + mcVersion + "...");

            if ("jar".equals(root.getProtocol())) {
                installFromJar(prefix, gameDir, log);
            } else {
                installFromDirectory(root, gameDir, log);
            }

            log.accept("Default modpack synced.");
        } catch (Exception e) {
            // A broken/missing pack shouldn't block launching the game entirely.
            log.accept("Could not sync default modpack: " + e.getMessage());
        }
    }

    private static void installFromJar(String prefix, Path gameDir, Consumer<String> log) throws Exception {
        URI jarUri = DefaultModpackManager.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        Path jarPath = Paths.get(jarUri);

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith(prefix)) continue;

                String relative = entry.getName().substring(prefix.length()); // e.g. "mods/foo.jar"
                Path dest = resolveDest(gameDir, relative);
                if (dest == null) continue;

                Files.createDirectories(dest.getParent());
                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }
                log.accept("  -> " + relative);
            }
        }
    }

    private static void installFromDirectory(java.net.URL root, Path gameDir, Consumer<String> log) throws Exception {
        Path rootPath = Paths.get(root.toURI());
        try (var walk = Files.walk(rootPath)) {
            for (Path source : (Iterable<Path>) walk::iterator) {
                if (Files.isDirectory(source)) continue;
                String relative = rootPath.relativize(source).toString().replace('\\', '/');
                Path dest = resolveDest(gameDir, relative);
                if (dest == null) continue;

                Files.createDirectories(dest.getParent());
                Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                log.accept("  -> " + relative);
            }
        }
    }

    /** Only "mods/" and "config/" are recognized top-level folders inside a pack; anything else is skipped. */
    private static Path resolveDest(Path gameDir, String relative) {
        if (relative.startsWith("mods/")) return gameDir.resolve(relative);
        if (relative.startsWith("config/")) return gameDir.resolve(relative);
        return null;
    }
}
