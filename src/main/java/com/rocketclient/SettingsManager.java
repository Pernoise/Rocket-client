package com.rocketclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.*;

public class SettingsManager {

    private static final Path DATA_DIR      = Paths.get(System.getProperty("user.home"), ".rocketclient");
    private static final Path SETTINGS_FILE = DATA_DIR.resolve("settings.json");
    private static final Gson GSON          = new GsonBuilder().setPrettyPrinting().create();

    public String  javaPath      = "java";
    public String  javaArgs      = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200";
    public int     ramMb         = 2048;
    public boolean hideLauncher  = true;
    public boolean closeLauncher = false;
    public boolean discordRpc    = true;
    public boolean autoUpdate    = true;

    public SettingsManager() {}

    public void load() {
        try {
            if (Files.exists(SETTINGS_FILE)) {
                String json = new String(Files.readAllBytes(SETTINGS_FILE));
                JsonObject obj = GSON.fromJson(json, JsonObject.class);
                if (obj.has("javaPath"))      this.javaPath      = obj.get("javaPath").getAsString();
                if (obj.has("javaArgs"))      this.javaArgs      = obj.get("javaArgs").getAsString();
                if (obj.has("ramMb"))         this.ramMb         = obj.get("ramMb").getAsInt();
                if (obj.has("hideLauncher"))  this.hideLauncher  = obj.get("hideLauncher").getAsBoolean();
                if (obj.has("closeLauncher")) this.closeLauncher = obj.get("closeLauncher").getAsBoolean();
                if (obj.has("discordRpc"))    this.discordRpc    = obj.get("discordRpc").getAsBoolean();
                if (obj.has("autoUpdate"))    this.autoUpdate    = obj.get("autoUpdate").getAsBoolean();
            }
        } catch (Exception e) {
            System.out.println("Could not load settings: " + e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(DATA_DIR);
            JsonObject obj = new JsonObject();
            obj.addProperty("javaPath",      javaPath);
            obj.addProperty("javaArgs",      javaArgs);
            obj.addProperty("ramMb",         ramMb);
            obj.addProperty("hideLauncher",  hideLauncher);
            obj.addProperty("closeLauncher", closeLauncher);
            obj.addProperty("discordRpc",    discordRpc);
            obj.addProperty("autoUpdate",    autoUpdate);
            Files.write(SETTINGS_FILE, GSON.toJson(obj).getBytes());
        } catch (Exception e) {
            System.out.println("Could not save settings: " + e.getMessage());
        }
    }

    public static int getSystemMaxRamMb() {
        try {
            com.sun.management.OperatingSystemMXBean os =
                (com.sun.management.OperatingSystemMXBean)
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            return (int)(os.getTotalMemorySize() / 1024 / 1024);
        } catch (Exception e) {
            return (int)(Runtime.getRuntime().totalMemory() / 1024 / 1024);
        }
    }
}
