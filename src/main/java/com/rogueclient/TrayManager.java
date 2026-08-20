package com.rogueclient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Lets the launcher minimize to the OS system tray instead of exiting when
 * Minecraft launches or the window is closed, if "Hide launcher" is enabled.
 *
 * Uses a custom JavaFX Popup instead of java.awt.PopupMenu for the tray
 * click menu, since the native AWT popup can't be themed at all. The popup
 * is attached to a dedicated, permanently-visible 1x1 owner stage parked
 * off-screen rather than the main stage - JavaFX's Popup.show(owner)
 * requires the owner window to actually be showing, and the main stage is
 * hidden while minimized to tray, which is exactly when the menu is needed.
 * That mismatch was why right-click/restore could silently fail.
 */
public class TrayManager {

    private static volatile Stage stage;
    private static Stage popupOwner;
    private static TrayIcon trayIcon;
    private static boolean initialized = false;
    private static Popup trayMenu;

    public static void init(Stage primaryStage) {
        stage = primaryStage;
        if (initialized || !SystemTray.isSupported()) return;

        try {
            Platform.setImplicitExit(false);

            Platform.runLater(() -> {
                popupOwner = new Stage(StageStyle.UTILITY);
                popupOwner.setOpacity(0);
                popupOwner.setX(-10000);
                popupOwner.setY(-10000);
                popupOwner.setWidth(1);
                popupOwner.setHeight(1);
                popupOwner.show();
            });

            java.awt.Image icon = loadCroppedTrayIcon();

            trayIcon = new TrayIcon(icon, "Rogue Client");
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
            // Fallback: some platforms fire the action event on single click
            // instead of (or in addition to) mouseClicked. Harmless if both fire.
            trayIcon.addActionListener(e -> Platform.runLater(TrayManager::restoreFromTray));

            SystemTray.getSystemTray().add(trayIcon);
            initialized = true;
        } catch (AWTException e) {
            System.out.println("Could not initialize system tray: " + e.getMessage());
        }
    }

    /**
     * Loads the app icon and crops it to its opaque bounding box before handing
     * it to the tray. The source PNG has a lot of transparent padding around
     * the glyph, which made it look tiny/invisible once Windows auto-shrinks
     * it to actual tray icon size.
     */
    private static java.awt.Image loadCroppedTrayIcon() {
        try (InputStream in = TrayManager.class.getClassLoader().getResourceAsStream("icons/rogue-launch.png")) {
            BufferedImage src = ImageIO.read(in);

            int minX = src.getWidth(), minY = src.getHeight(), maxX = -1, maxY = -1;
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int alpha = (src.getRGB(x, y) >> 24) & 0xff;
                    if (alpha > 10) {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            if (maxX < minX || maxY < minY) return src; // fully transparent, bail out safely

            int w = maxX - minX + 1;
            int h = maxY - minY + 1;
            BufferedImage cropped = src.getSubimage(minX, minY, w, h);

            // Pad to a square with a small margin so it doesn't touch the tray icon edges.
            int size = Math.max(w, h);
            int margin = (int) (size * 0.12);
            int canvas = size + margin * 2;

            BufferedImage padded = new BufferedImage(canvas, canvas, BufferedImage.TYPE_INT_ARGB);
            var g = padded.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(cropped, margin + (size - w) / 2, margin + (size - h) / 2, w, h, null);
            g.dispose();

            return padded;
        } catch (Exception e) {
            System.out.println("Could not load/crop tray icon: " + e.getMessage());
            return java.awt.Toolkit.getDefaultToolkit().createImage(
                TrayManager.class.getClassLoader().getResource("icons/rogue-launch.png")
            );
        }
    }

    private static void showTrayMenu(double screenX, double screenY) {
        if (popupOwner == null) return;

        if (trayMenu != null && trayMenu.isShowing()) {
            trayMenu.hide();
            return;
        }

        Button showBtn = new Button("Show Rogue Client");
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
        trayMenu.show(popupOwner);

        // setAutoHide alone is unreliable here since popupOwner is an invisible,
        // off-screen utility window with no real OS focus semantics for the
        // window manager to key "click elsewhere" off of. Explicitly requesting
        // focus and dismissing on focus loss covers the cases autoHide misses.
        box.requestFocus();
        trayMenu.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && trayMenu.isShowing()) trayMenu.hide();
        });
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
        if (popupOwner != null) {
            Platform.runLater(() -> popupOwner.close());
        }
        Platform.exit();
        System.exit(0);
    }
}
