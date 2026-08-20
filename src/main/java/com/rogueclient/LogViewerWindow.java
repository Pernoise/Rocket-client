package com.rogueclient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class LogViewerWindow {

    private static final Path LAUNCHER_LOG = Paths.get(System.getProperty("user.home"), ".rogueclient", "logs", "launcher-latest.log");
    private static final Path MC_LOG       = Paths.get(System.getProperty("user.home"), ".rogueclient", "minecraft", "logs", "minecraft-latest.log");
    private static final Path FORGE_LOG    = Paths.get(System.getProperty("user.home"), ".rogueclient", "minecraft", "logs", "forge-latest.log");
    private static final Path LOGS_DIR     = Paths.get(System.getProperty("user.home"), ".rogueclient", "logs");

    public static void open() {
        Stage stage = new Stage();

        VBox logLines = new VBox(1);
        logLines.setPadding(new Insets(10));

        ScrollPane logScroll = new ScrollPane(logLines);
        logScroll.getStyleClass().add("rogue-scroll");
        logScroll.setFitToWidth(true);
        logScroll.setStyle(
            "-fx-background: #080404; -fx-background-color: #080404; " +
            "-fx-border-color: #1a1a1a; -fx-border-radius: 6; -fx-background-radius: 6;"
        );
        VBox.setVgrow(logScroll, Priority.ALWAYS);

        Button launcherTab = new Button("Launcher Log");
        Button mcTab = new Button("Minecraft Log");

        Path[] currentPath = { LAUNCHER_LOG };

        Button copyBtn = new Button("Copy Log");
        copyBtn.setStyle(secondaryBtnStyle());
        copyBtn.setOnAction(e -> {
            StringBuilder sb = new StringBuilder();
            for (javafx.scene.Node node : logLines.getChildren()) {
                if (node instanceof Label label) {
                    sb.append(label.getText()).append("\n");
                }
            }
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(sb.toString());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            copyBtn.setText("Copied!");
        });

        Runnable reload = () -> {
            loadLog(logLines, currentPath[0]);
            copyBtn.setText("Copy Log");
            Platform.runLater(() -> logScroll.setVvalue(1.0));
        };

        launcherTab.setOnAction(e -> {
            setActiveTab(launcherTab, mcTab);
            currentPath[0] = LAUNCHER_LOG;
            reload.run();
        });
        mcTab.setOnAction(e -> {
            setActiveTab(mcTab, launcherTab);
            currentPath[0] = MinecraftLauncher.lastLogsDir != null
                ? MinecraftLauncher.lastLogsDir.resolve("minecraft-latest.log")
                : (Files.exists(MC_LOG) ? MC_LOG : FORGE_LOG);
            reload.run();
        });

        HBox tabRow = new HBox(6, launcherTab, mcTab);

        Button clearBtn = new Button("Clear Log");
        clearBtn.setStyle(secondaryBtnStyle());
        clearBtn.setOnAction(e -> {
            try {
                Files.write(currentPath[0], new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception ex) {
                System.out.println("Could not clear log: " + ex.getMessage());
            }
            reload.run();
        });

        Button openFolderBtn = new Button("Open Logs Folder");
        openFolderBtn.setStyle(secondaryBtnStyle());
        openFolderBtn.setOnAction(e -> openFolder(LOGS_DIR));

        Button closeBtn = new Button("Close");
        closeBtn.setStyle(secondaryBtnStyle());
        closeBtn.setOnAction(e -> stage.close());

        HBox spacerRow = new HBox();
        HBox.setHgrow(spacerRow, Priority.ALWAYS);

        HBox btnRow = new HBox(8, clearBtn, copyBtn, spacerRow, openFolderBtn, closeBtn);

        VBox root = new VBox(12, tabRow, logScroll, btnRow);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #080404;");

        RogueWindowChrome.apply(stage, "LOGS", root, 860, 620, null);

        setActiveTab(launcherTab, mcTab);
        reload.run();

        stage.centerOnScreen();
        stage.show();
    }

    private static void loadLog(VBox container, Path path) {
        container.getChildren().clear();

        if (!Files.exists(path)) {
            container.getChildren().add(logLine("No log file found yet at: " + path, "#888888"));
            return;
        }

        try {
            for (String line : Files.readAllLines(path)) {
                container.getChildren().add(logLine(line, colorFor(line)));
            }
        } catch (Exception e) {
            container.getChildren().add(logLine("Could not read log: " + e.getMessage(), "#ff4444"));
        }
    }

    private static Label logLine(String text, String color) {
        Label label = new Label(text.isEmpty() ? " " : text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 11;");
        return label;
    }

    /**
     * Colors lines by what they likely are, so real errors jump out instead of
     * getting lost in a wall of identical white monospace text:
     *  - actual errors/exceptions: red
     *  - warnings: orange
     *  - "Caused by" chains: bright red, since that's usually the real cause
     *  - indented stack trace frames ("at ..."): dimmed gray, they're noise 95% of the time
     *  - everything else: normal white
     */
    private static String colorFor(String line) {
        String lower = line.toLowerCase().trim();
        if (lower.startsWith("caused by")) return "#ff6666";
        if (lower.contains("exception") || lower.contains("error") || lower.contains("crashed") || lower.contains("failed")) return "#ff4444";
        if (lower.contains("warn")) return "#e0a030";
        if (lower.trim().startsWith("at ") || lower.trim().startsWith("...")) return "#666666";
        return "#ffffff";
    }

    private static void setActiveTab(Button active, Button inactive) {
        active.setStyle(tabBtnActiveStyle());
        inactive.setStyle(tabBtnStyle());
    }

    private static void openFolder(Path path) {
        try {
            if (!Files.exists(path)) Files.createDirectories(path);
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("explorer", path.toString());
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", path.toString());
            } else {
                pb = new ProcessBuilder("xdg-open", path.toString());
            }
            pb.start();
        } catch (Exception e) {
            System.out.println("Could not open logs folder: " + e.getMessage());
        }
    }

    private static String tabBtnStyle() {
        return "-fx-background-color: #0f0f0f; -fx-text-fill: #888888; -fx-font-family: 'JetBrains Mono'; " +
            "-fx-font-size: 12; -fx-border-color: #1a1a1a; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-padding: 8 16;";
    }

    private static String tabBtnActiveStyle() {
        return "-fx-background-color: #161616; -fx-text-fill: #ffffff; -fx-font-family: 'JetBrains Mono'; " +
            "-fx-font-size: 12; -fx-border-color: #1a1a1a; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-padding: 8 16;";
    }

    private static String secondaryBtnStyle() {
        return "-fx-background-color: #141414; -fx-text-fill: #ffffff; -fx-font-family: 'JetBrains Mono'; " +
            "-fx-font-size: 12; -fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-padding: 8 16;";
    }
}
