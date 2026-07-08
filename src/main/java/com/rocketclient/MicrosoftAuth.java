package com.rocketclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MicrosoftAuth {

    private static final String CLIENT_ID    = "dc74ccfb-45bd-41c1-b948-1462a516e4c6";
    private static final String REDIRECT_URI = "https://login.microsoftonline.com/common/oauth2/nativeclient";
    private static final Gson   GSON         = new Gson();

    public static AccountManager.Account login(Consumer<String> statusCallback) throws Exception {
        CompletableFuture<String> codeFuture = new CompletableFuture<>();

        String authUrl =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize" +
            "?client_id=" + CLIENT_ID +
            "&response_type=code" +
            "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8") +
            "&scope=" + URLEncoder.encode("XboxLive.signin offline_access", "UTF-8") +
            "&prompt=select_account";

        System.setProperty("javax.net.ssl.trustStore", System.getProperty("java.home") + "\\lib\\security\\cacerts"); Platform.runLater(() -> {
            Stage browserStage = new Stage();
            browserStage.initModality(Modality.APPLICATION_MODAL);
            browserStage.setTitle("Login with Microsoft");
            browserStage.setWidth(500);
            browserStage.setHeight(650);

            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();

            engine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
                if (newUrl != null && newUrl.startsWith(REDIRECT_URI)) {
                    try {
                        String query = new URL(newUrl).getQuery();
                        if (query != null) {
                            for (String param : query.split("&")) {
                                if (param.startsWith("code=")) {
                                    String code = URLDecoder.decode(param.substring(5), "UTF-8");
                                    engine.load("about:blank");
                                    codeFuture.complete(code);
                                    browserStage.close();
                                    break;
                                }
                            }
                        }
                        if (newUrl.contains("error=")) {
                            codeFuture.completeExceptionally(new Exception("Login cancelled or failed"));
                            browserStage.close();
                        }
                    } catch (Exception e) {
                        codeFuture.completeExceptionally(e);
                        browserStage.close();
                    }
                }
            });

            browserStage.setOnCloseRequest(e -> {
                if (!codeFuture.isDone()) {
                    codeFuture.completeExceptionally(new Exception("Login window closed"));
                }
            });

            engine.load(authUrl);
            browserStage.setScene(new Scene(webView));
            browserStage.show();
        });

        statusCallback.accept("Complete login in the browser window...");
        String code = codeFuture.get();
        statusCallback.accept("Authenticating...");
        return exchangeCodeForAccount(code);
    }

    private static AccountManager.Account exchangeCodeForAccount(String code) throws Exception {
        String tokenBody =
            "client_id=" + CLIENT_ID +
            "&code=" + URLEncoder.encode(code, "UTF-8") +
            "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8") +
            "&grant_type=authorization_code";

        JsonObject tokenResponse = post(
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
            tokenBody, "application/x-www-form-urlencoded", null
        );

        if (tokenResponse.has("error")) {
            throw new Exception(tokenResponse.get("error_description").getAsString());
        }

        String accessToken  = tokenResponse.get("access_token").getAsString();
        String refreshToken = tokenResponse.has("refresh_token") ? tokenResponse.get("refresh_token").getAsString() : "";

        JsonObject xblBody = new JsonObject();
        JsonObject props = new JsonObject();
        props.addProperty("AuthMethod", "RPS");
        props.addProperty("SiteName", "user.auth.xboxlive.com");
        props.addProperty("RpsTicket", "d=" + accessToken);
        xblBody.add("Properties", props);
        xblBody.addProperty("RelyingParty", "http://auth.xboxlive.com");
        xblBody.addProperty("TokenType", "JWT");

        JsonObject xblResponse = post("https://user.auth.xboxlive.com/user/authenticate", GSON.toJson(xblBody), "application/json", null);
        String xblToken = xblResponse.get("Token").getAsString();
        String userHash = xblResponse.getAsJsonObject("DisplayClaims")
            .getAsJsonArray("xui").get(0).getAsJsonObject()
            .get("uhs").getAsString();

        JsonObject xstsBody = new JsonObject();
        JsonObject xstsProps = new JsonObject();
        xstsProps.addProperty("SandboxId", "RETAIL");
        com.google.gson.JsonArray tokens = new com.google.gson.JsonArray();
        tokens.add(xblToken);
        xstsProps.add("UserTokens", tokens);
        xstsBody.add("Properties", xstsProps);
        xstsBody.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        xstsBody.addProperty("TokenType", "JWT");

        JsonObject xstsResponse = post("https://xsts.auth.xboxlive.com/xsts/authorize", GSON.toJson(xstsBody), "application/json", null);
        String xstsToken = xstsResponse.get("Token").getAsString();

        JsonObject mcBody = new JsonObject();
        mcBody.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);
        JsonObject mcResponse = post("https://api.minecraftservices.com/authentication/login_with_xbox", GSON.toJson(mcBody), "application/json", null);
        String mcAccessToken = mcResponse.get("access_token").getAsString();

        JsonObject profile = get("https://api.minecraftservices.com/minecraft/profile", mcAccessToken);
        String uuid     = profile.get("id").getAsString();
        String username = profile.get("name").getAsString();

        return new AccountManager.Account("microsoft", uuid, username, mcAccessToken, refreshToken);
    }

    private static JsonObject post(String url, String body, String contentType, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Accept", "application/json");
        if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return GSON.fromJson(new String(is.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
    }

    private static JsonObject get(String url, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return GSON.fromJson(new String(is.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
    }
}
