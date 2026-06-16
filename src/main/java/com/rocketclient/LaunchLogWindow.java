package com.rocketclient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LaunchLogWindow {

    private final Stage stage;
    private final TextArea logArea;

    public LaunchLogWindow() {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setMinWidth(640);
        stage.setMinHeight(400);

        VBox root = new VBox(12);
        root.setStyle(
            "-fx-background-color: #080404; " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-border-color: #1a1a1a; " +
            "-fx-border-width: 1;"
        );
        root.setPadding(new Insets(20));

        Label title = new Label("LAUNCH LOG");
        title.setStyle("-fx-text-fill: #2a2a2a; -fx-font-size: 9; -fx-font-family: 'JetBrains Mono';");

        logArea = new TextArea();
        logArea.setStyle(
            "-fx-background-color: #080404; -fx-text-fill: #ffffff; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 11; " +
            "-fx-border-color: #1a1a1a; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-control-inner-background: #080404;"
        );
        logArea.setEditable(false);
        logArea.setWrapText(true);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        Button copyBtn = new Button("Copy to Clipboard");
        copyBtn.setStyle(
            "-fx-background-color: #141414; -fx-text-fill: #666666; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
            "-fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-padding: 8 16;"
        );
        copyBtn.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(logArea.getText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            copyBtn.setText("Copied!");
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle(
            "-fx-background-color: #141414; -fx-text-fill: #666666; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
            "-fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-padding: 8 16;"
        );
        closeBtn.setOnAction(e -> stage.close());

        HBox btnRow = new HBox(8, copyBtn, closeBtn);

        root.getChildren().addAll(title, logArea, btnRow);

        Scene scene = new Scene(root, 640, 420);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
    }

    public void show() {
        Platform.runLater(() -> stage.show());
    }

    public void appendLog(String line) {
        Platform.runLater(() -> logArea.appendText(line + "\n"));
    }

    public void setTitle(String title) {
        Platform.runLater(() -> stage.setTitle(title));
    }
}
