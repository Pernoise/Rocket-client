package com.rocketclient;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AuthPanel extends VBox {

    private final AccountManager accountManager;

    public AuthPanel(AccountManager accountManager) {
        this.accountManager = accountManager;
        setPadding(new Insets(20));
        setSpacing(12);
        setStyle("-fx-background-color: #0f0f0f;");

        Label title = new Label("Accounts");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold;");

        Button msTab  = new Button("Microsoft");
        Button elyTab = new Button("Ely.by");
        Button loggedTab = new Button("Logged in");

        msTab.setStyle(activeTabStyle());
        elyTab.setStyle(inactiveTabStyle());
        loggedTab.setStyle(inactiveTabStyle());

        HBox tabBar = new HBox(0, msTab, elyTab, loggedTab);

        VBox msPanel     = buildMicrosoftPanel();
        VBox elyPanel    = buildElyByPanel();
        VBox loggedPanel = buildLoggedInPanel();

        elyPanel.setVisible(false);    elyPanel.setManaged(false);
        loggedPanel.setVisible(false); loggedPanel.setManaged(false);

        VBox content = new VBox(msPanel, elyPanel, loggedPanel);

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

        Label info = new Label("Click Login to get a code, then enter it at microsoft.com/devicelogin");
        info.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        info.setWrapText(true);

        Label codeLabel = new Label();
        codeLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 28; -fx-font-family: 'JetBrains Mono'; -fx-font-weight: bold;");
        codeLabel.setVisible(false);

        Button copyBtn = new Button("Copy Code");
        copyBtn.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #666666; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 6 12; -fx-border-color: #2a2a2a; -fx-border-radius: 6; -fx-background-radius: 6;");
        copyBtn.setVisible(false);
        copyBtn.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(codeLabel.getText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            copyBtn.setText("Copied!");
        });

        Label status = new Label();
        status.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        status.setWrapText(true);

        Button loginBtn = new Button("Login with Microsoft");
        loginBtn.setStyle(primaryBtnStyle());
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> {
            loginBtn.setDisable(true);
            loginBtn.setText("Getting code...");
            status.setText("");
            new Thread(() -> {
                try {
                    AccountManager.Account account = MicrosoftAuth.login(msg -> Platform.runLater(() -> {
                        if (msg.contains("enter code:")) {
                            String code = msg.split("enter code: ")[1].trim();
                            codeLabel.setText(code);
                            codeLabel.setVisible(true);
                            copyBtn.setVisible(true);
                            status.setText("Enter the code above at microsoft.com/devicelogin");
                            loginBtn.setText("Waiting for login...");
                        } else {
                            status.setText(msg);
                        }
                    }));
                    accountManager.addAccount(account);
                    Platform.runLater(() -> {
                        status.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
                        status.setText("Logged in as " + account.username);
                        codeLabel.setVisible(false);
                        copyBtn.setVisible(false);
                        loginBtn.setText("Login with Microsoft");
                        loginBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        status.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
                        status.setText("Error: " + ex.getMessage());
                        codeLabel.setVisible(false);
                        copyBtn.setVisible(false);
                        loginBtn.setText("Login with Microsoft");
                        loginBtn.setDisable(false);
                    });
                }
            }).start();
        });

        panel.getChildren().addAll(info, loginBtn, codeLabel, copyBtn, status);
        return panel;
    }

    private VBox buildElyByPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(16, 0, 0, 0));

        Label info = new Label("Sign in with your Ely.by account.");
        info.setStyle("-fx-text-fill: #555555; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username or Email");
        usernameField.setStyle("-fx-background-color: #141414; -fx-text-fill: #888888; -fx-prompt-text-fill: #333333; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-border-color: #1a1a1a; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle("-fx-background-color: #141414; -fx-text-fill: #888888; -fx-prompt-text-fill: #333333; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-border-color: #1a1a1a; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12;");

        Label status = new Label();
        status.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
        status.setWrapText(true);

        Button loginBtn = new Button("Login with Ely.by");
        loginBtn.setStyle(primaryBtnStyle());
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            if (username.isEmpty() || password.isEmpty()) {
                status.setText("Please enter your username and password.");
                return;
            }
            loginBtn.setDisable(true);
            loginBtn.setText("Logging in...");
            new Thread(() -> {
                try {
                    AccountManager.Account account = ElyByAuth.login(username, password);
                    accountManager.addAccount(account);
                    Platform.runLater(() -> {
                        status.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
                        status.setText("Logged in as " + account.username);
                        loginBtn.setText("Login with Ely.by");
                        loginBtn.setDisable(false);
                        usernameField.clear();
                        passwordField.clear();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        status.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
                        status.setText("Error: " + ex.getMessage());
                        loginBtn.setText("Login with Ely.by");
                        loginBtn.setDisable(false);
                    });
                }
            }).start();
        });

        panel.getChildren().addAll(info, usernameField, passwordField, loginBtn, status);
        return panel;
    }

    private VBox buildLoggedInPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(16, 0, 0, 0));
        return panel;
    }

    private void refreshLoggedIn(VBox panel) {
        panel.getChildren().clear();
        for (AccountManager.Account acc : accountManager.getAccounts()) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #141414; -fx-background-radius: 6; -fx-padding: 8 12;");

            Label name = new Label(acc.username + " (" + acc.type + ")");
            name.setStyle("-fx-text-fill: #666666; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
            HBox.setHgrow(name, Priority.ALWAYS);

            Button selectBtn = new Button("Select");
            selectBtn.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #555555; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 10; -fx-cursor: hand; -fx-padding: 4 8; -fx-border-color: #2a2a2a; -fx-border-radius: 4; -fx-background-radius: 4;");
            selectBtn.setOnAction(e -> {
                accountManager.setSelected(acc.uuid);
                selectBtn.setText("Selected");
            });

            Button removeBtn = new Button("Remove");
            removeBtn.setStyle("-fx-background-color: #1a0000; -fx-text-fill: #663333; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 10; -fx-cursor: hand; -fx-padding: 4 8; -fx-border-color: #2a0000; -fx-border-radius: 4; -fx-background-radius: 4;");
            removeBtn.setOnAction(e -> {
                accountManager.removeAccount(acc.uuid);
                refreshLoggedIn(panel);
            });

            row.getChildren().addAll(name, selectBtn, removeBtn);
            panel.getChildren().add(row);
        }

        if (accountManager.getAccounts().isEmpty()) {
            Label empty = new Label("No accounts logged in.");
            empty.setStyle("-fx-text-fill: #333333; -fx-font-size: 11; -fx-font-family: 'JetBrains Mono';");
            panel.getChildren().add(empty);
        }
    }

    private String activeTabStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-cursor: hand; -fx-border-color: transparent transparent #ffffff transparent; -fx-border-width: 0 0 1 0; -fx-padding: 6 12; -fx-background-radius: 0;";
    }

    private String inactiveTabStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #444444; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 12; -fx-cursor: hand; -fx-border-color: transparent; -fx-padding: 6 12; -fx-background-radius: 0;";
    }

    private String primaryBtnStyle() {
        return "-fx-background-color: #ffffff; -fx-text-fill: #000000; -fx-font-family: 'JetBrains Mono'; -fx-font-size: 13; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 12 24; -fx-background-radius: 6;";
    }
}
