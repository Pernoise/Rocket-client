package com.rocketclient;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;

public class AuthPanel extends VBox {

    private final AccountManager accountManager;

    public AuthPanel(AccountManager accountManager) {
        this.accountManager = accountManager;

        setStyle("-fx-background-color: #0f0f0f;");
        setPrefWidth(340);
        setPadding(new Insets(24));
        setSpacing(16);

        Label title = new Label("Accounts");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold; -fx-opacity: 0.88;");

        HBox tabBar = new HBox(2);
        tabBar.setStyle("-fx-border-color: #1a1a1a; -fx-border-width: 0 0 1 0;");

        Button msTab     = tabButton("Microsoft", true);
        Button elyTab    = tabButton("Ely.by",    false);
        Button loggedTab = tabButton("Logged in", false);
        tabBar.getChildren().addAll(msTab, elyTab, loggedTab);

        StackPane content = new StackPane();

        VBox msPanel     = buildMicrosoftPanel();
        VBox elyPanel    = buildElyByPanel();
        VBox loggedPanel = buildLoggedInPanel();

        elyPanel.setVisible(false);    elyPanel.setManaged(false);
        loggedPanel.setVisible(false); loggedPanel.setManaged(false);

        content.getChildren().addAll(msPanel, elyPanel, loggedPanel);

        msTab.setOnAction(e -> {
            msPanel.setVisible(true);      msPanel.setManaged(true);
            elyPanel.setVisible(false);    elyPanel.setManaged(false);
            loggedPanel.setVisible(false); loggedPanel.setManaged(false);
            msTab.setStyle(activeTabStyle());
            elyTab.setStyle(inactiveTabStyle());
            loggedTab.setStyle(inactiveTabStyle());
        });

        elyTab.setOnAction(e -> {
            elyPanel.setVisible(true);     elyPanel.setManaged(true);
            msPanel.setVisible(false);     msPanel.setManaged(false);
            loggedPanel.setVisible(false); loggedPanel.setManaged(false);
            elyTab.setStyle(activeTabStyle());
            msTab.setStyle(inactiveTabStyle());
            loggedTab.setStyle(inactiveTabStyle());
        });

        loggedTab.setOnAction(e -> {
            loggedPanel.setVisible(true);  loggedPanel.setManaged(true);
            msPanel.setVisible(false);     msPanel.setManaged(false);
            elyPanel.setVisible(false);    elyPanel.setManaged(false);
            loggedTab.setStyle(activeTabStyle());
            msTab.setStyle(inactiveTabStyle());
            elyTab.setStyle(inactiveTabStyle());
            refreshLoggedIn(loggedPanel);
        });

        getChildren().addAll(title, tabBar, content);
    }

