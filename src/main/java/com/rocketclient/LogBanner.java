package com.rocketclient;

/**
 * Prints the same kind of clear, branded startup block other launchers open their log
 * with - ASCII logo, host specs, active config - instead of the log just starting mid-
 * sentence with whatever the first library happens to print. Purely cosmetic/diagnostic;
 * doesn't affect anything functional.
 */
public class LogBanner {

    private static final String[] LOGO = {
        " ____             _        _   ",
        "|  _ \\ ___   ___ | | _____| |_ ",
        "| |_) / _ \\ / _ \\| |/ / _ \\ __|",
        "|  _ < (_) | (_) |   <  __/ |_ ",
        "|_| \\_\\___/ \\___/|_|\\_\\___/\\__|"
    };

    public static void print(SettingsManager settings) {
        System.out.println();
        for (String line : LOGO) System.out.println(line);
        System.out.println();
        System.out.println("        Rocket Client \u2014 a lightweight Minecraft launcher");
        System.out.println("                   built by Pernoise");
        System.out.println();

        System.out.println("[INFO] Host system details:");
        System.out.println("[INFO]  \u2022 OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("[INFO]  \u2022 Arch: " + System.getProperty("os.arch"));
        System.out.println("[INFO]  \u2022 CPU cores: " + Runtime.getRuntime().availableProcessors() + cpuNameSuffix());
        System.out.println("[INFO]  \u2022 RAM: " + SettingsManager.getSystemMaxRamMb() + " MB");
        System.out.println("[INFO]  \u2022 Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        System.out.println();

        System.out.println("[INFO] Loaded settings successfully.");
        System.out.println("[INFO] Printing config for troubleshooting purposes:");
        System.out.println("[INFO] " + configJson(settings));
        System.out.println();
    }

    /**
     * Java has no built-in CPU model name (only core count) - PROCESSOR_IDENTIFIER is a
     * Windows-only env var set by the OS itself, so this is a no-cost way to get a real
     * model string there without pulling in a native-info library like OSHI just for this.
     */
    private static String cpuNameSuffix() {
        String id = System.getenv("PROCESSOR_IDENTIFIER");
        return (id != null && !id.isBlank()) ? " (" + id + ")" : "";
    }

    private static String configJson(SettingsManager s) {
        return "{"
            + "\"javaPath\":\"" + esc(s.javaPath) + "\","
            + "\"javaArgs\":\"" + esc(s.javaArgs) + "\","
            + "\"ramMb\":" + s.ramMb + ","
            + "\"hideLauncher\":" + s.hideLauncher + ","
            + "\"closeLauncher\":" + s.closeLauncher + ","
            + "\"discordRpc\":" + s.discordRpc + ","
            + "\"enableTray\":" + s.enableTray + ","
            + "\"launchOnStartup\":" + s.launchOnStartup
            + "}";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
