package com.rocketclient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

/**
 * "Skin & Cape" popup, opened from the left rail. Mirrors AuthPanel's layout
 * language (same section labels, row style, primary button) so it reads as
 * part of the same set of windows rather than a bolted-on feature.
 */
public class SkinManagerWindow {

    public static void open(AccountManager accountManager) {
        AccountManager.Account acc = accountManager.getSelected();

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f0f0f;");

        Label title = new Label("Skin & Cape");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold;");

        if (acc == null) {
            root.getChildren().addAll(title, infoLabel("No account selected. Log in from the Account window first."));
            RocketWindowChrome.apply(popup, "SKIN & CAPE", root, 460, 220, null);
            popup.centerOnScreen();
            popup.showAndWait();
            return;
        }

        if (!"microsoft".equalsIgnoreCase(acc.type)) {
            root.getChildren().addAll(title, infoLabel(
                "Skin & cape management requires a Microsoft account.\n" +
                acc.username + " is signed in with " + acc.type + ", which Mojang doesn't expose a profile API for."
            ));
            RocketWindowChrome.apply(popup, "SKIN & CAPE", root, 460, 220, null);
            popup.centerOnScreen();
            popup.showAndWait();
            return;
        }

        if (acc.accessToken == null || acc.accessToken.isBlank()) {
            root.getChildren().addAll(title, infoLabel(
                "This account's session token is missing or couldn't be decrypted.\n" +
                "Remove " + acc.username + " from Accounts > Logged in and log back in with Microsoft to fix this."
            ));
            RocketWindowChrome.apply(popup, "SKIN & CAPE", root, 460, 240, null);
            popup.centerOnScreen();
            popup.showAndWait();
            return;
        }

        ImageView bodyRender = new ImageView();
        bodyRender.setFitWidth(180);
        bodyRender.setFitHeight(340);
        bodyRender.setPreserveRatio(true);
        bodyRender.setSmooth(false); // upscaled pixel art stays crisp instead of blurring

        Label modelLabel = new Label("");
        modelLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 10; -fx-font-family: 'JetBrains Mono';");

        VBox previewBox = new VBox(6, bodyRender, modelLabel);
        previewBox.setAlignment(Pos.TOP_CENTER);
        previewBox.setPadding(new Insets(12));
        previewBox.setPrefWidth(220);
        previewBox.setStyle("-fx-background-color: #141414; -fx-background-radius: 8; -fx-border-color: #1a1a1a; -fx-border-radius: 8; -fx-border-width: 1;");

        Label status = new Label("Loading your skins and capes...");
        status.setWrapText(true);
        status.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");

        Label skinsHeader = sectionHeader("Skins");
        VBox skinsList = new VBox(6);

        Label capesHeader = sectionHeader("Capes");
        VBox capesList = new VBox(6);

        VBox listsColumn = new VBox(12, skinsHeader, skinsList, capesHeader, capesList);

        ScrollPane scroll = new ScrollPane(listsColumn);
        scroll.getStyleClass().add("rocket-scroll");
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setPrefHeight(320);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox rightColumn = new VBox(10, status, scroll);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        HBox mainRow = new HBox(16, previewBox, rightColumn);
        VBox.setVgrow(mainRow, Priority.ALWAYS);

        ToggleGroup variantGroup = new ToggleGroup();
        ToggleButton classicBtn = new ToggleButton("Classic");
        ToggleButton slimBtn = new ToggleButton("Slim");
        classicBtn.setToggleGroup(variantGroup);
        slimBtn.setToggleGroup(variantGroup);
        classicBtn.setSelected(true);
        classicBtn.setStyle(toggleStyle(true));
        slimBtn.setStyle(toggleStyle(false));
        classicBtn.setOnAction(e -> { classicBtn.setStyle(toggleStyle(true)); slimBtn.setStyle(toggleStyle(false)); });
        slimBtn.setOnAction(e -> { slimBtn.setStyle(toggleStyle(true)); classicBtn.setStyle(toggleStyle(false)); });
        classicBtn.setOnMouseEntered(e -> classicBtn.setStyle(toggleHoverStyle(classicBtn.isSelected())));
        classicBtn.setOnMouseExited(e -> classicBtn.setStyle(toggleStyle(classicBtn.isSelected())));
        slimBtn.setOnMouseEntered(e -> slimBtn.setStyle(toggleHoverStyle(slimBtn.isSelected())));
        slimBtn.setOnMouseExited(e -> slimBtn.setStyle(toggleStyle(slimBtn.isSelected())));

        Button uploadBtn = new Button("Upload New Skin");
        uploadBtn.setStyle(primaryBtnStyle());
        uploadBtn.setOnMouseEntered(e -> uploadBtn.setStyle(primaryBtnHoverStyle()));
        uploadBtn.setOnMouseExited(e -> uploadBtn.setStyle(primaryBtnStyle()));
        uploadBtn.setMaxWidth(Double.MAX_VALUE);

        HBox uploadRow = new HBox(8, uploadBtn, classicBtn, slimBtn);
        uploadRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(uploadBtn, Priority.ALWAYS);

        root.getChildren().addAll(title, mainRow, uploadRow);

        Runnable[] reloadHolder = new Runnable[1];
        reloadHolder[0] = () -> loadProfile(acc, status, skinsList, capesList, modelLabel, bodyRender, reloadHolder);

        uploadBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select skin PNG");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image", "*.png"));
            File file = chooser.showOpenDialog(popup);
            if (file == null) return;

            String variant = classicBtn.isSelected() ? "classic" : "slim";
            uploadBtn.setDisable(true);
            uploadBtn.setText("Uploading...");
            new Thread(() -> {
                try {
                    MinecraftServicesClient.uploadSkin(acc.accessToken, file.toPath(), variant);
                    Platform.runLater(() -> {
                        uploadBtn.setDisable(false);
                        uploadBtn.setText("Upload New Skin");
                        reloadHolder[0].run();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        uploadBtn.setDisable(false);
                        uploadBtn.setText("Upload New Skin");
                        status.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
                        status.setText("Upload failed: " + ex.getMessage());
                    });
                }
            }).start();
        });

        reloadHolder[0].run();

        RocketWindowChrome.apply(popup, "SKIN & CAPE", root, 640, 640, null);
        popup.centerOnScreen();
        popup.showAndWait();
    }

    private static void loadProfile(AccountManager.Account acc, Label status, VBox skinsList, VBox capesList,
                                     Label modelLabel, ImageView bodyRender, Runnable[] reloadHolder) {
        new Thread(() -> {
            try {
                MinecraftServicesClient.Profile profile = MinecraftServicesClient.fetchProfile(acc.accessToken);
                Platform.runLater(() -> {
                    status.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
                    status.setText(profile.skins.size() + " skin(s), " + profile.capes.size() + " cape(s) on this account.");

                    MinecraftServicesClient.SkinEntry activeSkin = profile.skins.stream()
                        .filter(MinecraftServicesClient.SkinEntry::active)
                        .findFirst().orElse(null);
                    boolean slim = activeSkin != null && "SLIM".equalsIgnoreCase(activeSkin.variant);
                    modelLabel.setText(activeSkin == null ? "" : (slim ? "Slim model" : "Classic model"));

                    if (activeSkin != null) {
                        refreshBodyRender(bodyRender, acc.uuid);
                    }

                    skinsList.getChildren().clear();
                    for (MinecraftServicesClient.SkinEntry skin : profile.skins) {
                        skinsList.getChildren().add(skinRow(acc, skin, reloadHolder));
                    }
                    if (profile.skins.isEmpty()) {
                        skinsList.getChildren().add(emptyRow("No skins on this account yet."));
                    }

                    capesList.getChildren().clear();
                    for (MinecraftServicesClient.CapeEntry cape : profile.capes) {
                        capesList.getChildren().add(capeRow(acc, cape, reloadHolder));
                    }
                    if (profile.capes.isEmpty()) {
                        capesList.getChildren().add(emptyRow("No capes on this account."));
                    } else {
                        capesList.getChildren().add(removeCapeRow(acc, reloadHolder));
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    status.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
                    status.setText("Could not load profile: " + ex.getMessage());
                });
            }
        }).start();
    }

    private static HBox skinRow(AccountManager.Account acc, MinecraftServicesClient.SkinEntry skin,
                                 Runnable[] reloadHolder) {
        HBox row = rowBase();

        // Front of the head, from the skin's own texture (8,8 8x8 in the 64x64 sheet).
        ImageView thumb = texturePreview(skin.url, 8, 8, 8, 8, 40, 40);
        thumb.setStyle("-fx-background-color: #0a0a0a;");

        Label name = new Label((skin.variant != null ? skin.variant : "Skin") + (skin.active() ? " (equipped)" : ""));
        name.setStyle(rowLabelStyle(skin.active()));
        HBox.setHgrow(name, Priority.ALWAYS);

        Button equipBtn = new Button(skin.active() ? "Equipped" : "Equip");
        equipBtn.setStyle(skin.active() ? equippedBtnStyle() : selectBtnStyle());
        equipBtn.setDisable(skin.active());
        equipBtn.setOnMouseEntered(e -> { if (!equipBtn.isDisabled()) equipBtn.setStyle(selectBtnHoverStyle()); });
        equipBtn.setOnMouseExited(e -> { if (!equipBtn.isDisabled()) equipBtn.setStyle(selectBtnStyle()); });
        equipBtn.setOnAction(e -> {
            equipBtn.setDisable(true);
            equipBtn.setText("Equipping...");
            new Thread(() -> {
                try {
                    MinecraftServicesClient.activateSkinByUrl(acc.accessToken, skin.url, skin.variant);
                    Platform.runLater(() -> reloadHolder[0].run());
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        equipBtn.setDisable(false);
                        equipBtn.setText("Equip");
                    });
                }
            }).start();
        });

        row.getChildren().addAll(thumb, name, equipBtn);
        return row;
    }

    private static HBox capeRow(AccountManager.Account acc, MinecraftServicesClient.CapeEntry cape, Runnable[] reloadHolder) {
        HBox row = rowBase();

        // Front panel of the cape texture (1,1 10x16 in the 64x32 sheet).
        ImageView thumb = texturePreview(cape.url, 1, 1, 10, 16, 30, 48);
        thumb.setStyle("-fx-background-color: #0a0a0a;");

        Label name = new Label(cape.alias + (cape.active() ? " (equipped)" : ""));
        name.setStyle(rowLabelStyle(cape.active()));
        HBox.setHgrow(name, Priority.ALWAYS);

        Button equipBtn = new Button(cape.active() ? "Equipped" : "Equip");
        equipBtn.setStyle(cape.active() ? equippedBtnStyle() : selectBtnStyle());
        equipBtn.setDisable(cape.active());
        equipBtn.setOnMouseEntered(e -> { if (!equipBtn.isDisabled()) equipBtn.setStyle(selectBtnHoverStyle()); });
        equipBtn.setOnMouseExited(e -> { if (!equipBtn.isDisabled()) equipBtn.setStyle(selectBtnStyle()); });
        equipBtn.setOnAction(e -> {
            equipBtn.setDisable(true);
            equipBtn.setText("Equipping...");
            new Thread(() -> {
                try {
                    MinecraftServicesClient.activateCape(acc.accessToken, cape.id);
                    Platform.runLater(() -> reloadHolder[0].run());
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        equipBtn.setDisable(false);
                        equipBtn.setText("Equip");
                    });
                }
            }).start();
        });

        row.getChildren().addAll(thumb, name, equipBtn);
        return row;
    }

    private static HBox removeCapeRow(AccountManager.Account acc, Runnable[] reloadHolder) {
        HBox row = rowBase();
        Label name = new Label("No cape");
        name.setStyle(rowLabelStyle(false));
        HBox.setHgrow(name, Priority.ALWAYS);

        Button removeBtn = new Button("Unequip");
        removeBtn.setStyle(selectBtnStyle());
        removeBtn.setOnMouseEntered(e -> { if (!removeBtn.isDisabled()) removeBtn.setStyle(selectBtnHoverStyle()); });
        removeBtn.setOnMouseExited(e -> { if (!removeBtn.isDisabled()) removeBtn.setStyle(selectBtnStyle()); });
        removeBtn.setOnAction(e -> {
            removeBtn.setDisable(true);
            new Thread(() -> {
                try {
                    MinecraftServicesClient.removeCape(acc.accessToken);
                    Platform.runLater(() -> reloadHolder[0].run());
                } catch (Exception ex) {
                    Platform.runLater(() -> removeBtn.setDisable(false));
                }
            }).start();
        });

        row.getChildren().addAll(name, removeBtn);
        return row;
    }

    /** High-resolution full-body render (includes the equipped cape) with fallback sources if one is unreachable. */
    private static void refreshBodyRender(ImageView bodyRender, String uuid) {
        long t = System.currentTimeMillis();
        String primary   = "https://crafatar.com/renders/body/" + uuid + "?overlay&scale=10&t=" + t;
        String fallback1 = "https://vzge.me/full/720/" + uuid + "?y=180";
        String fallback2 = "https://mc-heads.net/body/" + uuid + "/540";
        loadWithFallback(bodyRender, new String[]{primary, fallback1, fallback2}, 0);
    }

    private static void loadWithFallback(ImageView view, String[] urls, int index) {
        if (index >= urls.length) return;
        Image img = new Image(urls[index], true);
        img.errorProperty().addListener((obs, was, isError) -> {
            if (isError) {
                Platform.runLater(() -> loadWithFallback(view, urls, index + 1));
            }
        });
        view.setImage(img);
    }

    /** Crops a small square/rect region out of a raw skin/cape texture and scales it up crisply. */
    private static ImageView texturePreview(String textureUrl, double vx, double vy, double vw, double vh, double outW, double outH) {
        ImageView iv = new ImageView();
        iv.setFitWidth(outW);
        iv.setFitHeight(outH);
        iv.setSmooth(false); // keep pixel-art crisp instead of blurring the crop
        iv.setPreserveRatio(false);
        Image tex = new Image(textureUrl, true);
        tex.progressProperty().addListener((obs, oldP, newP) -> {
            if (newP.doubleValue() >= 1.0 && !tex.isError()) {
                Platform.runLater(() -> {
                    iv.setImage(tex);
                    iv.setViewport(new javafx.geometry.Rectangle2D(vx, vy, vw, vh));
                });
            }
        });
        return iv;
    }

    private static HBox rowBase() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #141414; -fx-background-radius: 6; -fx-padding: 8 12;");
        return row;
    }

    private static Label emptyRow(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        return label;
    }

    private static Label sectionHeader(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #888888; -fx-font-size: 10; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold;");
        return label;
    }

    private static Label infoLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        return label;
    }

    private static String rowLabelStyle(boolean active) {
        return "-fx-text-fill: " + (active ? "#4caf50" : "#ffffff") + "; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';";
    }

    private static String selectBtnStyle() {
        return "-fx-background-color: #1a1a1a; -fx-text-fill: #ffffff; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 10; -fx-cursor: hand; -fx-padding: 4 10; -fx-border-color: #2a2a2a; -fx-border-radius: 4; -fx-background-radius: 4;";
    }

    private static String selectBtnHoverStyle() {
        return "-fx-background-color: #262626; -fx-text-fill: #ffffff; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 10; -fx-cursor: hand; -fx-padding: 4 10; -fx-border-color: #3a3a3a; -fx-border-radius: 4; -fx-background-radius: 4;";
    }

    private static String equippedBtnStyle() {
        return "-fx-background-color: #101a10; -fx-text-fill: #4caf50; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 10; -fx-padding: 4 10; -fx-border-color: #1a2a1a; -fx-border-radius: 4; -fx-background-radius: 4;";
    }

    private static String primaryBtnStyle() {
        return "-fx-background-color: #0f0f0f; -fx-text-fill: #ffffff; " +
            "-fx-font-size: 12; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono'; " +
            "-fx-border-color: #1a1a1a; -fx-border-width: 1; " +
            "-fx-background-radius: 8; -fx-border-radius: 8; " +
            "-fx-cursor: hand; -fx-padding: 10 20; -fx-opacity: 0.88;";
    }

    private static String primaryBtnHoverStyle() {
        return "-fx-background-color: #1c1c1c; -fx-text-fill: #ffffff; " +
            "-fx-font-size: 12; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono'; " +
            "-fx-border-color: #2a2a2a; -fx-border-width: 1; " +
            "-fx-background-radius: 8; -fx-border-radius: 8; " +
            "-fx-cursor: hand; -fx-padding: 10 20; -fx-opacity: 1;";
    }

    private static String toggleStyle(boolean selected) {
        return "-fx-background-color: " + (selected ? "#1a1a1a" : "#0f0f0f") + "; -fx-text-fill: #ffffff; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 10; -fx-cursor: hand; -fx-padding: 10 12; " +
            "-fx-border-color: #2a2a2a; -fx-border-radius: 6; -fx-background-radius: 6;";
    }

    private static String toggleHoverStyle(boolean selected) {
        return "-fx-background-color: " + (selected ? "#242424" : "#1a1a1a") + "; -fx-text-fill: #ffffff; " +
            "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 10; -fx-cursor: hand; -fx-padding: 10 12; " +
            "-fx-border-color: #3a3a3a; -fx-border-radius: 6; -fx-background-radius: 6;";
    }
}
