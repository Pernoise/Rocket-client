package com.rocketclient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LeftPanel extends VBox {

    private final AccountManager accountManager;
    private final SettingsManager settingsManager;
    private VBox accountWidget = new VBox(4);

    public LeftPanel(AccountManager accountManager, SettingsManager settingsManager) {
        this.accountManager  = accountManager;
        this.settingsManager = settingsManager;

        setPrefWidth(68);
        setStyle("-fx-background-color: #0f0f0f; -fx-border-color: #1a1a1a; -fx-border-width: 0 1 0 0; -fx-background-radius: 0 0 0 12; -fx-border-radius: 0 0 0 12;");
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(14, 0, 14, 0));
        setSpacing(6);

        VBox logo     = createIcon("icons/rocket-launch.png", "Rocket Client", true,  null,  false, false);
        VBox account  = createIcon("icons/user.png",          "Account",       false, null,  true,  false);
        VBox settings = createIcon("icons/gear.png",          "Settings",      false, null,  false, true);
        VBox launcherFolder = createIcon("icons/folder.png",  "Open Launcher Folder", false, null, false, false);
        launcherFolder.setOnMouseClicked(e -> openLauncherFolder());

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        accountWidget.setAlignment(Pos.CENTER);
        refreshAccountWidget();

        VBox discord = createIcon("icons/discord-logo.png", "Discord", false, "https://discord.com/invite/urHfdFdsbh", false, false);
        VBox website = createIcon("icons/globe.png", "Website", false, "https://rocketclient.rocketclient.abrdns.com/#home", false, false);
        getChildren().addAll(logo, account, settings, launcherFolder, spacer, accountWidget, discord, website);

    }

    public void refreshAccountWidget() {
        accountWidget.getChildren().clear();
        AccountManager.Account acc = accountManager.getSelected();
        if (acc == null) return;

        javafx.scene.layout.StackPane avatarBox = new javafx.scene.layout.StackPane();
        avatarBox.setPrefSize(34, 34);
        avatarBox.setMaxSize(34, 34);
        avatarBox.setStyle("-fx-background-color: #161616; -fx-background-radius: 8;");

        try {
            String avatarUrl = "https://skins.manacube.com/avatars/" + acc.uuid;
            Image avatar = new Image(avatarUrl, true);
            ImageView iv = new ImageView(avatar);
            iv.setFitWidth(28);
            iv.setFitHeight(28);
            iv.setPreserveRatio(true);

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(28, 28);
            clip.setArcWidth(10);
            clip.setArcHeight(10);
            iv.setClip(clip);

            avatar.errorProperty().addListener((obs, was, isError) -> {
                if (isError) {
                    Platform.runLater(() -> {
                        avatarBox.getChildren().clear();
                        avatarBox.getChildren().add(fallbackAvatarLabel());
                    });
                }
            });

            avatarBox.getChildren().add(iv);
        } catch (Exception e) {
            System.out.println("Could not load avatar: " + e.getMessage());
            avatarBox.getChildren().add(fallbackAvatarLabel());
        }

        Label nameLabel = new Label(acc.username);
        nameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 9; -fx-font-family: 'JetBrains Mono';");

        Platform.runLater(() -> accountWidget.getChildren().addAll(avatarBox, nameLabel));
    }

    private Label fallbackAvatarLabel() {
        Label fallback = new Label("?");
        fallback.setStyle("-fx-text-fill: #555555; -fx-font-size: 14; -fx-font-family: 'JetBrains Mono';");
        return fallback;
    }

    private VBox createIcon(String resourcePath, String tooltip, boolean isLogo, String url, boolean isAuth, boolean isSettings) {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(34, 34);
        box.setMaxSize(34, 34);

        String baseStyle = isLogo
            ? "-fx-background-color: #1a1a1a; -fx-background-radius: 8; -fx-border-color: #2a2a2a; -fx-border-radius: 8; -fx-border-width: 0.5;"
            : "-fx-background-color: #161616; -fx-background-radius: 8;";
        box.setStyle(baseStyle);

        try {
            Image img = new Image(getClass().getClassLoader().getResourceAsStream(resourcePath));
            ImageView iv = new ImageView(img);
            iv.setFitWidth(isLogo ? 18 : 16);
            iv.setFitHeight(isLogo ? 18 : 16);
            iv.setPreserveRatio(true);
            box.getChildren().add(iv);
        } catch (Exception e) {
            System.out.println("Could not load icon: " + resourcePath);
        }

        Tooltip.install(box, new Tooltip(tooltip));

        if (isLogo == false) {
            box.setOnMouseEntered(e -> box.setStyle("-fx-background-color: #222222; -fx-background-radius: 8;"));
            box.setOnMouseExited(e  -> box.setStyle("-fx-background-color: #161616; -fx-background-radius: 8;"));

            if (isAuth) {
                box.setOnMouseClicked(e -> openAuthPanel());
            } else if (isSettings) {
                box.setOnMouseClicked(e -> openSettingsPanel());
            } else if (url != null) {
                box.setOnMouseClicked(e -> BrowserUtil.open(url));
            }
        }

        return box;
    }

    private void openAuthPanel() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);

        AuthPanel authPanel = new AuthPanel(accountManager);
        authPanel.setStyle("-fx-background-color: #0f0f0f;");

        RocketWindowChrome.apply(popup, "LOGIN", authPanel, 400, 500, this::refreshAccountWidget);
        popup.centerOnScreen();
        popup.showAndWait();
    }

    private void openSettingsPanel() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);

        SettingsPanel settingsPanel = new SettingsPanel(settingsManager);
        settingsPanel.setStyle("-fx-background-color: #0f0f0f;");

        RocketWindowChrome.apply(popup, "SETTINGS", settingsPanel, 520, 580, null);
        popup.centerOnScreen();
        popup.showAndWait();
    }

    private void openLauncherFolder() {
        try {
            Path path = Paths.get(System.getProperty("user.home"), ".rocketclient");
            if (Files.exists(path) == false) {
                Files.createDirectories(path);
            }

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
            System.out.println("Could not open launcher folder: " + e.getMessage());
        }
    }
}
