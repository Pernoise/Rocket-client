package com.rocketclient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Font.loadFont(getClass().getClassLoader().getResourceAsStream("fonts/JetBrainsMono-Regular.ttf"), 12);
        Font.loadFont(getClass().getClassLoader().getResourceAsStream("fonts/JetBrainsMono-Bold.ttf"), 12);

        AccountManager accountManager   = new AccountManager();
        SettingsManager settingsManager = new SettingsManager();
        settingsManager.load();

        SplashScreen splash = new SplashScreen(() -> {
            DiscordRPC.start(settingsManager);

            BorderPane root = new BorderPane();
            root.setStyle("-fx-background-color: #080404; -fx-font-family: 'JetBrains Mono';");

            root.setLeft(new LeftPanel(accountManager, settingsManager));
            root.setCenter(new CenterPanel(accountManager, settingsManager));
            root.setRight(new NewsPanel());

            Scene scene = new Scene(root, 900, 540);
            stage.setTitle("Rocket Client — BETA");
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

            stage.setOnCloseRequest(e -> DiscordRPC.stop());
            stage.show();

            if (FirstLaunchDialog.isFirstLaunch()) {
                Platform.runLater(() -> FirstLaunchDialog.show());
            }
        });

        splash.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
