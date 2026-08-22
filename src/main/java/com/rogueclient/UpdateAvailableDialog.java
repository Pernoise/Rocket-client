package com.rogueclient;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class UpdateAvailableDialog {

    public static void show(UpdateManager.UpdateInfo info) {
        Stage stage = new Stage();

        VBox root = new VBox(14);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28, 32, 24, 32));
        root.setStyle("-fx-background-color: #080404;");

        Label title = new Label("Update available");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 15; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold;");

        Label msg = new Label(
            "Rogue Client v" + info.latestVersion + " is out — you're on v" + AppVersion.CURRENT + ".\n" +
            "Grab the new version from the website."
        );
        msg.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12; -fx-font-family: 'JetBrains Mono'; -fx-opacity: 0.8; -fx-text-alignment: center;");
        msg.setWrapText(true);
        msg.setMaxWidth(320);

        Button downloadBtn = new Button("Open Website");
        downloadBtn.setStyle(
            "-fx-background-color: #ffffff; -fx-text-fill: #000000; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-font-weight: bold; " +
            "-fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 9 28;"
        );
        downloadBtn.setOnAction(e -> {
            BrowserUtil.open(info.websiteUrl);
            stage.close();
        });

        Button laterBtn = new Button("Later");
        laterBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #888888; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 6;"
        );
        laterBtn.setOnAction(e -> stage.close());

        root.getChildren().addAll(title, msg, downloadBtn, laterBtn);

        RogueWindowChrome.apply(stage, "UPDATE AVAILABLE", root, 400, 260, null);
        stage.centerOnScreen();
        stage.show();
    }
}
