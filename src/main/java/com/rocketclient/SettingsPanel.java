package com.rocketclient;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

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
        Button logTab     = tabButton("Log",     false);
        Button aboutTab   = tabButton("About",   false);
        tabBar.getChildren().addAll(launchTab, discordTab, logTab, aboutTab);

        VBox launchPanel  = buildLaunchPanel();
        VBox discordPanel = buildDiscordPanel();
        VBox logPanel     = buildLogPanel();
        VBox aboutPanel   = buildAboutPanel();

        discordPanel.setVisible(false); discordPanel.setManaged(false);
        logPanel.setVisible(false);     logPanel.setManaged(false);
        aboutPanel.setVisible(false);   aboutPanel.setManaged(false);

        StackPane content = new StackPane(launchPanel, discordPanel, logPanel, aboutPanel);
        VBox.setVgrow(content, Priority.ALWAYS);

        launchTab.setOnAction(e -> {
            launchPanel.setVisible(true);   launchPanel.setManaged(true);
            discordPanel.setVisible(false); discordPanel.setManaged(false);
            logPanel.setVisible(false);     logPanel.setManaged(false);
            aboutPanel.setVisible(false);   aboutPanel.setManaged(false);
            setActive(launchTab, discordTab, logTab, aboutTab);
        });

        discordTab.setOnAction(e -> {
            discordPanel.setVisible(true);  discordPanel.setManaged(true);
            launchPanel.setVisible(false);  launchPanel.setManaged(false);
            logPanel.setVisible(false);     logPanel.setManaged(false);
            aboutPanel.setVisible(false);   aboutPanel.setManaged(false);
            setActive(discordTab, launchTab, logTab, aboutTab);
        });

        logTab.setOnAction(e -> {
            logPanel.setVisible(true);      logPanel.setManaged(true);
            launchPanel.setVisible(false);  launchPanel.setManaged(false);
            discordPanel.setVisible(false); discordPanel.setManaged(false);
            aboutPanel.setVisible(false);   aboutPanel.setManaged(false);
            setActive(logTab, launchTab, discordTab, aboutTab);
        });

        aboutTab.setOnAction(e -> {
            aboutPanel.setVisible(true);    aboutPanel.setManaged(true);
            launchPanel.setVisible(false);  launchPanel.setManaged(false);
            discordPanel.setVisible(false); discordPanel.setManaged(false);
            logPanel.setVisible(false);     logPanel.setManaged(false);
            setActive(aboutTab, launchTab, discordTab, logTab);
        });

        getChildren().addAll(title, tabBar, content);
    }

    private VBox buildLaunchPanel() {
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
        ramLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");

        panel.getChildren().add(sectionLabel("RAM Allocation (System max: " + systemMax + " MB)"));
        Slider ramSlider = new Slider(512, systemMax, settings.ramMb);
        ramSlider.setBlockIncrement(512);
        ramSlider.setMajorTickUnit(1024);
        ramSlider.setStyle("-fx-control-inner-background: #1a1a1a;");
        ramSlider.setMaxWidth(Double.MAX_VALUE);
        ramSlider.valueProperty().addListener((obs, o, n) -> {
            int val = (n.intValue() / 512) * 512;
            ramLabel.setText(val + " MB");
            settings.ramMb = val;
            settings.save();
        });
        panel.getChildren().addAll(ramSlider, ramLabel);

        panel.getChildren().add(sectionLabel("Launcher Behaviour"));
        panel.getChildren().add(toggleRow("Hide launcher when Minecraft launches", settings.hideLauncher, val -> {
            settings.hideLauncher = val;
            settings.save();
        }));
        panel.getChildren().add(toggleRow("Close launcher without closing Minecraft", settings.closeLauncher, val -> {
            settings.closeLauncher = val;
            settings.save();
        }));

        panel.getChildren().add(sectionLabel("Updates"));
        panel.getChildren().add(toggleRow("Automatically check for updates", settings.autoUpdate, val -> {
            settings.autoUpdate = val;
            settings.save();
        }));

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
        info.setStyle("-fx-text-fill: #333333; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        info.setWrapText(true);
        panel.getChildren().add(info);

        return panel;
    }

    private VBox buildLogPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16, 0, 0, 0));

        panel.getChildren().add(sectionLabel("Game Log"));

        TextArea logArea = new TextArea();
        logArea.setStyle("-fx-background-color: #0d0d0d; -fx-text-fill: #555555; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 10; -fx-border-color: #1a1a1a;");
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(300);
        logArea.setText("No log yet. Launch Minecraft to see output here.");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        Button uploadBtn = new Button("Copy Log to Clipboard");
        uploadBtn.setStyle(secondaryBtnStyle());
        uploadBtn.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(logArea.getText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            uploadBtn.setText("Copied!");
        });

        panel.getChildren().addAll(logArea, uploadBtn);
        return panel;
    }

    private VBox buildAboutPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(16, 0, 0, 0));
        panel.setAlignment(Pos.TOP_LEFT);

        Label version = new Label("Rocket Client — Alpha v0.1");
        version.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-opacity: 0.88;");

        Label desc = new Label("A performance-focused Minecraft client.\nOpen source, no ads, no telemetry.");
        desc.setStyle("-fx-text-fill: #444444; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        desc.setWrapText(true);

        panel.getChildren().add(sectionLabel("Info"));
        panel.getChildren().addAll(version, desc);

        panel.getChildren().add(sectionLabel("Links"));
        panel.getChildren().add(linkLabel("Discord", "https://discord.com/invite/urHfdFdsbh"));
        panel.getChildren().add(linkLabel("Website", "https://rocketclient.rocketclient.abrdns.com/#home"));

        panel.getChildren().add(sectionLabel("Updates"));
        Button checkUpdateBtn = new Button("Check for Updates");
        checkUpdateBtn.setStyle(secondaryBtnStyle());
        Label updateStatus = new Label();
        updateStatus.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");

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
        Label credits = new Label("Built by Pernoise\nUI: JavaFX + JetBrains Mono\nAuth: Microsoft OAuth + Ely.by");
        credits.setStyle("-fx-text-fill: #333333; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        panel.getChildren().add(credits);

        return panel;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text.toUpperCase());
        l.setStyle("-fx-text-fill: #2a2a2a; -fx-font-size: 9; -fx-font-family: 'JetBrains Mono';");
        return l;
    }

    private HBox toggleRow(String text, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(12);

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #666666; -fx-font-size: 12; -fx-font-family: 'JetBrains Mono';");
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
        return "-fx-background-color: #1a1a1a; -fx-text-fill: #444444; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 9; -fx-font-weight: bold; " +
               "-fx-background-radius: 4; -fx-cursor: hand; -fx-border-color: #2a2a2a; -fx-border-radius: 4; -fx-border-width: 0.5;";
    }

    private Label linkLabel(String text, String url) {
        Label l = new Label(text + " →");
        l.setStyle("-fx-text-fill: #555555; -fx-font-size: 12; -fx-font-family: 'JetBrains Mono'; -fx-cursor: hand;");
        l.setOnMouseClicked(e -> BrowserUtil.open(url));
        l.setOnMouseEntered(e -> l.setStyle("-fx-text-fill: #888888; -fx-font-size: 12; -fx-font-family: 'JetBrains Mono'; -fx-cursor: hand;"));
        l.setOnMouseExited(e  -> l.setStyle("-fx-text-fill: #555555; -fx-font-size: 12; -fx-font-family: 'JetBrains Mono'; -fx-cursor: hand;"));
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
        return "-fx-background-color: transparent; -fx-text-fill: #555555; " +
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
        return "-fx-background-color: #141414; -fx-text-fill: #666666; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
               "-fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
               "-fx-cursor: hand; -fx-padding: 8 16;";
    }
}
