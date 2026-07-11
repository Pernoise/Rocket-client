package com.rocketclient;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class SplashScreen {

    private final Stage stage;
    private final Runnable onFinished;

    public SplashScreen(Runnable onFinished) {
        this.onFinished = onFinished;
        this.stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60, 80, 32, 80));
        root.setStyle(
            "-fx-background-color: #080404;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #1a1a1a;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;"
        );

        ImageView logo = new ImageView();
        try {
            Image img = new Image(getClass().getClassLoader().getResourceAsStream("Images/icon.png"));
            logo.setImage(img);
            logo.setFitWidth(48);
            logo.setFitHeight(48);
            logo.setPreserveRatio(true);
        } catch (Exception e) {
            System.out.println("Could not load splash logo");
        }

        Label title = new Label("Rocket Client");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 22; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-opacity: 0.88;");

        Label version = new Label("Beta v0.6");
        version.setStyle("-fx-text-fill: #2a2a2a; -fx-font-size: 10; -fx-font-family: 'JetBrains Mono';");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(260);
        progressBar.setPrefHeight(2);
        progressBar.setStyle("-fx-accent: #ffffff; -fx-background-color: #111111; -fx-background-radius: 2; -fx-border-radius: 2;");

        Label status = new Label("Initializing...");
        status.setStyle("-fx-text-fill: #2e2e2e; -fx-font-size: 10; -fx-font-family: 'JetBrains Mono';");

        Label website = new Label("rocketclient.rocketclient.abrdns.com");
        website.setStyle("-fx-text-fill: #1a1a1a; -fx-font-size: 9; -fx-font-family: 'JetBrains Mono';");

        root.getChildren().addAll(logo, title, version, progressBar, status, website);

        Scene scene = new Scene(root, 480, 220);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        try {
            Image icon = new Image(getClass().getClassLoader().getResourceAsStream("icons/rocket-launch.png"));
            stage.getIcons().add(icon);
        } catch (Exception ignored) {}

        stage.centerOnScreen();

        String[][] steps = {
            {"0.10", "Initializing..."},
            {"0.30", "Loading assets..."},
            {"0.55", "Checking for updates..."},
            {"0.75", "Preparing launcher..."},
            {"0.95", "Almost ready..."},
            {"1.00", "Welcome back."}
        };

        Timeline timeline = new Timeline();
        for (int i = 0; i < steps.length; i++) {
            final double progress = Double.parseDouble(steps[i][0]);
            final String text     = steps[i][1];
            final boolean last    = (i == steps.length - 1);

            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(600 * (i + 1)), e -> {
                progressBar.setProgress(progress);
                status.setText(text);
                if (last) {
                    new Timeline(new KeyFrame(Duration.millis(400), ev -> {
                        stage.close();
                        onFinished.run();
                    })).play();
                }
            }));
        }

        stage.setOnShown(e -> timeline.play());
    }

    public void show() {
        stage.show();
    }
}
