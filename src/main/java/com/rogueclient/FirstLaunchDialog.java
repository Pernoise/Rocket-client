package com.rogueclient;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.file.*;

public class FirstLaunchDialog {

    private static final Path FLAG_FILE = Paths.get(
        System.getProperty("user.home"), ".rogueclient", "launched.flag"
    );

    public static boolean isFirstLaunch() {
        return !Files.exists(FLAG_FILE);
    }

    public static void markLaunched() {
        try {
            Files.createDirectories(FLAG_FILE.getParent());
            Files.createFile(FLAG_FILE);
        } catch (Exception ignored) {}
    }

    public static void show() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40, 48, 32, 48));
        root.setStyle(
            "-fx-background-color: #080404;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #1a1a1a;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;"
        );

        // Logo
        try {
            Image img = new Image(
                FirstLaunchDialog.class.getClassLoader()
                    .getResourceAsStream("images/icon.png")
            );

            ImageView iv = new ImageView(img);
            iv.setFitWidth(40);
            iv.setFitHeight(40);
            iv.setPreserveRatio(true);

            root.getChildren().add(iv);
        } catch (Exception ignored) {}

        Label title = new Label("Welcome to Rogue Client");
        title.setStyle(
            "-fx-text-fill: #ffffff;" +
            "-fx-font-size: 16;" +
            "-fx-font-family: 'JetBrains Mono';" +
            "-fx-font-weight: bold;" +
            "-fx-opacity: 0.88;"
        );

        Label msg = new Label(
            "A lightweight, custom Minecraft launcher.\n\n" +
            "Join the community or check out the site below."
        );

        msg.setStyle(
            "-fx-text-fill: #ffffff;" +
            "-fx-font-size: 12;" +
            "-fx-font-family: 'JetBrains Mono';" +
            "-fx-text-alignment: center;" +
            "-fx-opacity: 0.8;"
        );

        msg.setWrapText(true);
        msg.setMaxWidth(320);

        HBox socialRow = new HBox(14);
        socialRow.setAlignment(Pos.CENTER);

        socialRow.getChildren().addAll(
            socialIcon(
                "icons/discord-logo.png",
                "Discord",
                "https://discord.com/invite/urHfdFdsbh"
            ),
            socialIcon(
                "icons/Logo.ico",
                "Website",
                "https://rogue.pernoise.workers.dev/"
            )
        );

        Button btn = new Button("Let's go");
        btn.setStyle(
            "-fx-background-color: #ffffff;" +
            "-fx-text-fill: #000000;" +
            "-fx-font-family: 'JetBrains Mono';" +
            "-fx-font-size: 13;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 7;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 10 40;"
        );

        btn.setOnAction(e -> {
            markLaunched();
            stage.close();
        });

        root.getChildren().addAll(title, msg, socialRow, btn);

        Scene scene = new Scene(root, 420, 300);
        scene.setFill(Color.TRANSPARENT);

        stage.setScene(scene);
        stage.centerOnScreen();
        stage.showAndWait();
    }

    private static VBox socialIcon(String iconPath, String name, String url) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-cursor: hand;");

        try {
            Image img = new Image(
                FirstLaunchDialog.class.getClassLoader()
                    .getResourceAsStream(iconPath)
            );

            ImageView icon = new ImageView(img);
            icon.setFitWidth(28);
            icon.setFitHeight(28);
            icon.setPreserveRatio(true);

            Label label = new Label(name);
            label.setStyle(
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 10;" +
                "-fx-font-family: 'JetBrains Mono';"
            );

            box.getChildren().addAll(icon, label);

            box.setOnMouseClicked(e ->
                BrowserUtil.open(url)
            );

        } catch (Exception ignored) {}

        return box;
    }
}
