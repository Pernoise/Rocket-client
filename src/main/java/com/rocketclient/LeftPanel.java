package com.rocketclient;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.awt.Desktop;
import java.net.URI;

public class LeftPanel extends VBox {

    public LeftPanel() {
        setPrefWidth(56);
        setStyle("-fx-background-color: #0f0f0f; -fx-border-color: #1a1a1a; -fx-border-width: 0 1 0 0;");
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(14, 0, 14, 0));
        setSpacing(6);

        VBox logo     = createIcon("icons/rocket-launch.png", "Rocket Client", true, null);
        VBox account  = createIcon("icons/user.png", "Account", false, null);
        VBox store    = createIcon("icons/handbag-simple.png", "Store — Coming Soon", false, null);
        VBox settings = createIcon("icons/gear.png", "Settings", false, null);

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox discord = createIcon("icons/discord-logo.png", "Discord — Coming Soon", false, null);
        VBox github  = createIcon("icons/github-logo.png", "GitHub", false, "https://github.com/Pernoise/Rocket-client");
        VBox website = createIcon("icons/globe.png", "Website — Coming Soon", false, null);

        getChildren().addAll(logo, account, store, settings, spacer, discord, github, website);
    }

    private VBox createIcon(String resourcePath, String tooltip, boolean isLogo, String url) {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(34, 34);
        box.setMaxSize(34, 34);

        String baseStyle = isLogo
            ? "-fx-background-color: #1a1a1a; -fx-background-radius: 8; -fx-border-color: #2a2a2a; -fx-border-radius: 8; -fx-border-width: 0.5;"
            : "-fx-background-color: #161616; -fx-background-radius: 8;";
        box.setStyle(baseStyle);

        try {
            Image img = new Image(getClass().getClassLoader().getResourceAsStream(resourcePath));
            ImageView iv = new ImageView(img);
            iv.setFitWidth(isLogo ? 18 : 16);
            iv.setFitHeight(isLogo ? 18 : 16);
            iv.setPreserveRatio(true);
            box.getChildren().add(iv);
        } catch (Exception e) {
            System.out.println("Could not load icon: " + resourcePath);
        }

        Tooltip.install(box, new Tooltip(tooltip));

        if (isLogo == false) {
            box.setOnMouseEntered(e -> box.setStyle("-fx-background-color: #222222; -fx-background-radius: 8;"));
            box.setOnMouseExited(e  -> box.setStyle("-fx-background-color: #161616; -fx-background-radius: 8;"));

            if (url != null) {
                box.setOnMouseClicked(e -> {
                    try {
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (Exception ex) {
                        System.out.println("Could not open URL: " + url);
                    }
                });
                box.setStyle("-fx-background-color: #161616; -fx-background-radius: 8; -fx-cursor: hand;");
            }
        }

        return box;
    }
}
