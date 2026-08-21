package com.rogueclient;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        ramSlider.getStyleClass().add("rogue-slider");
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
            panel.getChildren().add(toggleRow("Launch Rogue Client on system startup", settings.launchOnStartup, val -> {
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

        Label version = new Label("Rogue Client — v1.0");
        version.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-opacity: 0.88;");

        Label desc = new Label("A modern, lightweight Minecraft launcher built with love and.. Java");
        desc.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        desc.setWrapText(true);

        panel.getChildren().add(sectionLabel("Info"));
        panel.getChildren().addAll(version, desc);
        panel.getChildren().add(sectionLabel("Links"));
        panel.getChildren().add(linkLabel("Discord", "https://discord.com/invite/urHfdFdsbh"));
        panel.getChildren().add(linkLabel("Website", "https://rogueclient.rogueclient.abrdns.com"));
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
                    java.net.URL url = new java.net.URL("https://api.github.com/repos/Pernoise/Rogue-client/releases/latest");
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

    private static final String NUKE_IDLE_TEXT = "Clear RogueClient data";

    private VBox buildDevtoolsPanel() {

        VBox panel = new VBox(20);
        panel.setPadding(new Insets(16, 0, 0, 0));

        // ---- Instance backup ----
        panel.getChildren().add(sectionLabel("Instance Backup"));

        Label backupDesc = new Label("Compress every installed instance into a single zip archive.");
        backupDesc.setWrapText(true);
        backupDesc.setStyle("-fx-text-fill: #7a7a7a; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        panel.getChildren().add(backupDesc);

        HBox backupRow = new HBox(12);
        backupRow.setAlignment(Pos.CENTER_LEFT);

        Button backupBtn = new Button("Backup Instances");
        backupBtn.setStyle(secondaryBtnStyle());

        Label backupStatus = statusLabel();

        backupBtn.setOnAction(e -> startInstanceBackup(backupBtn, backupStatus));

        backupRow.getChildren().addAll(backupBtn, backupStatus);
        panel.getChildren().add(backupRow);

        // ---- Danger zone ----
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #1a1a1a;");
        panel.getChildren().add(divider);

        panel.getChildren().add(sectionLabel("Danger Zone"));

        Label nukeDesc = new Label("Permanently deletes your entire RogueClient data folder, including instances, accounts and settings.");
        nukeDesc.setWrapText(true);
        nukeDesc.setStyle("-fx-text-fill: #7a7a7a; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        panel.getChildren().add(nukeDesc);

        HBox nukeRow = new HBox(12);
        nukeRow.setAlignment(Pos.CENTER_LEFT);

        Button clearLocalDataBtn = new Button(NUKE_IDLE_TEXT);
        clearLocalDataBtn.setStyle(secondaryBtnStyle() + " -fx-background-color: #2E0000;");

        Label nukeStatus = statusLabel();

        clearLocalDataBtn.setOnAction(e -> {

            switch (clearLocalDataBtn.getText()) {
                case NUKE_IDLE_TEXT:
                    ChangeText(clearLocalDataBtn, "Are you sure?", false);
                    return;
                case "Are you sure?":
                    ChangeText(clearLocalDataBtn, "Are you really sure?", false);
                    return;
                case "Are you really sure?":
                    ChangeText(clearLocalDataBtn, "Are you really actually sure?", false);
                    return;
                case "Are you really actually sure?":
                    ChangeText(clearLocalDataBtn, "Are you really actually extremely sure?", false);
                    return;
                case "Are you really actually extremely sure?":
                    ChangeText(clearLocalDataBtn, "One last click for good luck?", false);
                    return;
                case "One last click for good luck?":
                    break;
                default:
                    ChangeText(clearLocalDataBtn, NUKE_IDLE_TEXT, false);
                    return;
            }

            Path path = Paths.get(System.getProperty("user.home"), ".rogueclient");

            if (Files.exists(path)) {
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
                    showStatus(nukeStatus, "Success", true);
                } catch (IOException | UncheckedIOException ex) {
                    System.out.println("File deletion failed! " + ex.getMessage());
                    showStatus(nukeStatus, "Failed", false);
                }
            } else {
                showStatus(nukeStatus, "Nothing to clear", false);
            }

            ChangeText(clearLocalDataBtn, NUKE_IDLE_TEXT, false);
        });

        nukeRow.getChildren().addAll(clearLocalDataBtn, nukeStatus);
        panel.getChildren().add(nukeRow);

        return panel;
    }

    /** Small pill of text used to report the outcome of a devtools action next to its button. Starts invisible. */
    private Label statusLabel() {
        Label label = new Label();
        label.setOpacity(0);
        label.setStyle("-fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold;");
        return label;
    }

    /** Shows a status message next to a devtools button, then fades it back out after a couple seconds. */
    private void showStatus(Label label, String text, boolean success) {
        label.setText(text);
        label.setStyle("-fx-text-fill: " + (success ? "#3ddc84" : "#e0605a") +
                "; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold;");
        label.setOpacity(1);

        FadeTransition fade = new FadeTransition(Duration.seconds(0.5), label);
        fade.setDelay(Duration.seconds(2.5));
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.play();
    }

    /**
     * Zips every instance under ~/.rogueclient/instances into a single archive, shown in a small
     * modal progress window that the user is warned not to close. The window has a cancel button
     * that aborts the compression job and deletes whatever partial archive had been written so far.
     * Once compression finishes successfully, the user is asked where the finished zip should go.
     */
    private void startInstanceBackup(Button backupBtn, Label statusLabel) {
        Path instancesDir = Paths.get(System.getProperty("user.home"), ".rogueclient", "instances");

        if (!Files.exists(instancesDir)) {
            showStatus(statusLabel, "No instances found", false);
            return;
        }

        final Path tempZip;
        try {
            tempZip = Files.createTempFile("rogueclient-backup-", ".zip");
        } catch (IOException ex) {
            showStatus(statusLabel, "Couldn't start backup", false);
            return;
        }

        // --- progress window ---
        Stage progressStage = new Stage();
        progressStage.initStyle(StageStyle.UNDECORATED);
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setResizable(false);
        progressStage.setTitle("Backing up instances");

        VBox root = new VBox(14);
        root.setStyle("-fx-background-color: #0f0f0f; -fx-border-color: #1a1a1a; -fx-border-width: 1;");
        root.setPadding(new Insets(20));
        root.setPrefWidth(360);

        Label title = new Label("Backing up instances");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold;");

        Label fileLabel = new Label("Preparing...");
        fileLabel.setStyle("-fx-text-fill: #7a7a7a; -fx-font-size: 10; -fx-font-family: 'JetBrains Mono';");
        fileLabel.setWrapText(true);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #ffffff;");

        Label percentLabel = new Label("0%");
        percentLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");

        Label warningLabel = new Label("Do not close this window while the backup is running.");
        warningLabel.setWrapText(true);
        warningLabel.setStyle("-fx-text-fill: #d9a441; -fx-font-size: 10; -fx-font-family: 'JetBrains Mono';");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(secondaryBtnStyle() + " -fx-background-color: #2E0000;");

        HBox btnRow = new HBox(cancelBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, fileLabel, progressBar, percentLabel, warningLabel, btnRow);
        progressStage.setScene(new Scene(root));

        // Undecorated + consuming the close request means the only way out is the Cancel button.
        progressStage.setOnCloseRequest(javafx.event.Event::consume);

        // --- background zip task ---
        Task<Void> backupTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<Path> files;
                try (Stream<Path> walk = Files.walk(instancesDir)) {
                    files = walk.filter(Files::isRegularFile).toList();
                }

                long total = Math.max(files.size(), 1);
                long done = 0;

                try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempZip))) {
                    for (Path file : files) {
                        if (isCancelled()) {
                            break;
                        }
                        String entryName = instancesDir.relativize(file).toString().replace('\\', '/');
                        updateMessage(entryName);
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(file, zos);
                        zos.closeEntry();
                        done++;
                        updateProgress(done, total);
                    }
                }

                if (isCancelled()) {
                    Files.deleteIfExists(tempZip);
                }
                return null;
            }
        };

        progressBar.progressProperty().bind(backupTask.progressProperty());
        fileLabel.textProperty().bind(backupTask.messageProperty());
        backupTask.progressProperty().addListener((obs, oldVal, newVal) -> {
            int pct = (int) Math.round(Math.max(newVal.doubleValue(), 0) * 100);
            percentLabel.setText(pct + "%");
        });

        cancelBtn.setOnAction(ev -> {
            cancelBtn.setDisable(true);
            warningLabel.setText("Cancelling...");
            backupTask.cancel(true);
        });

        backupTask.setOnSucceeded(ev -> {
            progressStage.close();
            promptBackupDestination(tempZip, statusLabel);
        });

        backupTask.setOnCancelled(ev -> {
            progressStage.close();
            showStatus(statusLabel, "Backup cancelled", false);
        });

        backupTask.setOnFailed(ev -> {
            progressStage.close();
            try {
                Files.deleteIfExists(tempZip);
            } catch (IOException ignored) {
            }
            showStatus(statusLabel, "Backup failed", false);
        });

        Thread worker = new Thread(backupTask, "instance-backup");
        worker.setDaemon(true);
        worker.start();

        progressStage.show();
    }

    /** Asks the user where the finished backup zip should be moved to once compression completes. */
    private void promptBackupDestination(Path tempZip, Label statusLabel) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose a folder to save the backup in");

        java.io.File destDir = chooser.showDialog(null);
        if (destDir == null) {
            try {
                Files.deleteIfExists(tempZip);
            } catch (IOException ignored) {
            }
            showStatus(statusLabel, "Backup discarded", false);
            return;
        }

        try {
            String filename = "rogueclient-instances-" + System.currentTimeMillis() + ".zip";
            Path destination = destDir.toPath().resolve(filename);
            Files.move(tempZip, destination);
            showStatus(statusLabel, "Success", true);
        } catch (IOException ex) {
            System.out.println("Failed to move backup archive: " + ex.getMessage());
            showStatus(statusLabel, "Failed to save backup", false);
        }
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
