package com.rogueclient;

import java.util.Properties;

/**
 * The one place the app's current version lives. Backed by version.properties,
 * generated at build time from build.gradle's `version` property (see the
 * generateVersionResource task).
 */
public class AppVersion {

    public static final String CURRENT = load();

    private static String load() {
        try (var in = AppVersion.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                String v = props.getProperty("version");
                if (v != null && !v.isBlank()) return v.trim();
            }
        } catch (Exception ignored) {
            // falls through to the dev-run fallback below
        }
        return "dev";
    }
}
