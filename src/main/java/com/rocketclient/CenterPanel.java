package com.rocketclient;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;

public class CenterPanel extends VBox {

    private final AccountManager accountManager;
    private final SettingsManager settingsManager;
    private boolean fabricMode = true;
    private String currentVersion = "26.1.2";

    public CenterPanel(AccountManager accountManager, SettingsManager settingsManager) {
        this.accountManager  = accountManager;
        this.settingsManager = settingsManager;

        setAlignment(Pos.CENTER);
        setPadding(new Insets(28, 36, 22, 36));
        setSpacing(6);

        Label name = new Label("Rocket Client");
        name.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 28; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-opacity: 0.88;");

        Label tagline = new Label("A Minecraft performance-focused client — BETA");
        tagline.setStyle("-fx-text-fill: #2e2e2e; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");

        Label quote = new Label(loadRandomQuote());
        quote.setStyle("-fx-text-fill: #383838; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-font-style: italic; -fx-font-weight: bold;");
        quote.setWrapText(true);
        quote.setMaxWidth(420);
        quote.setAlignment(Pos.CENTER);

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Loader icon
        ImageView loaderIcon = new ImageView();
        loaderIcon.setFitWidth(22);
        loaderIcon.setFitHeight(22);
        loaderIcon.setPreserveRatio(true);
        setLoaderIcon(loaderIcon, true);

        Button loaderBtn = new Button();
        loaderBtn.setGraphic(loaderIcon);
        loaderBtn.setStyle(
            "-fx-background-color: #0f0f0f; -fx-border-color: #1a1a1a; " +
            "-fx-border-width: 1 0 1 1; " +
            "-fx-border-radius: 8 0 0 8; -fx-background-radius: 8 0 0 8; " +
            "-fx-cursor: hand; -fx-padding: 14 10;"
        );
        loaderBtn.setTooltip(new javafx.scene.control.Tooltip("Fabric — click to switch to Forge 1.8.9"));

        // Play button
        Button playBtn = new Button("▶   Play  [" + currentVersion + "]");
        playBtn.setStyle(
            "-fx-background-color: #0f0f0f; -fx-text-fill: #ffffff; " +
            "-fx-font-size: 13; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono'; " +
            "-fx-border-color: #1a1a1a; -fx-border-width: 1 0 1 0; " +
            "-fx-background-radius: 0; -fx-border-radius: 0; " +
            "-fx-cursor: hand; -fx-padding: 16 24; -fx-opacity: 0.88;"
        );
        playBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(playBtn, Priority.ALWAYS);

        playBtn.setOnMouseEntered(e -> playBtn.setStyle(
            "-fx-background-color: #161616; -fx-text-fill: #ffffff; " +
            "-fx-font-size: 13; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono'; " +
            "-fx-border-color: #1a1a1a; -fx-border-width: 1 0 1 0; " +
            "-fx-background-radius: 0; -fx-border-radius: 0; " +
            "-fx-cursor: hand; -fx-padding: 16 24; -fx-opacity: 0.88;"
        ));
        playBtn.setOnMouseExited(e -> playBtn.setStyle(
            "-fx-background-color: #0f0f0f; -fx-text-fill: #ffffff; " +
            "-fx-font-size: 13; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono'; " +
            "-fx-border-color: #1a1a1a; -fx-border-width: 1 0 1 0; " +
            "-fx-background-radius: 0; -fx-border-radius: 0; " +
            "-fx-cursor: hand; -fx-padding: 16 24; -fx-opacity: 0.88;"
        ));

        // Version arrow
        Button versionBtn = new Button("▾");
        versionBtn.setStyle(
            "-fx-background-color: #0f0f0f; -fx-text-fill: #555555; " +
            "-fx-font-size: 13; -fx-font-weight: bold; " +
            "-fx-border-color: #1a1a1a; -fx-border-width: 1 1 1 0; " +
            "-fx-border-radius: 0 8 8 0; -fx-background-radius: 0 8 8 0; " +
            "-fx-cursor: hand; -fx-min-width: 40; -fx-padding: 16 10;"
        );

        HBox playRow = new HBox(0, loaderBtn, playBtn, versionBtn);
        playRow.setMaxWidth(Double.MAX_VALUE);

        // Version list
        VBox versionList = new VBox(2);
        versionList.setStyle("-fx-background-color: #0d0d0d;");
        versionList.setPadding(new Insets(4));

        String[] versions = {
            "26.1.2", "26.1.1", "26.1",
            "1.21.11", "1.21.10", "1.21.9", "1.21.8", "1.21.7",
            "1.21.6", "1.21.5", "1.21.4", "1.21.3", "1.21.2",
            "1.21.1", "1.21", "1.20.6", "1.20.5", "1.20.4",
            "1.20.3", "1.20.2", "1.20.1", "1.20", "1.19.4",
            "1.19.3", "1.19.2", "1.19.1", "1.19"
        };

        ScrollPane scrollPane = new ScrollPane(versionList);
        scrollPane.setMaxHeight(160);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #0d0d0d; -fx-background-color: #0d0d0d; -fx-border-color: #1a1a1a; -fx-border-radius: 7; -fx-background-radius: 7;");
        scrollPane.setVisible(false);
        scrollPane.setManaged(false);

        for (String v : versions) {
            Label vLabel = new Label(v);
            vLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-padding: 6 10;");
            vLabel.setMaxWidth(Double.MAX_VALUE);
            vLabel.setOnMouseEntered(e -> vLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-padding: 6 10; -fx-background-color: #161616; -fx-background-radius: 5;"));
            vLabel.setOnMouseExited(e  -> vLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-padding: 6 10;"));
            vLabel.setOnMouseClicked(e -> {
                currentVersion = v;
                playBtn.setText("▶   Play  [" + v + "]");
                scrollPane.setVisible(false);
                scrollPane.setManaged(false);
            });
            versionList.getChildren().add(vLabel);
        }

        versionBtn.setOnAction(e -> {
            boolean visible = scrollPane.isVisible();
            scrollPane.setVisible(!visible);
            scrollPane.setManaged(!visible);
        });

        loaderBtn.setOnAction(e -> {
            fabricMode = !fabricMode;
            setLoaderIcon(loaderIcon, fabricMode);
            if (fabricMode) {
                currentVersion = "26.1.2";
                playBtn.setText("▶   Play  [26.1.2]");
                versionBtn.setVisible(true);
                versionBtn.setManaged(true);
                loaderBtn.setTooltip(new javafx.scene.control.Tooltip("Fabric — click to switch to Forge 1.8.9"));
            } else {
                currentVersion = "1.8.9";
                playBtn.setText("▶   Play  [1.8.9 — Forge]");
                versionBtn.setVisible(false);
                versionBtn.setManaged(false);
                scrollPane.setVisible(false);
                scrollPane.setManaged(false);
                loaderBtn.setTooltip(new javafx.scene.control.Tooltip("Forge 1.8.9 — click to switch to Fabric"));
            }
        });

        playBtn.setOnAction(e -> handlePlay(playBtn));

        getChildren().addAll(name, tagline, quote, spacer, playRow, scrollPane);
    }

    private void setLoaderIcon(ImageView iv, boolean fabric) {
        try {
            String path = fabric ? "icons/fabric.png" : "icons/forge.png";
            Image img = new Image(getClass().getClassLoader().getResourceAsStream(path));
            iv.setImage(img);
        } catch (Exception e) {
            System.out.println("Could not load loader icon");
        }
    }

    private void handlePlay(Button playBtn) {
        if (!accountManager.hasAccounts()) {
            String original = playBtn.getText();
            playBtn.setText("Login first!");
            playBtn.setStyle(
                "-fx-background-color: #1a0000; -fx-text-fill: #f44336; " +
                "-fx-font-size: 13; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono'; " +
                "-fx-border-color: #2a0000; -fx-border-width: 1 0 1 0; " +
                "-fx-background-radius: 0; -fx-cursor: hand; -fx-padding: 16 24;"
            );
            new Timeline(new KeyFrame(Duration.seconds(2), e -> {
                playBtn.setText(original);
                playBtn.setStyle(
                    "-fx-background-color: #0f0f0f; -fx-text-fill: #ffffff; " +
                    "-fx-font-size: 13; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono'; " +
                    "-fx-border-color: #1a1a1a; -fx-border-width: 1 0 1 0; " +
                    "-fx-background-radius: 0; -fx-cursor: hand; -fx-padding: 16 24; -fx-opacity: 0.88;"
                );
            })).play();
            return;
        }

        playBtn.setDisable(true);
        playBtn.setText("Launching...");

        LaunchLogWindow logWindow = new LaunchLogWindow();
        logWindow.show();

        AccountManager.Account account = accountManager.getSelected();
        boolean useFabric = fabricMode;
        String version    = currentVersion;

        Thread thread = new Thread(() -> {
            try {
                if (useFabric) {
                    MinecraftLauncher.launch(version, account, settingsManager, logWindow::appendLog);
                } else {
                    MinecraftLauncher.launch("1.8.9", account, settingsManager, logWindow::appendLog);
                }
                javafx.application.Platform.runLater(() -> {
                    playBtn.setText("▶   Play  [" + version + "]");
                    playBtn.setDisable(false);
                    logWindow.setTitle("Rocket Client — Minecraft Running");
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    playBtn.setText("Launch failed!");
                    playBtn.setDisable(false);
                    logWindow.appendLog("ERROR: " + ex.getMessage());
                    logWindow.setTitle("Rocket Client — Launch Failed");
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private String loadRandomQuote() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("quotes.json");
            List<String> quotes = new Gson().fromJson(
                new InputStreamReader(is),
                new TypeToken<List<String>>(){}.getType()
            );
            return quotes.get(new Random().nextInt(quotes.size()));
        } catch (Exception e) {
            return "";
        }
    }
}

