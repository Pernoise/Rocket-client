package com.rogueclient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LaunchLogWindow {

    private final Stage stage;
    private final TextArea logArea;

    private final java.util.Set<String> shownTips = new java.util.HashSet<>();
    private String loadingVersion = null;
    private VBox tipsBox;

    public LaunchLogWindow() {
        stage = new Stage();

        VBox root = new VBox(12);
        root.setStyle("-fx-background-color: #080404;");
        root.setPadding(new Insets(20));

        tipsBox = new VBox(6);

        logArea = new TextArea();
        logArea.getStyleClass().add("rogue-scroll");
        logArea.setStyle(
            "-fx-background-color: #080404; -fx-text-fill: #ffffff; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 11; " +
            "-fx-border-color: #1a1a1a; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-control-inner-background: #080404;"
        );
        logArea.setEditable(false);
        logArea.setWrapText(true);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        Button copyBtn = new Button("Copy to Clipboard");
        copyBtn.setStyle(
            "-fx-background-color: #141414; -fx-text-fill: #ffffff; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
            "-fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-padding: 8 16;"
        );
        copyBtn.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(logArea.getText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            copyBtn.setText("Copied!");
        });

        Button uploadBtn = new Button("Upload Log");
        uploadBtn.setStyle(
            "-fx-background-color: #141414; -fx-text-fill: #ffffff; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
            "-fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-padding: 8 16;"
        );
        uploadBtn.setOnAction(e -> {
            String content = logArea.getText();
            if (content == null || content.isBlank()) {
                uploadBtn.setText("Log is empty");
                return;
            }
            uploadBtn.setDisable(true);
            uploadBtn.setText("Uploading...");
            new Thread(() -> {
                try {
                    String url = uploadToMclogs(content);
                    Platform.runLater(() -> {
                        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                        cc.putString(url);
                        javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
                        uploadBtn.setText("Link copied!");
                        uploadBtn.setDisable(false);
                        try {
                            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                        } catch (Exception ignored) {
                            // no default browser available in this environment - the link is
                            // already on the clipboard above, so the user can still get to it.
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        uploadBtn.setText("Upload failed");
                        uploadBtn.setDisable(false);
                    });
                }
            }).start();
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle(
            "-fx-background-color: #141414; -fx-text-fill: #ffffff; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
            "-fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-padding: 8 16;"
        );
        closeBtn.setOnAction(e -> stage.close());

        HBox btnRow = new HBox(8, copyBtn, uploadBtn, closeBtn);

        root.getChildren().addAll(tipsBox, logArea, btnRow);

        RogueWindowChrome.apply(stage, "LAUNCH LOG", root, 640, 420, null);
    }

    /** Uploads log text to mclo.gs (the standard modded-Minecraft log paste service) and returns the share URL. */
    private static String uploadToMclogs(String content) throws Exception {
        String body = "content=" + java.net.URLEncoder.encode(content, "UTF-8");
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
            new java.net.URL("https://api.mclo.gs/1/log").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(20000);
        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
        }

        String response;
        try (java.io.InputStream in = conn.getInputStream()) {
            response = new String(in.readAllBytes(), "UTF-8");
        }

        com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(response).getAsJsonObject();
        if (!json.has("success") || !json.get("success").getAsBoolean()) {
            String errMsg = json.has("error") ? json.get("error").getAsString() : "unknown error";
            throw new RuntimeException("mclo.gs rejected the upload: " + errMsg);
        }
        return json.get("url").getAsString();
    }

    public void show() {
        Platform.runLater(() -> stage.show());
    }

    public void appendLog(String line) {
        Platform.runLater(() -> {
            logArea.appendText(line + "\n");
            checkForKnownIssues(line);
        });
    }

    /**
     * Scans incoming log lines for a handful of well-known failure patterns and surfaces
     * a plain-language tip instead of leaving the person to decode a raw stack trace.
     * Each tip only shows once per launch even if the pattern repeats across lines.
     */
    private void checkForKnownIssues(String line) {
        java.util.regex.Matcher versionMatch =
            java.util.regex.Pattern.compile("Loading Minecraft (\\S+) with").matcher(line);
        if (versionMatch.find()) {
            loadingVersion = versionMatch.group(1);
        }

        if ((line.contains("does not appear to support OpenGL") || line.contains("GLFW error"))
            && shownTips.add("gpu-driver")) {
            addTip(
                "Graphics driver couldn't start OpenGL",
                "This is a GPU driver issue, not a launcher bug - usually happens on laptops with " +
                "both an integrated and a dedicated GPU when Java runs on the wrong one. Try: update " +
                "your GPU drivers, or in Windows go to Settings > Display > Graphics, add javaw.exe " +
                "(inside the installed Java folder) and set it to \"High performance\"."
            );
        }

        java.util.regex.Matcher optifineMatch =
            java.util.regex.Pattern.compile("OptiFine_(\\d+\\.\\d+(?:\\.\\d+)?)_").matcher(line);
        if (optifineMatch.find() && loadingVersion != null) {
            String optifineVersion = optifineMatch.group(1);
            if (!loadingVersion.startsWith(optifineVersion) && shownTips.add("optifine-mismatch")) {
                addTip(
                    "OptiFine version doesn't match this instance",
                    "This OptiFine build is for " + optifineVersion + ", but this instance is running " +
                    loadingVersion + ". Mismatched OptiFine builds commonly cause crashes or rendering " +
                    "issues - remove it from the mods folder and use a build made for " + loadingVersion + "."
                );
            }
        }
    }

    private void addTip(String title, String body) {
        Label titleLabel = new Label("\u26A0 " + title);
        titleLabel.setStyle("-fx-text-fill: #e0a030; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-font-weight: bold;");

        Label bodyLabel = new Label(body);
        bodyLabel.setWrapText(true);
        bodyLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 11;");

        VBox card = new VBox(4, titleLabel, bodyLabel);
        card.setPadding(new Insets(10));
        card.setStyle(
            "-fx-background-color: #1a1508; -fx-border-color: #3a2c10; " +
            "-fx-border-radius: 6; -fx-background-radius: 6;"
        );
        tipsBox.getChildren().add(card);
    }

    public void setTitle(String title) {
        Platform.runLater(() -> stage.setTitle(title));
    }
}
