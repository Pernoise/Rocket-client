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

    // Account tokens live in their own clearly-labeled folder instead of loose in
    // the main data dir, so it's obvious at a glance what NOT to screenshot/zip/share.
    private static final Path ACCOUNTS_DIR = DATA_DIR.resolve("DO-NOT-SHARE-YOUR-ACCOUNT-DATA");
    private static final Path ACCOUNTS_FILE = ACCOUNTS_DIR.resolve("accounts.json");
    private static final Path KEY_FILE = ACCOUNTS_DIR.resolve(".key");
    private static final Path OLD_ACCOUNTS_FILE = DATA_DIR.resolve("accounts.json"); // pre-encryption location
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String WARNING_TEXT =
        "DO NOT SHARE THIS FOLDER OR ITS CONTENTS WITH ANYONE.\n\n" +
        "accounts.json contains your login session tokens. Anyone who has this file\n" +
        "can sign in as you, the same as if you handed them your password - even\n" +
        "though the values inside are encrypted at rest, they are still decryptable\n" +
        "by this launcher (and by whoever holds .key) and should be treated as secrets.\n\n" +
        "Never paste the contents of accounts.json into Discord, a support ticket,\n" +
        "a screen share, or a bug report. If you think this file has leaked, remove\n" +
        "the affected account from Accounts > Logged in and change your password.\n";

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
            Path source = Files.exists(ACCOUNTS_FILE) ? ACCOUNTS_FILE
                : (Files.exists(OLD_ACCOUNTS_FILE) ? OLD_ACCOUNTS_FILE : null);
            if (source == null) return;

            String json = new String(Files.readAllBytes(source));
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root.has("selected") && !root.get("selected").isJsonNull()) {
                selectedUuid = root.get("selected").getAsString();
            }
            if (root.has("accounts")) {
                JsonArray arr = root.getAsJsonArray("accounts");
                for (JsonElement el : arr) {
                    Account a = GSON.fromJson(el, Account.class);
                    // Values from the old plaintext file pass through decrypt() unchanged
                    // (no "enc:v1:" prefix), and get encrypted on the very next save().
                    a.accessToken = CryptoUtil.decrypt(a.accessToken, KEY_FILE);
                    a.clientToken = CryptoUtil.decrypt(a.clientToken, KEY_FILE);
                    accounts.add(a);
                }
            }

            if (source.equals(OLD_ACCOUNTS_FILE)) {
                save(); // writes into the new encrypted/warned location
                Files.deleteIfExists(OLD_ACCOUNTS_FILE);
            }
        } catch (Exception e) {
            System.out.println("Could not load accounts: " + e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(ACCOUNTS_DIR);
            writeWarningReadme();

            JsonObject root = new JsonObject();
            root.addProperty("selected", selectedUuid);

            JsonArray arr = new JsonArray();
            for (Account a : accounts) {
                Account enc = new Account(a.type, a.uuid, a.username,
                    CryptoUtil.encrypt(a.accessToken, KEY_FILE),
                    CryptoUtil.encrypt(a.clientToken, KEY_FILE));
                arr.add(GSON.toJsonTree(enc));
            }
            root.add("accounts", arr);

            Files.write(ACCOUNTS_FILE, GSON.toJson(root).getBytes());
        } catch (Exception e) {
            System.out.println("Could not save accounts: " + e.getMessage());
        }
    }

    private void writeWarningReadme() {
        try {
            Path readme = ACCOUNTS_DIR.resolve("README - DO NOT SHARE.txt");
            if (!Files.exists(readme)) {
                Files.write(readme, WARNING_TEXT.getBytes());
            }
        } catch (Exception e) {
            System.out.println("Could not write account folder warning: " + e.getMessage());
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
