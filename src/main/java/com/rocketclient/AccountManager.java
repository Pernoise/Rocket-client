package com.rocketclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class AccountManager {

    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), ".rocketclient");
    private static final Path ACCOUNTS_FILE = DATA_DIR.resolve("accounts.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class Account {
        public String type;
        public String uuid;
        public String username;
        public String accessToken;
        public String clientToken;

        public Account(String type, String uuid, String username, String accessToken, String clientToken) {
            this.type = type;
            this.uuid = uuid;
            this.username = username;
            this.accessToken = accessToken;
            this.clientToken = clientToken;
        }
    }

    private List<Account> accounts = new ArrayList<>();
    private String selectedUuid = null;

    public AccountManager() {
        load();
    }

    private void load() {
        try {
            if (Files.exists(ACCOUNTS_FILE)) {
                String json = new String(Files.readAllBytes(ACCOUNTS_FILE));
                JsonObject root = GSON.fromJson(json, JsonObject.class);
                if (root.has("selected")) {
                    selectedUuid = root.get("selected").getAsString();
                }
                if (root.has("accounts")) {
                    JsonArray arr = root.getAsJsonArray("accounts");
                    for (JsonElement el : arr) {
                        Account a = GSON.fromJson(el, Account.class);
                        accounts.add(a);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Could not load accounts: " + e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(DATA_DIR);
            JsonObject root = new JsonObject();
            root.addProperty("selected", selectedUuid);
            root.add("accounts", GSON.toJsonTree(accounts));
            Files.write(ACCOUNTS_FILE, GSON.toJson(root).getBytes());
        } catch (Exception e) {
            System.out.println("Could not save accounts: " + e.getMessage());
        }
    }

    public void addAccount(Account account) {
        accounts.removeIf(a -> a.uuid.equals(account.uuid));
        accounts.add(account);
        if (selectedUuid == null) {
            selectedUuid = account.uuid;
        }
        save();
    }

    public void removeAccount(String uuid) {
        accounts.removeIf(a -> a.uuid.equals(uuid));
        if (uuid.equals(selectedUuid)) {
            selectedUuid = accounts.isEmpty() ? null : accounts.get(0).uuid;
        }
        save();
    }

    public void setSelected(String uuid) {
        this.selectedUuid = uuid;
        save();
    }

    public Account getSelected() {
        if (selectedUuid == null) return null;
        return accounts.stream()
            .filter(a -> a.uuid.equals(selectedUuid))
            .findFirst()
            .orElse(null);
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public boolean hasAccounts() {
        return !accounts.isEmpty();
    }
}
