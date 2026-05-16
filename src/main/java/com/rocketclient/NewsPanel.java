package com.rocketclient;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class NewsPanel extends VBox {

    public NewsPanel() {
        setPrefWidth(260);
        setStyle("-fx-border-color: #161616; -fx-border-width: 0 0 0 1;");
        setPadding(new Insets(20, 20, 20, 20));
        setSpacing(10);

        Label title = new Label("NEWS");
        title.setStyle("-fx-text-fill: #2a2a2a; -fx-font-size: 9; -fx-font-family: 'JetBrains Mono';");

        getChildren().addAll(title, newsCard("16/05/2026", "The creation of Rocket Client"));
    }

    private VBox newsCard(String date, String text) {
        VBox card = new VBox(4);
        card.setStyle("-fx-border-color: #161616; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10;");

        Label dateLabel = new Label(date);
        dateLabel.setStyle("-fx-text-fill: #2e2e2e; -fx-font-size: 10; -fx-font-family: 'JetBrains Mono';");

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        textLabel.setWrapText(true);

        card.getChildren().addAll(dateLabel, textLabel);
        return card;
    }
}
