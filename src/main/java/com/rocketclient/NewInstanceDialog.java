package com.rocketclient;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class NewInstanceDialog {

    public static void open(InstanceManager instanceManager, String mcVersion, String loader, Consumer<InstanceManager.Instance> onCreated) {
        Stage stage = new Stage();

        String[] selectedIcon = { InstanceManager.BUILT_IN_ICONS[loader.equals("forge") ? 1 : 2] };

        ImageView iconPreview = new ImageView();
        iconPreview.setFitWidth(40);
        iconPreview.setFitHeight(40);
        iconPreview.setPreserveRatio(true);
        loadIcon(iconPreview, selectedIcon[0]);

        javafx.scene.layout.StackPane iconBox = new javafx.scene.layout.StackPane(iconPreview);
        iconBox.setPrefSize(56, 56);
        iconBox.setMaxSize(56, 56);
        iconBox.setStyle("-fx-background-color: #0f0f0f; -fx-border-color: #1a1a1a; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;");

        HBox iconRow = new HBox(14, iconBox, iconHint());
        iconRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label("NAME");
        nameLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 10; -fx-font-family: 'JetBrains Mono';");

        TextField nameField = new TextField(loaderLabel(loader) + " " + mcVersion);
        nameField.setStyle(
            "-fx-background-color: #0f0f0f; -fx-text-fill: #ffffff; -fx-font-family: 'JetBrains Mono'; " +
            "-fx-font-size: 13; -fx-border-color: #1a1a1a; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12;"
        );

        Label loaderLabel = new Label("LOADER");
        loaderLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 10; -fx-font-family: 'JetBrains Mono';");

        Label loaderValue = new Label(loaderLabel(loader) + "  \u00b7  " + mcVersion);
        loaderValue.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13; -fx-font-family: 'JetBrains Mono';");

        // Simple built-in icon picker: cycles through the icon set on click.
        int[] iconIndex = { indexOf(selectedIcon[0]) };
        iconBox.setOnMouseClicked(e -> {
            iconIndex[0] = (iconIndex[0] + 1) % InstanceManager.BUILT_IN_ICONS.length;
            selectedIcon[0] = InstanceManager.BUILT_IN_ICONS[iconIndex[0]];
            loadIcon(iconPreview, selectedIcon[0]);
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(dialogBtnStyle());
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(e -> stage.close());

        Button createBtn = new Button("Create");
        createBtn.setStyle(dialogBtnStyle());
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = loaderLabel(loader) + " " + mcVersion;
            InstanceManager.Instance inst = instanceManager.create(name, mcVersion, loader, selectedIcon[0]);
            stage.close();
            if (onCreated != null) onCreated.accept(inst);
        });

        HBox btnRow = new HBox(8, cancelBtn, createBtn);
        HBox.setHgrow(cancelBtn, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(createBtn, javafx.scene.layout.Priority.ALWAYS);

        VBox content = new VBox(16, iconRow, labeled(nameLabel, nameField), labeled(loaderLabel, loaderValue), btnRow);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #080404;");

        RocketWindowChrome.apply(stage, "NEW INSTANCE", content, 360, 320, null);
        stage.centerOnScreen();
        stage.show();
    }

    private static VBox labeled(Label label, javafx.scene.Node field) {
        VBox box = new VBox(6, label, field);
        return box;
    }

    private static Label iconHint() {
        Label hint = new Label("Click to change\ninstance icon");
        hint.setStyle("-fx-text-fill: #888888; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        return hint;
    }

    private static void loadIcon(ImageView iv, String path) {
        try {
            iv.setImage(new Image(NewInstanceDialog.class.getClassLoader().getResourceAsStream(path)));
        } catch (Exception e) {
            System.out.println("Could not load instance icon " + path + ": " + e.getMessage());
        }
    }

    private static int indexOf(String icon) {
        for (int i = 0; i < InstanceManager.BUILT_IN_ICONS.length; i++) {
            if (InstanceManager.BUILT_IN_ICONS[i].equals(icon)) return i;
        }
        return 0;
    }

    private static String loaderLabel(String loader) {
        return loader.equals("forge") ? "Forge" : "Fabric";
    }

    private static String dialogBtnStyle() {
        return "-fx-background-color: #0f0f0f; -fx-text-fill: #ffffff; -fx-font-family: 'JetBrains Mono'; " +
            "-fx-font-size: 12; -fx-border-color: #1a1a1a; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-cursor: hand; -fx-padding: 9 0;";
    }
}
