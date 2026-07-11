package com.rocketclient;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class NewsPanel extends VBox {

    public NewsPanel() {
        setPrefWidth(260);
        setStyle("-fx-border-color: #161616; -fx-border-width: 0 0 0 1;");
        setPadding(new Insets(20, 20, 20, 20));
        setSpacing(10);

        Label title = new Label("NEWS");
        title.setStyle("-fx-text-fill: #2a2a2a; -fx-font-size: 9; -fx-font-family: 'JetBrains Mono';");

        VBox cards = new VBox(8);
        cards.getChildren().addAll(
            newsCard("08/07/2026", "Microsoft login now working via browser OAuth flow."),
            newsCard("08/07/2026", "New launcher icon."),
            newsCard("06/07/2026", "Account avatar and username shown in left panel."),
            newsCard("06/07/2026", "Maximize button added to title bar."),
            newsCard("05/07/2026", "Discord RPC now working on Windows via named pipes."),
            newsCard("04/07/2026", "Store button removed."),
            newsCard("03/07/2026", "Windows installer with desktop shortcut support."),
            newsCard("03/07/2026", "Custom branded installer via Inno Setup."),
            newsCard("30/06/2026", "Live launch log window added."),
            newsCard("30/06/2026", "Memory leak fix, launcher closes when Minecraft exits."),
            newsCard("28/06/2026", "Custom title bar replacing system default."),
            newsCard("28/06/2026", "Hardcoded Java paths removed, works with any Java."),
            newsCard("28/06/2026", "Auto-detects platform for correct JavaFX natives."),
            newsCard("28/06/2026", "Linux AppImage build available."),
            newsCard("18/05/2026", "Rocket Client is now open for BETA testing."),
            newsCard("18/05/2026", "We want your feedback. Open a ticket on GitHub if you encounter any bugs."),
            newsCard("17/05/2026", "Ely.by authentication fully working."),
            newsCard("17/05/2026", "Discord RPC connected."),
            newsCard("17/05/2026", "Fabric loader support added."),
            newsCard("17/05/2026", "Forge 1.8.9 toggle added."),
            newsCard("17/05/2026", "Settings panel live, Java args and RAM allocation."),
            newsCard("17/05/2026", "Splash screen on launch."),
            newsCard("17/05/2026", "Account switcher with logout support."),
            newsCard("17/05/2026", "Version selector 1.19 to 26.1.2."),
            newsCard("17/05/2026", "Java path override in settings.")
        );

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

        getChildren().addAll(title, scroll);
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
