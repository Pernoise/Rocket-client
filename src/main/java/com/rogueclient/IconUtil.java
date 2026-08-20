package com.rogueclient;

import javafx.scene.image.Image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Loads instance icons that can be either a bundled classpath resource
 * (e.g. "icons/anvil.png") or a custom image the user picked from disk
 * (stored as an absolute path once imported). Custom icons get copied into
 * ~/.rogueclient/instance-icons/ on import so they survive even if the
 * user moves or deletes the original file they picked.
 */
public class IconUtil {

    private static final Path CUSTOM_ICONS_DIR = Paths.get(System.getProperty("user.home"), ".rogueclient", "instance-icons");

    public static Image load(String icon) {
        try {
            if (isCustom(icon)) {
                return new Image("file:" + icon);
            } else {
                InputStream in = IconUtil.class.getClassLoader().getResourceAsStream(icon);
                return in != null ? new Image(in) : null;
            }
        } catch (Exception e) {
            System.out.println("Could not load icon " + icon + ": " + e.getMessage());
            return null;
        }
    }

    public static boolean isCustom(String icon) {
        return icon != null && !icon.startsWith("icons/");
    }

    /** Copies a user-picked image file into the app's own icon cache and returns the new absolute path. */
    public static String importCustomIcon(Path sourceFile) throws IOException {
        Files.createDirectories(CUSTOM_ICONS_DIR);
        String ext = "";
        String name = sourceFile.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) ext = name.substring(dot);

        Path dest = CUSTOM_ICONS_DIR.resolve(UUID.randomUUID() + ext);
        Files.copy(sourceFile, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toAbsolutePath().toString();
    }
}
