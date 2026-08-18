package com.rocketclient;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.text.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public class SettingsPanel extends VBox {

    private final SettingsManager settings;

    public SettingsPanel(SettingsManager settings) {
        this.settings = settings;

        setStyle("-fx-background-color: #0f0f0f;");
        setPrefWidth(480);
        setPrefHeight(520);
        setPadding(new Insets(24));
        setSpacing(16);

        Label title = new Label("Settings");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-opacity: 0.88;");

        HBox tabBar = new HBox(2);
        tabBar.setStyle("-fx-border-color: #1a1a1a; -fx-border-width: 0 0 1 0;");

        Button launchTab  = tabButton("Launch",  true);
        Button discordTab = tabButton("Discord", false);
        Button aboutTab   = tabButton("About",   false);
        Button devToolsTab = tabButton("DevTools", false);
        tabBar.getChildren().addAll(launchTab, discordTab, aboutTab, devToolsTab);

        VBox launchPanel  = buildLaunchPanel();
        VBox discordPanel = buildDiscordPanel();
        VBox aboutPanel   = buildAboutPanel();
        VBox devtoolsPanel = buildDevtoolsPanel();

        discordPanel.setVisible(false); discordPanel.setManaged(false);
        aboutPanel.setVisible(false);   aboutPanel.setManaged(false);
        devtoolsPanel.setVisible(false);
        devtoolsPanel.setManaged(false);

        StackPane content = new StackPane(launchPanel, discordPanel, aboutPanel, devtoolsPanel);
        VBox.setVgrow(content, Priority.ALWAYS);

        launchTab.setOnAction(e -> {
            launchPanel.setVisible(true);   launchPanel.setManaged(true);
            discordPanel.setVisible(false); discordPanel.setManaged(false);
            aboutPanel.setVisible(false);   aboutPanel.setManaged(false);
            devtoolsPanel.setVisible(false);
            devtoolsPanel.setManaged(false);
            setActive(launchTab, discordTab, aboutTab, devToolsTab);
        });

        discordTab.setOnAction(e -> {
            discordPanel.setVisible(true);  discordPanel.setManaged(true);
            launchPanel.setVisible(false);  launchPanel.setManaged(false);
            aboutPanel.setVisible(false);   aboutPanel.setManaged(false);
            devtoolsPanel.setVisible(false);
            devtoolsPanel.setManaged(false);
            setActive(discordTab, launchTab, aboutTab, devToolsTab);
        });

        aboutTab.setOnAction(e -> {
            aboutPanel.setVisible(true);    aboutPanel.setManaged(true);
            launchPanel.setVisible(false);  launchPanel.setManaged(false);
            discordPanel.setVisible(false); discordPanel.setManaged(false);
            devtoolsPanel.setVisible(false);
            devtoolsPanel.setManaged(false);
            setActive(aboutTab, launchTab, discordTab, devToolsTab);
        });
        devToolsTab.setOnAction(e -> {
            aboutPanel.setVisible(false);
            aboutPanel.setManaged(false);
            launchPanel.setVisible(false);
            launchPanel.setManaged(false);
            discordPanel.setVisible(false);
            discordPanel.setManaged(false);
            devtoolsPanel.setVisible(true);
            devtoolsPanel.setManaged(true);
            setActive(devToolsTab, launchTab, discordTab, aboutTab);
        });


        getChildren().addAll(title, tabBar, content);
    }

    private VBox buildLaunchPanel() {
        // Self-correct a stale settings.json saved before these became mutually exclusive.
        if (settings.hideLauncher && settings.closeLauncher) {
            settings.hideLauncher = false;
            settings.save();
        }

        VBox panel = new VBox(16);
        panel.setPadding(new Insets(16, 0, 0, 0));

        panel.getChildren().add(sectionLabel("Java Path"));
        HBox javaPathRow = new HBox(8);
        TextField javaPathField = new TextField(settings.javaPath);
        javaPathField.setStyle(fieldStyle());
        javaPathField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(javaPathField, Priority.ALWAYS);

        Button browseBtn = new Button("Browse");
        browseBtn.setStyle(secondaryBtnStyle());
        browseBtn.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Select Java Executable");
            java.io.File f = fc.showOpenDialog(null);
            if (f != null) {
                javaPathField.setText(f.getAbsolutePath());
                settings.javaPath = f.getAbsolutePath();
                settings.save();
            }
        });

        javaPathField.textProperty().addListener((obs, o, n) -> {
            settings.javaPath = n;
            settings.save();
        });

        javaPathRow.getChildren().addAll(javaPathField, browseBtn);
        panel.getChildren().add(javaPathRow);

        panel.getChildren().add(sectionLabel("Java Arguments"));
        TextField javaArgsField = new TextField(settings.javaArgs);
        javaArgsField.setStyle(fieldStyle());
        javaArgsField.setMaxWidth(Double.MAX_VALUE);
        javaArgsField.textProperty().addListener((obs, o, n) -> {
            settings.javaArgs = n;
            settings.save();
        });
        panel.getChildren().add(javaArgsField);

        int systemMax = SettingsManager.getSystemMaxRamMb();
        Label ramLabel = new Label(settings.ramMb + " MB");
        ramLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");

        panel.getChildren().add(sectionLabel("RAM Allocation (System max: " + systemMax + " MB)"));
        Slider ramSlider = new Slider(512, systemMax, settings.ramMb);
        ramSlider.setBlockIncrement(512);
        ramSlider.setMajorTickUnit(1024);
        ramSlider.getStyleClass().add("rocket-slider");
        ramSlider.setMaxWidth(Double.MAX_VALUE);
        ramSlider.valueProperty().addListener((obs, o, n) -> {
            int val = (n.intValue() / 512) * 512;
            ramLabel.setText(val + " MB");
            settings.ramMb = val;
            settings.save();
        });
        panel.getChildren().addAll(ramSlider, ramLabel);

        panel.getChildren().add(sectionLabel("Launcher Behaviour"));
        HBox hideRow = toggleRow("Hide launcher when Minecraft launches", settings.hideLauncher, val -> {
            settings.hideLauncher = val;
            settings.save();
        });
        HBox closeRow = toggleRow("Close launcher without closing Minecraft", settings.closeLauncher, val -> {
            settings.closeLauncher = val;
            settings.save();
        });

        // These two behaviors are mutually exclusive - only one can actually take effect when
        // Minecraft launches (closeLauncher wins in code if both were on), so keep the UI honest
        // by turning the other off automatically instead of letting them silently disagree.
        Button hideToggleBtn  = toggleButtonOf(hideRow);
        Button closeToggleBtn = toggleButtonOf(closeRow);
        hideToggleBtn.setOnAction(e -> {
            settings.hideLauncher = !settings.hideLauncher;
            setToggleState(hideToggleBtn, settings.hideLauncher);
            if (settings.hideLauncher && settings.closeLauncher) {
                settings.closeLauncher = false;
                setToggleState(closeToggleBtn, false);
            }
            settings.save();
        });
        closeToggleBtn.setOnAction(e -> {
            settings.closeLauncher = !settings.closeLauncher;
            setToggleState(closeToggleBtn, settings.closeLauncher);
            if (settings.closeLauncher && settings.hideLauncher) {
                settings.hideLauncher = false;
                setToggleState(hideToggleBtn, false);
            }
            settings.save();
        });

        panel.getChildren().add(hideRow);
        panel.getChildren().add(closeRow);
        panel.getChildren().add(toggleRow("Enable system tray icon (restart required)", settings.enableTray, val -> {
            settings.enableTray = val;
            settings.save();
        }));

        if (StartupManager.isSupported()) {
            panel.getChildren().add(toggleRow("Launch Rocket Client on system startup", settings.launchOnStartup, val -> {
                settings.launchOnStartup = val;
                settings.save();
                StartupManager.setEnabled(val);
            }));
        }



        return panel;
    }

    private VBox buildDiscordPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(16, 0, 0, 0));

        panel.getChildren().add(sectionLabel("Discord Rich Presence"));
        panel.getChildren().add(toggleRow("Enable Discord Rich Presence", settings.discordRpc, val -> {
            settings.discordRpc = val;
            settings.save();
        }));

        Label info = new Label("Shows what you're doing in Minecraft as your Discord status.");
        info.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        info.setWrapText(true);
        panel.getChildren().add(info);

        return panel;
    }

    void ChangeText(Button btn, String newText, boolean slower) {
        Text dummyText = new Text(newText);
        dummyText.setFont(Font.font("JetBrains Mono", 12));

        Insets padding = btn.getPadding();
        double horizontalPadding = (padding != null) ? (padding.getLeft() + padding.getRight()) : 32.0;
        double safetyBuffer = 6.0;

        double targetWidth = Math.ceil(dummyText.getLayoutBounds().getWidth() + horizontalPadding + safetyBuffer);

        if (btn.getPrefWidth() <= 0) {
            btn.setPrefWidth(btn.getWidth());
        }

        btn.setText(newText);

        Timeline timeline = new Timeline();
        KeyValue widthValue = new KeyValue(
                btn.prefWidthProperty(),
                targetWidth,
                Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0) // Smooth ease-in-out curve
        );
        KeyFrame keyFrame = new KeyFrame(Duration.millis(300), widthValue);
        if (slower) {
            keyFrame = new KeyFrame(Duration.millis(1600), widthValue);
        }

        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }
    private VBox buildAboutPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(16, 0, 0, 0));
        panel.setAlignment(Pos.TOP_LEFT);

        Label version = new Label("Rocket Client — v1.0");
        version.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-opacity: 0.88;");

        Label desc = new Label("A modern, lightweight Minecraft launcher built with love and.. Java");
        desc.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        desc.setWrapText(true);

        panel.getChildren().add(sectionLabel("Info"));
        panel.getChildren().addAll(version, desc);
        panel.getChildren().add(sectionLabel("Links"));
        panel.getChildren().add(linkLabel("Discord", "https://discord.com/invite/urHfdFdsbh"));
        panel.getChildren().add(linkLabel("Website", "https://rocketclient.rocketclient.abrdns.com"));
        panel.getChildren().add(sectionLabel("Updates"));
        Button checkUpdateBtn = new Button("Check for Updates");
        checkUpdateBtn.setStyle(secondaryBtnStyle());
        Label updateStatus = new Label();
        updateStatus.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        checkUpdateBtn.setOnAction(e -> {
            checkUpdateBtn.setDisable(true);
            checkUpdateBtn.setText("Checking...");
            Thread thread = new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL("https://api.github.com/repos/Pernoise/Rocket-client/releases/latest");
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    String response = new String(conn.getInputStream().readAllBytes());
                    com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(response, com.google.gson.JsonObject.class);
                    String latest = json.get("tag_name").getAsString();
                    javafx.application.Platform.runLater(() -> {
                        updateStatus.setText("Latest: " + latest);
                        checkUpdateBtn.setText("Check for Updates");
                        checkUpdateBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        updateStatus.setText("Could not check for updates.");
                        checkUpdateBtn.setText("Check for Updates");
                        checkUpdateBtn.setDisable(false);
                    });
                }
            });
            thread.setDaemon(true);
            thread.start();
        });

        panel.getChildren().addAll(checkUpdateBtn, updateStatus);

        panel.getChildren().add(sectionLabel("Credits"));
        Label credits = new Label("Built by Pernoise");
        credits.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        panel.getChildren().add(credits);

        HBox avatarCreditRow = new HBox(4);
        avatarCreditRow.setAlignment(Pos.CENTER_LEFT);
        Label creditPrefix = new Label("Thank you to");
        creditPrefix.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        Label creditSuffix = new Label("for providing avatars.");
        creditSuffix.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        avatarCreditRow.getChildren().addAll(creditPrefix, linkLabel("Crafatar", "https://crafatar.com"), creditSuffix);
        panel.getChildren().add(avatarCreditRow);

        return panel;
    }

    private VBox buildDevtoolsPanel() {

        VBox panel = new VBox(10);
        Button clearLocalDataBtn = new Button("Clear RocketClient data");
        clearLocalDataBtn.setStyle(secondaryBtnStyle() + " -fx-background-color: #2E0000;");
        //System.out.println("ironically enough, the devtools panel is in development.");
        clearLocalDataBtn.setOnAction(e -> {

            if (clearLocalDataBtn.getText().equals("Clear RocketClient data")) {
                ChangeText(clearLocalDataBtn, "Are you sure?", false);
                return;
            }
            if (clearLocalDataBtn.getText().equals("Are you sure?")) {
                ChangeText(clearLocalDataBtn, "Are you really sure?", false);
                return;
            }
            if (clearLocalDataBtn.getText().equals("Are you really sure?")) {
                ChangeText(clearLocalDataBtn, "Are you really actually sure?", false);
                return;
            }
            if (clearLocalDataBtn.getText().equals("Are you really actually sure?")) {
                ChangeText(clearLocalDataBtn, "Are you really actually extremely sure?", false);
                return;
            }
            if (clearLocalDataBtn.getText().equals("Are you really actually extremely sure?")) {
                ChangeText(clearLocalDataBtn, "One last click for good luck?", false);
                return;
            }
            Path path = Paths.get(System.getProperty("user.home"), ".rocketclient");

            if (Files.exists(path) && clearLocalDataBtn.getText().equals("One last click for good luck?")) {
                try {
                    // Walk the file tree in reverse (deleting files/subfolders before the parent folder)
                    try (Stream<Path> walk = Files.walk(path)) {
                        walk.sorted(Comparator.reverseOrder())
                                .forEach(p -> {
                                    try {
                                        Files.delete(p);
                                    } catch (IOException ex) {
                                        throw new UncheckedIOException(ex);
                                    }
                                });
                    }
                    ChangeText(clearLocalDataBtn, "Nuked data folder.", false);
                    return;
                } catch (IOException | UncheckedIOException ex) {
                    System.out.println("File deletion failed! " + ex.getMessage());
                    ChangeText(clearLocalDataBtn, "Failed to nuke the data folder.", false);
                    return;
                }
            }
            if (clearLocalDataBtn.getText().equals("Failed to nuke the data folder.") || clearLocalDataBtn.getText().equals("Nuked data folder.")) {
                ChangeText(clearLocalDataBtn, "Clear RocketClient data.", true);
            }


        });
        panel.getChildren().addAll(clearLocalDataBtn);
        return panel;

    }

    private Label sectionLabel(String text) {
        Label l = new Label(text.toUpperCase());
        l.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 9; -fx-font-family: 'JetBrains Mono';");
        return l;
    }

    /** Finds the toggle Button inside a row built by toggleRow(), so its state can be controlled externally. */
    private Button toggleButtonOf(HBox row) {
        return (Button) row.getChildren().stream()
            .filter(n -> n instanceof Button)
            .findFirst()
            .orElseThrow();
    }

    private void setToggleState(Button toggle, boolean on) {
        toggle.setText(on ? "ON" : "OFF");
        toggle.setStyle(on ? toggleOnStyle() : toggleOffStyle());
    }

    private HBox toggleRow(String text, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(12);

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12; -fx-font-family: 'JetBrains Mono';");
        HBox.setHgrow(label, Priority.ALWAYS);

        // Flat toggle button instead of checkbox
        final boolean[] state = {initial};
        Button toggle = new Button(initial ? "ON" : "OFF");
        toggle.setStyle(initial ? toggleOnStyle() : toggleOffStyle());
        toggle.setPrefWidth(48);
        toggle.setPrefHeight(22);
        toggle.setOnAction(e -> {
            state[0] = !state[0];
            toggle.setText(state[0] ? "ON" : "OFF");
            toggle.setStyle(state[0] ? toggleOnStyle() : toggleOffStyle());
            onChange.accept(state[0]);
        });

        row.getChildren().addAll(label, toggle);
        return row;
    }

    private String toggleOnStyle() {
        return "-fx-background-color: #ffffff; -fx-text-fill: #000000; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 9; -fx-font-weight: bold; " +
               "-fx-background-radius: 4; -fx-cursor: hand;";
    }

    private String toggleOffStyle() {
        return "-fx-background-color: #1a1a1a; -fx-text-fill: #ffffff; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 9; -fx-font-weight: bold; " +
               "-fx-background-radius: 4; -fx-cursor: hand; -fx-border-color: #2a2a2a; -fx-border-radius: 4; -fx-border-width: 0.5;";
    }

    private Label linkLabel(String text, String url) {
        Label l = new Label(text + " →");
        l.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12; -fx-font-family: 'JetBrains Mono'; -fx-cursor: hand;");
        l.setOnMouseClicked(e -> BrowserUtil.open(url));
        l.setOnMouseEntered(e -> l.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12; -fx-font-family: 'JetBrains Mono'; -fx-cursor: hand;"));
        l.setOnMouseExited(e  -> l.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12; -fx-font-family: 'JetBrains Mono'; -fx-cursor: hand;"));
        return l;
    }

    private Button tabButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setStyle(active ? activeTabStyle() : inactiveTabStyle());
        return btn;
    }

    private void setActive(Button active, Button... rest) {
        active.setStyle(activeTabStyle());
        for (Button b : rest) b.setStyle(inactiveTabStyle());
    }

    private String activeTabStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #ffffff; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
               "-fx-border-color: transparent transparent #ffffff transparent; " +
               "-fx-border-width: 0 0 1.5 0; -fx-padding: 8 14; -fx-cursor: hand;";
    }

    private String inactiveTabStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #ffffff; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
               "-fx-border-color: transparent; -fx-padding: 8 14; -fx-cursor: hand;";
    }

    private String fieldStyle() {
        return "-fx-background-color: #141414; -fx-text-fill: #ffffff; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
               "-fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
               "-fx-padding: 9 12; -fx-prompt-text-fill: #333333;";
    }

    private String secondaryBtnStyle() {
        return "-fx-background-color: #141414; -fx-text-fill: #ffffff; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
               "-fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
               "-fx-cursor: hand; -fx-padding: 8 16;";
    }

}
