package com.rocketclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages instances: isolated per-profile folders for mods/saves/config,
 * while shared data (vanilla jars, libraries, assets, Java runtimes) stays
 * in the existing common cache under ~/.rocketclient/minecraft and
 * ~/.rocketclient/java. Each instance gets its own subfolder under
 * ~/.rocketclient/instances/<id>/ with an instance.json describing it.
 */
public class InstanceManager {

    private static final Path INSTANCES_DIR = Paths.get(System.getProperty("user.home"), ".rocketclient", "instances");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Built-in icon set for instances, reusing icons already bundled with the app. */
    public static final String[] BUILT_IN_ICONS = {
        "icons/rocket-launch.png",
        "icons/anvil.png",
        "icons/fabric.png",
        "icons/cat.png",
        "icons/atom.png",
        "icons/biohazard.png"
    };

    public static class Instance {
        public String id;
        public String name;
        public String mcVersion;
        public String loader;      // "fabric" or "forge"
        public String icon;        // one of BUILT_IN_ICONS
        public long lastPlayed;    // epoch millis, 0 = never played

        public Instance() {}

        public Instance(String id, String name, String mcVersion, String loader, String icon) {
            this.id = id;
            this.name = name;
            this.mcVersion = mcVersion;
            this.loader = loader;
            this.icon = icon;
            this.lastPlayed = 0;
        }

        public Path dir() { return INSTANCES_DIR.resolve(id); }
        public Path modsDir() { return dir().resolve("mods"); }
        public Path savesDir() { return dir().resolve("saves"); }
        public Path resourcePacksDir() { return dir().resolve("resourcepacks"); }
        public Path shaderPacksDir() { return dir().resolve("shaderpacks"); }
        public Path configDir() { return dir().resolve("config"); }
        public Path logsDir() { return dir().resolve("logs"); }
        public Path crashReportsDir() { return dir().resolve("crash-reports"); }
    }

    public List<Instance> list() {
        List<Instance> result = new ArrayList<>();
        try {
            if (!Files.exists(INSTANCES_DIR)) return result;
            try (var stream = Files.list(INSTANCES_DIR)) {
                for (Path dir : stream.filter(Files::isDirectory).collect(Collectors.toList())) {
                    Path infoFile = dir.resolve("instance.json");
                    if (Files.exists(infoFile)) {
                        try {
                            String json = new String(Files.readAllBytes(infoFile));
                            Instance inst = GSON.fromJson(json, Instance.class);
                            if (inst != null) result.add(inst);
                        } catch (Exception e) {
                            System.out.println("Could not load instance at " + dir + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Could not list instances: " + e.getMessage());
        }
        return result;
    }

    public List<Instance> recent(int limit) {
        return list().stream()
            .filter(i -> i.lastPlayed > 0)
            .sorted(Comparator.comparingLong((Instance i) -> i.lastPlayed).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    public Instance create(String name, String mcVersion, String loader, String icon) {
        String id = uniqueId(name);
        Instance inst = new Instance(id, uniqueName(name), mcVersion, loader, icon);
        try {
            Files.createDirectories(inst.modsDir());
            Files.createDirectories(inst.savesDir());
            Files.createDirectories(inst.resourcePacksDir());
            Files.createDirectories(inst.shaderPacksDir());
            Files.createDirectories(inst.configDir());
            Files.createDirectories(inst.logsDir());
            Files.createDirectories(inst.crashReportsDir());
            save(inst);
        } catch (IOException e) {
            System.out.println("Could not create instance: " + e.getMessage());
        }
        return inst;
    }

    public void save(Instance inst) {
        try {
            Files.createDirectories(inst.dir());
            Files.write(inst.dir().resolve("instance.json"), GSON.toJson(inst).getBytes());
        } catch (IOException e) {
            System.out.println("Could not save instance: " + e.getMessage());
        }
    }

    public void markPlayed(Instance inst) {
        inst.lastPlayed = System.currentTimeMillis();
        save(inst);
    }

    public void rename(Instance inst, String newName) {
        inst.name = newName;
        save(inst);
    }

    public void delete(Instance inst) {
        try {
            if (Files.exists(inst.dir())) {
                Files.walk(inst.dir())
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
            }
        } catch (IOException e) {
            System.out.println("Could not delete instance: " + e.getMessage());
        }
    }

    private String uniqueName(String base) {
        List<String> existingNames = list().stream().map(i -> i.name).collect(Collectors.toList());
        if (!existingNames.contains(base)) return base;
        int n = 2;
        while (existingNames.contains(base + " (" + n + ")")) n++;
        return base + " (" + n + ")";
    }

    private String uniqueId(String base) {
        String slug = base.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (slug.isEmpty()) slug = "instance";
        String id = slug;
        int n = 2;
        while (Files.exists(INSTANCES_DIR.resolve(id))) {
            id = slug + "-" + n;
            n++;
        }
        return id;
    }
}
