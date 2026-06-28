package com.rocketclient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage stage) {
        Font.loadFont(getClass().getClassLoader().getResourceAsStream("fonts/JetBrainsMono-Regular.ttf"), 12);
        Font.loadFont(getClass().getClassLoader().getResourceAsStream("fonts/JetBrainsMono-Bold.ttf"), 12);

        AccountManager accountManager   = new AccountManager();
        SettingsManager settingsManager = new SettingsManager();
        settingsManager.load();

        SplashScreen splash = new SplashScreen(() -> {
            DiscordRPC.start(settingsManager);

            Label titleLabel = new Label("Rocket Client  BETA");
            titleLabel.setStyle("-fx-text-fill: #2a2a2a; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");

            Button minimizeBtn = new Button("-");
            minimizeBtn.setStyle(titleBtnStyle());
            minimizeBtn.setOnMouseEntered(e -> minimizeBtn.setStyle(titleBtnHoverStyle()));
            minimizeBtn.setOnMouseExited(e -> minimizeBtn.setStyle(titleBtnStyle()));
            minimizeBtn.setOnAction(e -> stage.setIconified(true));

            final boolean[] maximized = {false};
            Button maximizeBtn = new Button("[]");
            maximizeBtn.setStyle(titleBtnStyle());
            maximizeBtn.setOnMouseEntered(e -> maximizeBtn.setStyle(titleBtnHoverStyle()));
            maximizeBtn.setOnMouseExited(e -> maximizeBtn.setStyle(titleBtnStyle()));
            maximizeBtn.setOnAction(e -> {
                maximized[0] = !maximized[0];
                stage.setMaximized(maximized[0]);
                maximizeBtn.setText(maximized[0] ? "[-]" : "[]");
            });

            Button closeBtn = new Button("x");
            closeBtn.setStyle(titleBtnStyle());
            closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(closeBtnHoverStyle()));
            closeBtn.setOnMouseExited(e -> closeBtn.setStyle(titleBtnStyle()));
            closeBtn.setOnAction(e -> { DiscordRPC.stop(); Platform.exit(); });

            HBox spacer = new HBox();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox titleBar = new HBox(0, titleLabel, spacer, minimizeBtn, maximizeBtn, closeBtn);
            titleBar.setMaxWidth(Double.MAX_VALUE);
            titleBar.setAlignment(Pos.CENTER_LEFT);
            titleBar.setPadding(new Insets(6, 8, 6, 12));
            titleBar.setStyle("-fx-background-color: #080404; -fx-background-radius: 12 12 0 0;");

            titleBar.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
            titleBar.setOnMouseDragged(e -> {
                if (!stage.isMaximized()) {
                    stage.setX(e.getScreenX() - xOffset);
                    stage.setY(e.getScreenY() - yOffset);
                }
            });

            BorderPane root = new BorderPane();
            root.setStyle("-fx-background-color: #080404; -fx-font-family: 'JetBrains Mono'; -fx-background-radius: 0 0 12 12;");
            root.setLeft(new LeftPanel(accountManager, settingsManager));
            root.setCenter(new CenterPanel(accountManager, settingsManager));
            root.setRight(new NewsPanel());

            VBox wrapper = new VBox(0, titleBar, root);
            VBox.setVgrow(root, Priority.ALWAYS);
            wrapper.setStyle("-fx-background-color: #080404; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #1a1a1a; -fx-border-width: 1;");

            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(wrapper, 900, 540);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(540);

            try {
                Image icon = new Image(getClass().getClassLoader().getResourceAsStream("icons/rocket-launch.png"));
                stage.getIcons().add(icon);
            } catch (Exception e) {
                System.out.println("Could not load taskbar icon");
            }

            stage.show();

            if (FirstLaunchDialog.isFirstLaunch()) {
                Platform.runLater(() -> FirstLaunchDialog.show());
            }
        });

        splash.show();
    }

    private String titleBtnStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #333333; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 2 10; -fx-border-color: transparent;";
    }

    private String titleBtnHoverStyle() {
        return "-fx-background-color: #1a1a1a; -fx-text-fill: #888888; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 2 10; -fx-border-color: transparent;";
    }

    private String closeBtnHoverStyle() {
        return "-fx-background-color: #3a0000; -fx-text-fill: #ff4444; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 2 10; -fx-border-color: transparent;";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
