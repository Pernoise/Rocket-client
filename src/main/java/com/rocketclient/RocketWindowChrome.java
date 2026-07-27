package com.rocketclient;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Builds the same rounded, draggable, minimizable window chrome that Main.java
 * uses for the primary window, so popups (log, settings, login) look and behave
 * consistently instead of falling back to plain square UNDECORATED stages.
 */
public class RocketWindowChrome {

    /**
     * Wraps content in a themed window: rounded corners, draggable title bar,
     * minimize + close buttons. Caller still calls stage.show()/showAndWait().
     */
    public static void apply(Stage stage, String title, Region content, double width, double height, Runnable onClose) {
        stage.initStyle(StageStyle.TRANSPARENT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");

        Button minimizeBtn = new Button("-");
        minimizeBtn.setStyle(titleBtnStyle());
        minimizeBtn.setOnMouseEntered(e -> minimizeBtn.setStyle(titleBtnHoverStyle()));
        minimizeBtn.setOnMouseExited(e -> minimizeBtn.setStyle(titleBtnStyle()));
        minimizeBtn.setOnAction(e -> stage.setIconified(true));

        Button closeBtn = new Button("x");
        closeBtn.setStyle(titleBtnStyle());
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(closeBtnHoverStyle()));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(titleBtnStyle()));
        closeBtn.setOnAction(e -> {
            stage.close();
            if (onClose != null) onClose.run();
        });

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleBar = new HBox(0, titleLabel, spacer, minimizeBtn, closeBtn);
        titleBar.setMaxWidth(Double.MAX_VALUE);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(6, 8, 6, 12));
        titleBar.setStyle("-fx-background-color: #080404; -fx-background-radius: 12 12 0 0;");

        double[] offset = new double[2];
        titleBar.setOnMousePressed(e -> { offset[0] = e.getSceneX(); offset[1] = e.getSceneY(); });
        titleBar.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - offset[0]);
            stage.setY(e.getScreenY() - offset[1]);
        });

        content.setStyle(content.getStyle() + "; -fx-background-radius: 0 0 12 12;");

        VBox wrapper = new VBox(0, titleBar, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        wrapper.setStyle("-fx-background-color: #080404; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #1a1a1a; -fx-border-width: 1;");

        Scene scene = new Scene(wrapper, width, height);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(RocketWindowChrome.class.getClassLoader().getResource("css/theme.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(width);
        stage.setMinHeight(height);
    }

    private static String titleBtnStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 2 10; -fx-border-color: transparent;";
    }

    private static String titleBtnHoverStyle() {
        return "-fx-background-color: #1a1a1a; -fx-text-fill: #ffffff; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 2 10; -fx-border-color: transparent;";
    }

    private static String closeBtnHoverStyle() {
        return "-fx-background-color: #3a0000; -fx-text-fill: #ff4444; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 2 10; -fx-border-color: transparent;";
    }
}
