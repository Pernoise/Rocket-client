package com.rocketclient;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CenterPanel extends VBox {

    public CenterPanel() {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(28, 36, 22, 36));
        setSpacing(6);

        Label name = new Label("Rocket Client");
        name.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 28; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-opacity: 0.88;");

        Label tagline = new Label("A Minecraft performance-focused client");
        tagline.setStyle("-fx-text-fill: #2e2e2e; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button playBtn = new Button("▶   Play");
        playBtn.setStyle(
            "-fx-background-color: #ffffff; -fx-text-fill: #000000; " +
            "-fx-font-size: 14; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono'; " +
            "-fx-background-radius: 7 0 0 7; -fx-cursor: hand; -fx-padding: 16 24;"
        );
        playBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(playBtn, Priority.ALWAYS);

        Button versionBtn = new Button("▾");
        versionBtn.setStyle(
            "-fx-background-color: #ffffff; -fx-text-fill: #000000; " +
            "-fx-font-size: 14; -fx-font-weight: bold; " +
            "-fx-background-radius: 0 7 7 0; -fx-cursor: hand; -fx-min-width: 40; -fx-padding: 16 10;"
        );

        HBox playRow = new HBox(1, playBtn, versionBtn);
        playRow.setMaxWidth(Double.MAX_VALUE);

        VBox versionList = new VBox(2);
        versionList.setStyle("-fx-background-color: #0d0d0d;");
        versionList.setPadding(new Insets(4));

        String[] versions = {
            "26.1.2 (latest)", "26.1.1", "26.1",
            "1.21.11", "1.21.10", "1.21.9", "1.21.8", "1.21.7",
            "1.21.6", "1.21.5", "1.21.4", "1.21.3", "1.21.2",
            "1.21.1", "1.21", "1.20.6", "1.20.5", "1.20.4",
            "1.20.3", "1.20.2", "1.20.1", "1.20", "1.19.4",
            "1.19.3", "1.19.2", "1.19.1", "1.19", "1.18.2",
            "1.18.1", "1.18", "1.17.1", "1.17", "1.16.5"
        };

        for (String v : versions) {
            Label vLabel = new Label(v);
            vLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-padding: 6 10;");
            vLabel.setMaxWidth(Double.MAX_VALUE);
            vLabel.setOnMouseEntered(e -> vLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-padding: 6 10; -fx-background-color: #161616; -fx-background-radius: 5;"));
            vLabel.setOnMouseExited(e  -> vLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono'; -fx-padding: 6 10;"));
            versionList.getChildren().add(vLabel);
        }

        ScrollPane scrollPane = new ScrollPane(versionList);
        scrollPane.setMaxHeight(160);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #0d0d0d; -fx-background-color: #0d0d0d; -fx-border-color: #1e1e1e; -fx-border-radius: 7; -fx-background-radius: 7;");
        scrollPane.setVisible(false);
        scrollPane.setManaged(false);

        versionBtn.setOnAction(e -> {
            boolean visible = scrollPane.isVisible();
            scrollPane.setVisible(!visible);
            scrollPane.setManaged(!visible);
        });

        getChildren().addAll(name, tagline, spacer, playRow, scrollPane);
    }
}
