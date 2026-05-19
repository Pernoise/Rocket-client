package com.rocketclient;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LeftPanel extends VBox {

    private final AccountManager accountManager;
    private final SettingsManager settingsManager;

    public LeftPanel(AccountManager accountManager, SettingsManager settingsManager) {
        this.accountManager  = accountManager;
        this.settingsManager = settingsManager;

        setPrefWidth(56);
        setStyle("-fx-background-color: #0f0f0f; -fx-border-color: #1a1a1a; -fx-border-width: 0 1 0 0;");
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(14, 0, 14, 0));
        setSpacing(6);

        VBox logo     = createIcon("icons/rocket-launch.png", "Rocket Client", true,  null,  false, false);
        VBox account  = createIcon("icons/user.png",          "Account",       false, null,  true,  false);
        VBox store    = createIcon("icons/handbag-simple.png","Store — Coming Soon", false, null, false, false);
        VBox settings = createIcon("icons/gear.png",          "Settings",      false, null,  false, true);

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox discord = createIcon("icons/discord-logo.png", "Discord", false, "https://discord.com/invite/urHfdFdsbh", false, false);
        VBox website = createIcon("icons/globe.png", "Website", false, "https://rocketclient.rocketclient.abrdns.com/#home", false, false);

        getChildren().addAll(logo, account, store, settings, spacer, discord, website);
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
        popup.initStyle(StageStyle.UNDECORATED);

        AuthPanel authPanel = new AuthPanel(accountManager);

        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("✕");
        closeBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #444444; " +
            "-fx-font-size: 13; -fx-cursor: hand; -fx-border-color: transparent; " +
            "-fx-padding: 2 6;"
        );
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #888888; " +
            "-fx-font-size: 13; -fx-cursor: hand; -fx-border-color: transparent; " +
            "-fx-padding: 2 6;"
        ));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #444444; " +
            "-fx-font-size: 13; -fx-cursor: hand; -fx-border-color: transparent; " +
            "-fx-padding: 2 6;"
        ));
        closeBtn.setOnAction(e -> popup.close());

        javafx.scene.layout.HBox topBar = new javafx.scene.layout.HBox(closeBtn);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(6, 6, 0, 6));
        topBar.setStyle("-fx-background-color: #0f0f0f;");

        VBox root = new VBox(topBar, authPanel);
        root.setStyle("-fx-background-color: #0f0f0f; -fx-border-color: #1a1a1a; -fx-border-width: 1;");

        Scene scene = new Scene(root, 400, 500);
        popup.setScene(scene);
        popup.centerOnScreen();
        popup.showAndWait();
    }

    private void openSettingsPanel() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);

        SettingsPanel settingsPanel = new SettingsPanel(settingsManager);

        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("✕");
        closeBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #444444; " +
            "-fx-font-size: 13; -fx-cursor: hand; -fx-border-color: transparent; " +
            "-fx-padding: 2 6;"
        );
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #888888; " +
            "-fx-font-size: 13; -fx-cursor: hand; -fx-border-color: transparent; " +
            "-fx-padding: 2 6;"
        ));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #444444; " +
            "-fx-font-size: 13; -fx-cursor: hand; -fx-border-color: transparent; " +
            "-fx-padding: 2 6;"
        ));
        closeBtn.setOnAction(e -> popup.close());

        javafx.scene.layout.HBox topBar = new javafx.scene.layout.HBox(closeBtn);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(6, 6, 0, 6));
        topBar.setStyle("-fx-background-color: #0f0f0f;");

        VBox root = new VBox(topBar, settingsPanel);
        root.setStyle("-fx-background-color: #0f0f0f; -fx-border-color: #1a1a1a; -fx-border-width: 1;");

        Scene scene = new Scene(root, 520, 580);
        popup.setScene(scene);
        popup.centerOnScreen();
        popup.showAndWait();
    }
}
