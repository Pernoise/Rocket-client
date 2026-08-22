package com.rogueclient;

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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
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
    private final InstanceManager instanceManager = new InstanceManager();

    private InstanceManager.Instance currentInstance = null;
    private boolean fabricMode = true;

    private HBox recentRow;
    private Button selectBtn;
    private HBox playRow;
    private Button playBtn;
    private VBox dropdownContent;
    private ScrollPane scrollPane;
    private ImageView loaderIcon;

    private static final String[] VERSIONS = {
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
        "1.14.4", "1.14.3", "1.14.2", "1.14.1", "1.14",
        "1.8.9"
    };

    public CenterPanel(AccountManager accountManager, SettingsManager settingsManager) {
        this.accountManager  = accountManager;
        this.settingsManager = settingsManager;

        setAlignment(Pos.CENTER);
        setPadding(new Insets(28, 36, 22, 36));
        setSpacing(6);

        Label name = new Label("Rogue Client");
        Font customFont = Font.loadFont(getClass().getResourceAsStream("/fonts/gondens-demo/Gondens DEMO.otf"), 64);
        if (customFont != null) {
            name.setFont(customFont);
            name.setStyle("-fx-text-fill: #ffffff; -fx-opacity: 0.88;");
        } else {
            name.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 64; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-opacity: 0.88;");
        }

        Label quote = new Label(loadRandomQuote());
        quote.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-font-style: italic; -fx-font-weight: bold;");
        quote.setWrapText(true);
        quote.setMaxWidth(420);
        quote.setAlignment(Pos.CENTER);

        VBox topSpacer = new VBox();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);

        recentRow = new HBox(8);
        recentRow.setAlignment(Pos.CENTER);
        recentRow.setMaxWidth(420);
        recentRow.setPadding(new Insets(10));
        recentRow.setStyle("-fx-border-color: #1a1a1a; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;");

        VBox bottomSpacer = new VBox();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        loaderIcon = new ImageView();
        loaderIcon.setFitWidth(22);
        loaderIcon.setFitHeight(22);
        loaderIcon.setPreserveRatio(true);
        setLoaderIcon(loaderIcon, true);

        playBtn = new Button();
        playBtn.setStyle(playBtnStyle());
        playBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(playBtn, Priority.ALWAYS);
        playBtn.setOnMouseEntered(e -> playBtn.setStyle(playBtnHoverStyle()));
        playBtn.setOnMouseExited(e -> playBtn.setStyle(playBtnStyle()));
        playBtn.setOnAction(e -> handlePlay(playBtn));

        Button versionBtn = new Button("v");
        versionBtn.setStyle(versionBtnStyle());
        versionBtn.setOnMouseEntered(e -> versionBtn.setStyle(versionBtnHoverStyle()));
        versionBtn.setOnMouseExited(e -> versionBtn.setStyle(versionBtnStyle()));

        playRow = new HBox(8, playBtn, versionBtn);
        playRow.setMaxWidth(Double.MAX_VALUE);
        playRow.setAlignment(Pos.CENTER);
        playRow.setVisible(false);
        playRow.setManaged(false);

        selectBtn = new Button("Select or create an instance");
        selectBtn.setStyle(playBtnStyle());
        selectBtn.setMaxWidth(Double.MAX_VALUE);
        selectBtn.setOnMouseEntered(e -> selectBtn.setStyle(playBtnHoverStyle()));
        selectBtn.setOnMouseExited(e -> selectBtn.setStyle(playBtnStyle()));
        selectBtn.setOnAction(e -> toggleDropdown());

        dropdownContent = new VBox(2);
        dropdownContent.setPadding(new Insets(4));

        scrollPane = new ScrollPane(dropdownContent);
        scrollPane.getStyleClass().add("rogue-scroll");
        scrollPane.setMaxHeight(220);
        scrollPane.setMaxWidth(420);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #0d0d0d; -fx-background-color: #0d0d0d; -fx-border-color: #1a1a1a; -fx-border-radius: 7; -fx-background-radius: 7;");
        scrollPane.setVisible(false);
        scrollPane.setManaged(false);

        versionBtn.setOnAction(e -> toggleDropdown());

        refreshRecentRow();

        getChildren().addAll(name, quote, topSpacer, recentRow, bottomSpacer, selectBtn, playRow, scrollPane);
    }

    private void toggleDropdown() {
        boolean opening = !scrollPane.isVisible();
        if (opening) showInstancePicker();
        scrollPane.setVisible(opening);
        scrollPane.setManaged(opening);
    }

    private void closeDropdown() {
        scrollPane.setVisible(false);
        scrollPane.setManaged(false);
    }

    private void showInstancePicker() {
        dropdownContent.getChildren().clear();

        List<InstanceManager.Instance> all = instanceManager.list();
        for (InstanceManager.Instance inst : all) {
            dropdownContent.getChildren().add(dropdownRow(inst.name, () -> {
                selectInstance(inst);
                closeDropdown();
            }));
        }

        Label newInstanceRow = new Label("+  New instance");
        newInstanceRow.setStyle(dropdownRowStyle() + " -fx-text-fill: #888888;");
        newInstanceRow.setMaxWidth(Double.MAX_VALUE);
        newInstanceRow.setOnMouseEntered(e -> newInstanceRow.setStyle(dropdownRowHoverStyle() + " -fx-text-fill: #ffffff;"));
        newInstanceRow.setOnMouseExited(e -> newInstanceRow.setStyle(dropdownRowStyle() + " -fx-text-fill: #888888;"));
        newInstanceRow.setOnMouseClicked(e -> showVersionPicker());
        dropdownContent.getChildren().add(newInstanceRow);
    }

    private void showVersionPicker() {
        dropdownContent.getChildren().clear();
        for (String v : VERSIONS) {
            dropdownContent.getChildren().add(dropdownRow(v, () -> {
                closeDropdown();
                String loader = v.equals("1.8.9") ? "forge" : "fabric";
                NewInstanceDialog.open(instanceManager, v, loader, created -> {
                    selectInstance(created);
                    refreshRecentRow();
                });
            }));
        }
    }

    private Label dropdownRow(String text, Runnable onClick) {
        Label row = new Label(text);
        row.setStyle(dropdownRowStyle());
        row.setMaxWidth(Double.MAX_VALUE);
        row.setOnMouseEntered(e -> row.setStyle(dropdownRowHoverStyle()));
        row.setOnMouseExited(e -> row.setStyle(dropdownRowStyle()));
        row.setOnMouseClicked(e -> onClick.run());
        return row;
    }

    private String dropdownRowStyle() {
        return "-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-padding: 6 10;";
    }

    private String dropdownRowHoverStyle() {
        return dropdownRowStyle() + " -fx-background-color: #161616; -fx-background-radius: 5;";
    }

    private void selectInstance(InstanceManager.Instance inst) {
        currentInstance = inst;
        fabricMode = !inst.loader.equals("forge");
        setLoaderIcon(loaderIcon, fabricMode);
        playBtn.setText(">   Play  [" + inst.name + "]");

        selectBtn.setVisible(false);
        selectBtn.setManaged(false);
        playRow.setVisible(true);
        playRow.setManaged(true);

        refreshRecentRow();
    }

    private void refreshRecentRow() {
        recentRow.getChildren().clear();
        List<InstanceManager.Instance> recent = instanceManager.recent(3);
        for (InstanceManager.Instance inst : recent) {
            recentRow.getChildren().add(recentTile(inst));
        }
        recentRow.setVisible(!recent.isEmpty());
        recentRow.setManaged(!recent.isEmpty());
    }

    private VBox recentTile(InstanceManager.Instance inst) {
        boolean isSelected = currentInstance != null && currentInstance.id.equals(inst.id);

        ImageView iconView = new ImageView();
        iconView.setFitWidth(20);
        iconView.setFitHeight(20);
        iconView.setPreserveRatio(true);
        try {
            Image img = IconUtil.load(inst.icon);
            if (img != null) iconView.setImage(img);
        } catch (Exception ignored) {}

        StackPane iconBox = new StackPane(iconView);
        iconBox.setPrefSize(28, 28);
        iconBox.setMaxSize(28, 28);
        iconBox.setStyle("-fx-background-color: " + (isSelected ? "#1f1f1f" : "#161616") + "; -fx-background-radius: 6;");

        Label nameLabel = new Label(inst.name);
        nameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");

        Label timeLabel = new Label(relativeTime(inst.lastPlayed));
        timeLabel.setStyle("-fx-text-fill: " + (isSelected ? "#aaaaaa" : "#888888") + "; -fx-font-size: 9; -fx-font-family: 'JetBrains Mono';");

        VBox tile = new VBox(6, iconBox, nameLabel, timeLabel);
        tile.setPadding(new Insets(10));
        tile.setStyle(
            "-fx-background-color: " + (isSelected ? "#161616" : "#0f0f0f") + "; " +
            "-fx-border-color: " + (isSelected ? "#ffffff" : "#1a1a1a") + "; " +
            "-fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        tile.setPrefWidth(120);
        tile.setOnMouseClicked(e -> selectInstance(inst));
        return tile;
    }

    private String relativeTime(long lastPlayed) {
        if (lastPlayed <= 0) return "never played";
        long diffMs = System.currentTimeMillis() - lastPlayed;
        long minutes = diffMs / 60000;
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
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
        if (currentInstance == null) return;

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
                playBtn.setStyle(playBtnStyle());
            })).play();
            return;
        }

        playBtn.setDisable(true);
        playBtn.setText("Launching...");

        LaunchLogWindow logWindow = new LaunchLogWindow();
        logWindow.show();

        AccountManager.Account account = accountManager.getSelected();
        boolean useFabric = fabricMode;
        String version = currentInstance.mcVersion;
        InstanceManager.Instance instanceAtLaunch = currentInstance;
        String playLabel = ">   Play  [" + instanceAtLaunch.name + "]";

        DiscordRPC.updatePlaying(version);

        Thread thread = new Thread(() -> {
            try {
                if (useFabric) {
                    MinecraftLauncher.launch(instanceAtLaunch, account, settingsManager, logWindow::appendLog);
                } else {
                    ForgeLauncher.launch(account, settingsManager, logWindow::appendLog);
                }
                instanceManager.markPlayed(instanceAtLaunch);
                javafx.application.Platform.runLater(() -> {
                    playBtn.setText(playLabel);
                    playBtn.setDisable(false);
                    logWindow.setTitle("Rogue Client - Minecraft Running");
                    refreshRecentRow();

                    if (settingsManager.closeLauncher) {
                        TrayManager.quit();
                    } else if (settingsManager.hideLauncher) {
                        TrayManager.minimizeToTray();
                    }
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    playBtn.setText("Launch failed!");
                    playBtn.setDisable(false);
                    logWindow.appendLog("ERROR: " + ex.getMessage());
                    logWindow.setTitle("Rogue Client - Launch Failed");
                });
                DiscordRPC.setPresence("In the launcher", "Rogue Client Beta v0.4");
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
        }
        quotes.addAll(Arrays.asList(EXTRA_QUOTES));

        if (quotes.isEmpty()) return "";
        return quotes.get(new Random().nextInt(quotes.size()));
    }
}
