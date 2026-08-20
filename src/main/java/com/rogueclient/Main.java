package com.rogueclient;

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
        stage.setTitle("Rogue Client");
        stage.setTitle("Rogue Client");
        Font.loadFont(getClass().getClassLoader().getResourceAsStream("fonts/JetBrainsMono-Regular.ttf"), 12);
        Font.loadFont(getClass().getClassLoader().getResourceAsStream("fonts/JetBrainsMono-Bold.ttf"), 12);

        AccountManager accountManager   = new AccountManager();
        SettingsManager settingsManager = new SettingsManager();
        settingsManager.load();
        LogBanner.print(settingsManager);

        SplashScreen splash = new SplashScreen(() -> {
            DiscordRPC.start(settingsManager);

            Label titleLabel = new Label("Rogue Client  BETA");
            titleLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");

            Button minimizeBtn = new Button("-");
            minimizeBtn.setStyle(titleBtnStyle());
            minimizeBtn.setOnMouseEntered(e -> minimizeBtn.setStyle(titleBtnHoverStyle()));
            minimizeBtn.setOnMouseExited(e -> minimizeBtn.setStyle(titleBtnStyle()));
            minimizeBtn.setOnAction(e -> stage.setIconified(true));

            final boolean[] maximized = {false};
            final double[] preMaximizeBounds = new double[4]; // x, y, width, height

            Button maximizeBtn = new Button("[]");
            maximizeBtn.setStyle(titleBtnStyle());
            maximizeBtn.setOnMouseEntered(e -> maximizeBtn.setStyle(titleBtnHoverStyle()));
            maximizeBtn.setOnMouseExited(e -> maximizeBtn.setStyle(titleBtnStyle()));
            maximizeBtn.setOnAction(e -> {
                maximized[0] = !maximized[0];
                if (maximized[0]) {
                    preMaximizeBounds[0] = stage.getX();
                    preMaximizeBounds[1] = stage.getY();
                    preMaximizeBounds[2] = stage.getWidth();
                    preMaximizeBounds[3] = stage.getHeight();
                    // Snap to the screen's *visual* bounds (excludes the taskbar) so this reads
                    // as a normal windowed maximize, not true OS fullscreen covering everything.
                    javafx.geometry.Rectangle2D visual = javafx.stage.Screen
                        .getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())
                        .get(0).getVisualBounds();
                    stage.setX(visual.getMinX());
                    stage.setY(visual.getMinY());
                    stage.setWidth(visual.getWidth());
                    stage.setHeight(visual.getHeight());
                } else {
                    stage.setX(preMaximizeBounds[0]);
                    stage.setY(preMaximizeBounds[1]);
                    stage.setWidth(preMaximizeBounds[2]);
                    stage.setHeight(preMaximizeBounds[3]);
                }
                maximizeBtn.setText(maximized[0] ? "[-]" : "[]");
            });

            Button closeBtn = new Button("x");
            closeBtn.setStyle(titleBtnStyle());
            closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(closeBtnHoverStyle()));
            closeBtn.setOnMouseExited(e -> closeBtn.setStyle(titleBtnStyle()));
            closeBtn.setOnAction(e -> {
                if (settingsManager.hideLauncher) {
                    TrayManager.minimizeToTray();
                } else {
                    TrayManager.quit();
                }
            });

            HBox spacer = new HBox();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox titleBar = new HBox(0, titleLabel, spacer, minimizeBtn, maximizeBtn, closeBtn);
            titleBar.setMaxWidth(Double.MAX_VALUE);
            titleBar.setAlignment(Pos.CENTER_LEFT);
            titleBar.setPadding(new Insets(6, 8, 6, 12));
            titleBar.setStyle("-fx-background-color: #080404; -fx-background-radius: 12 12 0 0;");

            titleBar.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
            titleBar.setOnMouseDragged(e -> {
                if (!maximized[0]) {
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
            Scene scene = new Scene(wrapper, 1320, 770);
            scene.setFill(Color.TRANSPARENT);
            scene.getStylesheets().add(getClass().getClassLoader().getResource("css/theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(960);
            stage.setMinHeight(600);

            // Undecorated/transparent stages get no OS-provided resize borders, so drag-to-resize
            // has to be done by hand: watch the mouse near the window edges and drag width/height
            // ourselves. Top edge is intentionally excluded - it overlaps the title bar's own
            // drag-to-move handlers above, and fighting over the same pixels there gets janky.
            final double RESIZE_MARGIN = 6;
            final Object[] resizeState = { null, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
            // [0]=edge ("L","R","B","BL","BR",null), [1]=startScreenX, [2]=startScreenY,
            // [3]=startStageX, [4]=startStageY, [5]=startWidth, [6]=startHeight

            java.util.function.BiFunction<Double, Double, String> detectEdge = (x, y) -> {
                double w = scene.getWidth(), h = scene.getHeight();
                boolean left = x < RESIZE_MARGIN, right = x > w - RESIZE_MARGIN, bottom = y > h - RESIZE_MARGIN;
                if (bottom && left) return "BL";
                if (bottom && right) return "BR";
                if (left) return "L";
                if (right) return "R";
                if (bottom) return "B";
                return null;
            };

            scene.setOnMouseMoved(e -> {
                if (maximized[0]) { scene.setCursor(javafx.scene.Cursor.DEFAULT); return; }
                String edge = detectEdge.apply(e.getX(), e.getY());
                javafx.scene.Cursor cursor = switch (edge == null ? "" : edge) {
                    case "L" -> javafx.scene.Cursor.W_RESIZE;
                    case "R" -> javafx.scene.Cursor.E_RESIZE;
                    case "B" -> javafx.scene.Cursor.S_RESIZE;
                    case "BL" -> javafx.scene.Cursor.SW_RESIZE;
                    case "BR" -> javafx.scene.Cursor.SE_RESIZE;
                    default -> javafx.scene.Cursor.DEFAULT;
                };
                scene.setCursor(cursor);
            });
            scene.setOnMousePressed(e -> {
                if (maximized[0]) { resizeState[0] = null; return; }
                resizeState[0] = detectEdge.apply(e.getX(), e.getY());
                if (resizeState[0] != null) {
                    resizeState[1] = e.getScreenX();
                    resizeState[2] = e.getScreenY();
                    resizeState[3] = stage.getX();
                    resizeState[4] = stage.getY();
                    resizeState[5] = stage.getWidth();
                    resizeState[6] = stage.getHeight();
                }
            });
            scene.setOnMouseDragged(e -> {
                if (resizeState[0] == null) return;
                double dx = e.getScreenX() - (double) resizeState[1];
                double dy = e.getScreenY() - (double) resizeState[2];
                double startX = (double) resizeState[3], startY = (double) resizeState[4];
                double startW = (double) resizeState[5], startH = (double) resizeState[6];
                double newW = startW, newH = startH, newX = startX, newY = startY;

                switch ((String) resizeState[0]) {
                    case "R" -> newW = startW + dx;
                    case "L" -> { newW = startW - dx; newX = startX + dx; }
                    case "B" -> newH = startH + dy;
                    case "BR" -> { newW = startW + dx; newH = startH + dy; }
                    case "BL" -> { newW = startW - dx; newX = startX + dx; newH = startH + dy; }
                }

                newW = Math.max(stage.getMinWidth(), newW);
                newH = Math.max(stage.getMinHeight(), newH);
                if (resizeState[0].equals("L") || resizeState[0].equals("BL")) {
                    if (newW <= stage.getMinWidth()) newX = startX + (startW - stage.getMinWidth());
                }

                stage.setWidth(newW);
                stage.setHeight(newH);
                stage.setX(newX);
                stage.setY(newY);
            });
            scene.setOnMouseReleased(e -> resizeState[0] = null);

            try {
                Image icon = new Image(getClass().getClassLoader().getResourceAsStream("icons/rogue-launch.png"));
                stage.getIcons().add(icon);
            } catch (Exception e) {
                System.out.println("Could not load taskbar icon");
            }

            stage.show();
            if (settingsManager.enableTray) {
                TrayManager.init(stage);
            }

            if (FirstLaunchDialog.isFirstLaunch()) {
                Platform.runLater(() -> FirstLaunchDialog.show());
            }
        });

        splash.show();
    }

    private String titleBtnStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 2 10; -fx-border-color: transparent;";
    }

    private String titleBtnHoverStyle() {
        return "-fx-background-color: #1a1a1a; -fx-text-fill: #ffffff; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 2 10; -fx-border-color: transparent;";
    }

    private String closeBtnHoverStyle() {
        return "-fx-background-color: #3a0000; -fx-text-fill: #ff4444; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 2 10; -fx-border-color: transparent;";
    }

    public static void main(String[] args) { try { java.nio.file.Path logDir = java.nio.file.Paths.get(System.getProperty("user.home"), ".rogueclient", "logs"); java.nio.file.Files.createDirectories(logDir); java.io.PrintStream logStream = new java.io.PrintStream(new java.io.FileOutputStream(logDir.resolve("launcher-latest.log").toFile(), false)); System.setOut(logStream); System.setErr(logStream); } catch (Exception e) {} 
        launch(args);
    }
}
