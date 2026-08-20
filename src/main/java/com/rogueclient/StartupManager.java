package com.rogueclient;

import java.nio.file.Paths;

/**
 * Windows-only "launch at startup" toggle using the per-user Run registry key
 * (HKCU\Software\Microsoft\Windows\CurrentVersion\Run) - no admin rights needed,
 * and it's removed automatically if the user uninstalls without unchecking it first
 * since it just stops resolving to anything rather than erroring.
 */
public class StartupManager {

    private static final String REG_KEY  = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String REG_NAME = "RogueClient";

    public static boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static void setEnabled(boolean enabled) {
        if (!isSupported()) return;
        try {
            if (enabled) {
                String command = resolveLaunchCommand();
                new ProcessBuilder("reg", "add", REG_KEY, "/v", REG_NAME, "/t", "REG_SZ", "/d", command, "/f")
                    .redirectErrorStream(true).start().waitFor();
            } else {
                new ProcessBuilder("reg", "delete", REG_KEY, "/v", REG_NAME, "/f")
                    .redirectErrorStream(true).start().waitFor();
            }
        } catch (Exception e) {
            System.out.println("Could not update startup registry entry: " + e.getMessage());
        }
    }

    /**
     * Reconstructs the exact command this JVM was started with when available (works whether
     * this is running via a native exe wrapper or "javaw -jar ..."), falling back to rebuilding
     * a "javaw -jar <this jar>" command from the running jar's own location if the JVM won't
     * report its command line (restricted on some setups).
     */
    private static String resolveLaunchCommand() {
        try {
            var commandLine = ProcessHandle.current().info().commandLine();
            if (commandLine.isPresent() && !commandLine.get().isBlank()) {
                return commandLine.get();
            }
        } catch (Exception ignored) {
            // fall through to manual reconstruction below
        }

        try {
            String javaw = Paths.get(System.getProperty("java.home"), "bin", "javaw.exe").toString();
            String jarPath = Paths.get(
                StartupManager.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            ).toString();
            return "\"" + javaw + "\" -jar \"" + jarPath + "\"";
        } catch (Exception e) {
            return "\"" + Paths.get(System.getProperty("java.home"), "bin", "javaw.exe") + "\"";
        }
    }
}
