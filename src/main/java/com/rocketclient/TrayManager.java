package com.rocketclient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Lets the launcher minimize to the OS system tray instead of exiting when
 * Minecraft launches or the window is closed, if "Hide launcher" is enabled.
 * Without this, the launcher previously force-exited (Platform.exit() +
 * System.exit(0)) once the game process ended, which is why it "closed" -
 * that was a workaround for memory buildup, not the intended behavior.
 *
 * Uses a custom JavaFX Popup instead of java.awt.PopupMenu for the tray
 * click menu, since the native AWT popup can't be themed at all (it's
 * rendered entirely by the OS with no styling hooks).
 */
public class TrayManager {

    private static Stage stage;
    private static TrayIcon trayIcon;
    private static boolean initialized = false;
    private static Popup trayMenu;

    public static void init(Stage primaryStage) {
        stage = primaryStage;
        if (initialized || !SystemTray.isSupported()) return;

        try {
            Platform.setImplicitExit(false);

            java.awt.Image icon = Toolkit.getDefaultToolkit().createImage(
                TrayManager.class.getClassLoader().getResource("icons/rocket-launch.png")
            );

            trayIcon = new TrayIcon(icon, "Rocket Client");
            trayIcon.setImageAutoSize(true);
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        Platform.runLater(TrayManager::restoreFromTray);
                    } else {
                        Platform.runLater(() -> showTrayMenu(e.getXOnScreen(), e.getYOnScreen()));
                    }
                }
            });

            SystemTray.getSystemTray().add(trayIcon);
            initialized = true;
        } catch (AWTException e) {
            System.out.println("Could not initialize system tray: " + e.getMessage());
        }
    }

    private static void showTrayMenu(double screenX, double screenY) {
        if (trayMenu != null && trayMenu.isShowing()) {
            trayMenu.hide();
            return;
        }

        Button showBtn = new Button("Show Rocket Client");
        showBtn.setStyle(trayMenuItemStyle());
        showBtn.setMaxWidth(Double.MAX_VALUE);
        showBtn.setOnMouseEntered(e -> showBtn.setStyle(trayMenuItemHoverStyle()));
        showBtn.setOnMouseExited(e -> showBtn.setStyle(trayMenuItemStyle()));
        showBtn.setOnAction(e -> {
            trayMenu.hide();
            restoreFromTray();
        });

        Button quitBtn = new Button("Quit");
        quitBtn.setStyle(trayMenuItemStyle());
        quitBtn.setMaxWidth(Double.MAX_VALUE);
        quitBtn.setOnMouseEntered(e -> quitBtn.setStyle(trayMenuItemHoverStyle(true)));
        quitBtn.setOnMouseExited(e -> quitBtn.setStyle(trayMenuItemStyle()));
        quitBtn.setOnAction(e -> {
            trayMenu.hide();
            quit();
        });

        VBox box = new VBox(2, showBtn, quitBtn);
        box.setPadding(new Insets(4));
        box.setStyle(
            "-fx-background-color: #0f0f0f; -fx-background-radius: 8; " +
            "-fx-border-color: #1a1a1a; -fx-border-radius: 8; -fx-border-width: 1;"
        );

        trayMenu = new Popup();
        trayMenu.setAutoHide(true);
        trayMenu.getContent().add(box);
        trayMenu.setX(screenX - 90);
        trayMenu.setY(screenY - 90);
        trayMenu.show(stage);
    }

    private static String trayMenuItemStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #ffffff; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
            "-fx-cursor: hand; -fx-padding: 8 14; -fx-alignment: CENTER_LEFT; " +
            "-fx-background-radius: 6;";
    }

    private static String trayMenuItemHoverStyle() {
        return trayMenuItemHoverStyle(false);
    }

    private static String trayMenuItemHoverStyle(boolean destructive) {
        String bg = destructive ? "#3a0000" : "#1a1a1a";
        String fg = destructive ? "#ff4444" : "#ffffff";
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
            "-fx-cursor: hand; -fx-padding: 8 14; -fx-alignment: CENTER_LEFT; " +
            "-fx-background-radius: 6;";
    }

    public static void minimizeToTray() {
        if (!initialized) {
            Platform.runLater(() -> stage.setIconified(true));
            return;
        }
        Platform.runLater(stage::hide);
    }

    public static void restoreFromTray() {
        if (stage == null) return;
        Platform.runLater(() -> {
            stage.show();
            stage.setIconified(false);
            stage.toFront();
        });
    }

    public static void quit() {
        try {
            DiscordRPC.stop();
        } catch (Exception ignored) {}
        if (initialized && trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        Platform.exit();
        System.exit(0);
    }
}