    private VBox buildMicrosoftPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(16, 0, 0, 0));

        Label info = new Label("Click Login to open Microsoft's login page in your browser. Come back here when done.");
        info.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        info.setWrapText(true);

        Label status = new Label();
        status.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        status.setWrapText(true);

        Button loginBtn = new Button("Login with Microsoft");
        loginBtn.setStyle(primaryBtnStyle());
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        loginBtn.setOnAction(e -> {
            loginBtn.setDisable(true);
            loginBtn.setText("Waiting for browser...");
            status.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
            status.setText("Complete login in your browser.");

            MicrosoftAuth.login().thenAccept(account -> {
                accountManager.addAccount(account);
                Platform.runLater(() -> {
                    status.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
                    status.setText("Logged in as " + account.username);
                    loginBtn.setText("Login with Microsoft");
                    loginBtn.setDisable(false);
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> {
                    status.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
                    status.setText("Error: " + ex.getMessage());
                    loginBtn.setText("Login with Microsoft");
                    loginBtn.setDisable(false);
                });
                return null;
            });
        });

        panel.getChildren().addAll(info, loginBtn, status);
        return panel;
    }

    private VBox buildElyByPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(16, 0, 0, 0));

        Label info = new Label("Sign in with your Ely.by account.");
        info.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username or Email");
        usernameField.setStyle(fieldStyle());

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle(fieldStyle());

        Label status = new Label();
        status.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        status.setWrapText(true);

        Button loginBtn = new Button("Login");
        loginBtn.setStyle(primaryBtnStyle());
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        loginBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                status.setText("Please enter username and password.");
                return;
            }

            loginBtn.setDisable(true);
            loginBtn.setText("Logging in...");
            status.setText("");

            Thread thread = new Thread(() -> {
                try {
                    AccountManager.Account account = ElyByAuth.login(username, password);
                    accountManager.addAccount(account);
                    Platform.runLater(() -> {
                        status.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
                        status.setText("Logged in as " + account.username);
                        loginBtn.setText("Login");
                        loginBtn.setDisable(false);
                        passwordField.clear();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        status.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
                        status.setText("Error: " + ex.getMessage());
                        loginBtn.setText("Login");
                        loginBtn.setDisable(false);
                    });
                }
            });
            thread.setDaemon(true);
            thread.start();
        });

        panel.getChildren().addAll(info, usernameField, passwordField, loginBtn, status);
        return panel;
    }

    private VBox buildLoggedInPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(16, 0, 0, 0));
        refreshLoggedIn(panel);
        return panel;
    }

    private void refreshLoggedIn(VBox panel) {
        panel.getChildren().clear();

        if (accountManager.getAccounts().isEmpty()) {
            Label empty = new Label("No accounts logged in.");
            empty.setStyle("-fx-text-fill: #333333; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
            panel.getChildren().add(empty);
            return;
        }

        for (AccountManager.Account account : accountManager.getAccounts()) {
            VBox card = new VBox(6);
            boolean isSelected = accountManager.getSelected() != null &&
                account.uuid.equals(accountManager.getSelected().uuid);

            card.setStyle(isSelected
                ? "-fx-background-color: #141414; -fx-background-radius: 7; -fx-border-color: #333333; -fx-border-radius: 7; -fx-border-width: 0.5; -fx-padding: 12;"
                : "-fx-background-color: #141414; -fx-background-radius: 7; -fx-border-color: #222222; -fx-border-radius: 7; -fx-border-width: 0.5; -fx-padding: 12;"
            );

            Label typeBadge = new Label(account.type.equals("microsoft") ? "Microsoft" : "Ely.by");
            typeBadge.setStyle("-fx-text-fill: #333333; -fx-font-size: 9; -fx-font-family: 'JetBrains Mono';");

            Label username = new Label(account.username);
            username.setStyle(isSelected
                ? "-fx-text-fill: #ffffff; -fx-font-size: 13; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold;"
                : "-fx-text-fill: #888888; -fx-font-size: 13; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold;"
            );

            HBox btnRow = new HBox(6);

            Button selectBtn = new Button(isSelected ? "Active" : "Select");
            selectBtn.setStyle(isSelected ? activeAccountBtnStyle() : secondarySmallBtnStyle());
            selectBtn.setOnAction(ev -> {
                accountManager.setSelected(account.uuid);
                refreshLoggedIn(panel);
            });

            Button logoutBtn = new Button("Logout");
            logoutBtn.setStyle(logoutBtnStyle());
            logoutBtn.setOnAction(ev -> {
                accountManager.removeAccount(account.uuid);
                refreshLoggedIn(panel);
            });

            btnRow.getChildren().addAll(selectBtn, logoutBtn);
            card.getChildren().addAll(typeBadge, username, btnRow);
            panel.getChildren().add(card);
        }
    }

    private Button tabButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setStyle(active ? activeTabStyle() : inactiveTabStyle());
        return btn;
    }

    private String activeTabStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #ffffff; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
               "-fx-border-color: transparent transparent #ffffff transparent; " +
               "-fx-border-width: 0 0 1.5 0; -fx-padding: 8 14; -fx-cursor: hand;";
    }

    private String inactiveTabStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #555555; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
               "-fx-border-color: transparent; -fx-padding: 8 14; -fx-cursor: hand;";
    }

    private String primaryBtnStyle() {
        return "-fx-background-color: #ffffff; -fx-text-fill: #000000; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 13; -fx-font-weight: bold; " +
               "-fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 10 0;";
    }

    private String secondarySmallBtnStyle() {
        return "-fx-background-color: #1a1a1a; -fx-text-fill: #555555; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 10; " +
               "-fx-background-radius: 5; -fx-border-color: #2a2a2a; -fx-border-radius: 5; " +
               "-fx-border-width: 0.5; -fx-cursor: hand; -fx-padding: 5 10;";
    }

    private String activeAccountBtnStyle() {
        return "-fx-background-color: #222222; -fx-text-fill: #ffffff; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 10; " +
               "-fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 5 10;";
    }

    private String logoutBtnStyle() {
        return "-fx-background-color: #1a0000; -fx-text-fill: #f44336; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 10; " +
               "-fx-background-radius: 5; -fx-border-color: #2a0000; -fx-border-radius: 5; " +
               "-fx-border-width: 0.5; -fx-cursor: hand; -fx-padding: 5 10;";
    }

    private String fieldStyle() {
        return "-fx-background-color: #141414; -fx-text-fill: #ffffff; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; " +
               "-fx-border-color: #222222; -fx-border-radius: 6; -fx-background-radius: 6; " +
               "-fx-padding: 9 12; -fx-prompt-text-fill: #333333;";
    }

    private String logoutBtnStyle2() {
        return "-fx-background-color: #1a0000; -fx-text-fill: #f44336; " +
               "-fx-font-family: 'JetBrains Mono'; -fx-font-size: 10; " +
               "-fx-background-radius: 5; -fx-border-color: #2a0000; -fx-border-radius: 5; " +
               "-fx-border-width: 0.5; -fx-cursor: hand; -fx-padding: 5 10;";
    }
}
