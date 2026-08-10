package com.rocketclient;

import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class IconPickerWindow {

    /** Generic glyph icons suitable as instance identity icons - excludes brand/UI-function
     *  icons (discord logo, gear, folder, etc.) that would be confusing to reuse here. */
    /** Core built-in icons, always shown first. */
    public static final String[] ICONS = {
        "icons/rocket-launch.png",
        "icons/anvil.png",
        "icons/fabric.png"
    };

    public static void open(InstanceManager instanceManager, Consumer<String> onSelected) {
        Stage stage = new Stage();

        FlowPane grid = new FlowPane(10, 10);
        grid.setPadding(new Insets(4));

        for (String iconPath : ICONS) {
            grid.getChildren().add(iconTile(iconPath, () -> {
                onSelected.accept(iconPath);
                stage.close();
            }));
        }

        // Fill the rest with custom icons already in use across other instances,
        // instead of empty filler tiles - lets you reuse a picked icon without
        // re-browsing the filesystem every time.
        for (String customIcon : recentlyUsedCustomIcons(instanceManager)) {
            grid.getChildren().add(iconTile(customIcon, () -> {
                onSelected.accept(customIcon);
                stage.close();
            }));
        }

        javafx.scene.control.Button browseBtn = new javafx.scene.control.Button("Browse for image...");
        browseBtn.setMaxWidth(Double.MAX_VALUE);
        browseBtn.setStyle(
            "-fx-background-color: #0f0f0f; -fx-text-fill: #ffffff; -fx-font-family: 'JetBrains Mono'; " +
            "-fx-font-size: 12; -fx-border-color: #1a1a1a; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-padding: 9 0;"
        );
        browseBtn.setOnAction(e -> {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle("Choose an instance icon");
            chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"
            ));
            java.io.File picked = chooser.showOpenDialog(stage);
            if (picked != null) {
                try {
                    String imported = IconUtil.importCustomIcon(picked.toPath());
                    onSelected.accept(imported);
                    stage.close();
                } catch (Exception ex) {
                    System.out.println("Could not import icon: " + ex.getMessage());
                }
            }
        });

        VBox root = new VBox(14, grid, browseBtn);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #080404;");

        RocketWindowChrome.apply(stage, "CHOOSE ICON", root, 340, 380, null);
        stage.centerOnScreen();
        stage.show();
    }

    private static java.util.List<String> recentlyUsedCustomIcons(InstanceManager instanceManager) {
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        instanceManager.list().stream()
            .sorted(java.util.Comparator.comparingLong((InstanceManager.Instance i) -> i.lastPlayed).reversed())
            .map(i -> i.icon)
            .filter(IconUtil::isCustom)
            .forEach(seen::add);
        return new java.util.ArrayList<>(seen);
    }

    private static StackPane iconTile(String iconPath, Runnable onClick) {
        ImageView iv = new ImageView();
        iv.setFitWidth(24);
        iv.setFitHeight(24);
        iv.setPreserveRatio(true);
        try {
            Image img = IconUtil.load(iconPath);
            if (img != null) iv.setImage(img);
        } catch (Exception ignored) {}

        StackPane box = new StackPane(iv);
        box.setPrefSize(48, 48);
        box.setMaxSize(48, 48);
        box.setStyle("-fx-background-color: #0f0f0f; -fx-border-color: #1a1a1a; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        box.setOnMouseEntered(e -> box.setStyle("-fx-background-color: #161616; -fx-border-color: #1a1a1a; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;"));
        box.setOnMouseExited(e -> box.setStyle("-fx-background-color: #0f0f0f; -fx-border-color: #1a1a1a; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;"));
        box.setOnMouseClicked(e -> onClick.run());
        return box;
    }
}
