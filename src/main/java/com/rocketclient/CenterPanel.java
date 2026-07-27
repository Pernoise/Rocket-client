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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class CenterPanel extends VBox {

    private final AccountManager accountManager;
    private final SettingsManager settingsManager;
    private boolean fabricMode = true;
    private String currentVersion = "26.2";

    public CenterPanel(AccountManager accountManager, SettingsManager settingsManager) {
        this.accountManager  = accountManager;
        this.settingsManager = settingsManager;

        setAlignment(Pos.CENTER);
        setPadding(new Insets(28, 36, 22, 36));
        setSpacing(6);

        Label name = new Label("Rocket Client");
        name.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 28; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-opacity: 0.88;");

        Label tagline = new Label("A Minecraft performance-focused client - BETA");
        tagline.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");

        Label quote = new Label(loadRandomQuote());
        quote.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-font-style: italic; -fx-font-weight: bold;");
        quote.setWrapText(true);
        quote.setMaxWidth(420);
        quote.setAlignment(Pos.CENTER);

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        ImageView loaderIcon = new ImageView();
        loaderIcon.setFitWidth(22);
        loaderIcon.setFitHeight(22);
        loaderIcon.setPreserveRatio(true);
        setLoaderIcon(loaderIcon, true);

        Button loaderBtn = new Button();
        loaderBtn.setGraphic(loaderIcon);
        loaderBtn.setStyle(loaderBtnStyle());
        loaderBtn.setOnMouseEntered(e -> loaderBtn.setStyle(loaderBtnHoverStyle()));
        loaderBtn.setOnMouseExited(e -> loaderBtn.setStyle(loaderBtnStyle()));
        loaderBtn.setTooltip(new javafx.scene.control.Tooltip("Fabric - click to switch to Forge 1.8.9"));

        Button playBtn = new Button(">   Play  [" + currentVersion + "]");
        playBtn.setStyle(playBtnStyle());
        playBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(playBtn, Priority.ALWAYS);

        playBtn.setOnMouseEntered(e -> playBtn.setStyle(playBtnHoverStyle()));
        playBtn.setOnMouseExited(e -> playBtn.setStyle(playBtnStyle()));

        Button versionBtn = new Button("v");
        versionBtn.setStyle(versionBtnStyle());
        versionBtn.setOnMouseEntered(e -> versionBtn.setStyle(versionBtnHoverStyle()));
        versionBtn.setOnMouseExited(e -> versionBtn.setStyle(versionBtnStyle()));

        HBox playRow = new HBox(8, loaderBtn, playBtn, versionBtn);
        playRow.setMaxWidth(Double.MAX_VALUE);
        playRow.setAlignment(Pos.CENTER);

        VBox versionList = new VBox(2);
        versionList.setStyle("-fx-background-color: #0d0d0d;");
        versionList.setPadding(new Insets(4));

        String[] versions = {
            "26.2", "26.1.2", "26.1.1", "26.1",
            "1.21.11", "1.21.10", "1.21.9", "1.21.8", "1.21.7",
            "1.21.6", "1.21.5", "1.21.4", "1.21.3", "1.21.2",
            "1.21.1", "1.21", "1.20.6", "1.20.5", "1.20.4",
            "1.20.3", "1.20.2", "1.20.1", "1.20", "1.19.4",
            "1.19.3", "1.19.2", "1.19.1", "1.19",
            "1.18.2", "1.18.1", "1.18",
            "1.17.1", "1.17",
            "1.16.5", "1.16.4", "1.16.3", "1.16.2", "1.16.1", "1.16",
            "1.15.2", "1.15.1", "1.15",
            "1.14.4", "1.14.3", "1.14.2", "1.14.1", "1.14"
        };

        ScrollPane scrollPane = new ScrollPane(versionList);
        scrollPane.getStyleClass().add("rocket-scroll");
        scrollPane.setMaxHeight(160);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #0d0d0d; -fx-background-color: #0d0d0d; -fx-border-color: #1a1a1a; -fx-border-radius: 7; -fx-background-radius: 7;");
        scrollPane.setVisible(false);
        scrollPane.setManaged(false);

        for (String v : versions) {
            Label vLabel = new Label(v);
            vLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-padding: 6 10;");
            vLabel.setMaxWidth(Double.MAX_VALUE);
            vLabel.setOnMouseEntered(e -> vLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-padding: 6 10; -fx-background-color: #161616; -fx-background-radius: 5;"));
            vLabel.setOnMouseExited(e  -> vLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-padding: 6 10;"));
            vLabel.setOnMouseClicked(e -> {
                currentVersion = v;
                playBtn.setText(">   Play  [" + v + "]");
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
                currentVersion = "26.2";
                playBtn.setText(">   Play  [26.2]");
                versionBtn.setVisible(true);
                versionBtn.setManaged(true);
                loaderBtn.setTooltip(new javafx.scene.control.Tooltip("Fabric - click to switch to Forge 1.8.9"));
            } else {
                currentVersion = "1.8.9";
                playBtn.setText(">   Play  [1.8.9 - Forge]");
                versionBtn.setVisible(false);
                versionBtn.setManaged(false);
                scrollPane.setVisible(false);
                scrollPane.setManaged(false);
                loaderBtn.setTooltip(new javafx.scene.control.Tooltip("Forge 1.8.9 - click to switch to Fabric"));
            }
        });

        playBtn.setOnAction(e -> handlePlay(playBtn));

        getChildren().addAll(name, tagline, quote, spacer, playRow, scrollPane);
    }

    private String loaderBtnStyle() {
        return "-fx-background-color: #0f0f0f; -fx-border-color: #1a1a1a; " +
            "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; " +
            "-fx-cursor: hand; -fx-padding: 14 10;";
    }

    private String loaderBtnHoverStyle() {
        return "-fx-background-color: #161616; -fx-border-color: #1a1a1a; " +
            "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; " +
            "-fx-cursor: hand; -fx-padding: 14 10;";
    }

    private String playBtnStyle() {
        return "-fx-background-color: #0f0f0f; -fx-text-fill: #ffffff; " +
            "-fx-font-size: 13; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono'; " +
            "-fx-border-color: #1a1a1a; -fx-border-width: 1; " +
            "-fx-background-radius: 8; -fx-border-radius: 8; " +
            "-fx-cursor: hand; -fx-padding: 16 24; -fx-opacity: 0.88;";
    }

    private String playBtnHoverStyle() {
        return "-fx-background-color: #161616; -fx-text-fill: #ffffff; " +
            "-fx-font-size: 13; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono'; " +
            "-fx-border-color: #1a1a1a; -fx-border-width: 1; " +
            "-fx-background-radius: 8; -fx-border-radius: 8; " +
            "-fx-cursor: hand; -fx-padding: 16 24; -fx-opacity: 0.88;";
    }

    private String versionBtnStyle() {
        return "-fx-background-color: #0f0f0f; -fx-text-fill: #ffffff; " +
            "-fx-font-size: 13; -fx-font-weight: bold; " +
            "-fx-border-color: #1a1a1a; -fx-border-width: 1; " +
            "-fx-border-radius: 8; -fx-background-radius: 8; " +
            "-fx-cursor: hand; -fx-min-width: 40; -fx-padding: 16 10;";
    }

    private String versionBtnHoverStyle() {
        return "-fx-background-color: #161616; -fx-text-fill: #ffffff; " +
            "-fx-font-size: 13; -fx-font-weight: bold; " +
            "-fx-border-color: #1a1a1a; -fx-border-width: 1; " +
            "-fx-border-radius: 8; -fx-background-radius: 8; " +
            "-fx-cursor: hand; -fx-min-width: 40; -fx-padding: 16 10;";
    }

    private void setLoaderIcon(ImageView iv, boolean fabric) {
        try {
            String path = fabric ? "icons/fabric.png" : "icons/anvil.png";
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

        DiscordRPC.updatePlaying(version);

        Thread thread = new Thread(() -> {
            try {
                if (useFabric) {
                    MinecraftLauncher.launch(version, account, settingsManager, logWindow::appendLog);
                } else {
                    ForgeLauncher.launch(account, settingsManager, logWindow::appendLog);
                }
                javafx.application.Platform.runLater(() -> {
                    playBtn.setText(">   Play  [" + version + "]");
                    playBtn.setDisable(false);
                    logWindow.setTitle("Rocket Client - Minecraft Running");
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    playBtn.setText("Launch failed!");
                    playBtn.setDisable(false);
                    logWindow.appendLog("ERROR: " + ex.getMessage());
                    logWindow.setTitle("Rocket Client - Launch Failed");
                });
                DiscordRPC.setPresence("In the launcher", "Rocket Client Beta v0.4");
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static final String[] EXTRA_QUOTES = {
        "For sight so dear can blind the soul... what use are eyes that don't make you whole?",
        "Damned by the light that shows the break, For my own imperfection's sake.",
        "Tell the storms, then, to come as they please, and tell the winds I am the man, for we've chosen to dream, to explore, to discover the breeze. A reflection of the sea brings cheer, thus shall it be, thus will it be."
    };

    private String loadRandomQuote() {
        List<String> quotes = new ArrayList<>();
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("quotes.json");
            List<String> fromFile = new Gson().fromJson(
                new InputStreamReader(is),
                new TypeToken<List<String>>(){}.getType()
            );
            if (fromFile != null) quotes.addAll(fromFile);
        } catch (Exception e) {
            // quotes.json missing or unreadable, fall back to the hardcoded set below
        }
        quotes.addAll(Arrays.asList(EXTRA_QUOTES));

        if (quotes.isEmpty()) return "";
        return quotes.get(new Random().nextInt(quotes.size()));
    }
}
