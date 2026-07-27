package com.rocketclient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LaunchLogWindow {

    private final Stage stage;
    private final TextArea logArea;

    public LaunchLogWindow() {
        stage = new Stage();

        VBox root = new VBox(12);
        root.setStyle("-fx-background-color: #080404;");
        root.setPadding(new Insets(20));

        logArea = new TextArea();
        logArea.getStyleClass().add("rocket-scroll");
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
            "-fx-background-color: #141414; -fx-text-fill: #ffffff; " +
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
            "-fx-background-color: #141414; -fx-text-fill: #ffffff; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
            "-fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-padding: 8 16;"
        );
        closeBtn.setOnAction(e -> stage.close());

        HBox btnRow = new HBox(8, copyBtn, closeBtn);

        root.getChildren().addAll(logArea, btnRow);

        RocketWindowChrome.apply(stage, "LAUNCH LOG", root, 640, 420, null);
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
