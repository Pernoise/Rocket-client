package com.rocketclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MicrosoftAuth {

    private static final String CLIENT_ID    = "dc74ccfb-45bd-41c1-b948-1462a516e4c6";
    private static final String REDIRECT_URI = "http://localhost:9999/callback";
    private static final Gson   GSON         = new Gson();

    public static AccountManager.Account login(Consumer<String> statusCallback) throws Exception {
        CompletableFuture<String> codeFuture = new CompletableFuture<>();

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 9999), 10);
        server.setExecutor(Executors.newFixedThreadPool(2));
        server.createContext("/callback", exchange -> {
            try {
                String query = exchange.getRequestURI().getQuery();
                String code = null;
                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("code=")) {
                            code = URLDecoder.decode(param.substring(5), "UTF-8");
                            break;
                        }
                    }
                }
                String html = "<html><body style='background:#080404;color:#fff;font-family:monospace;display:flex;align-items:center;justify-content:center;height:100vh;margin:0'><h2>Login successful! You can close this window.</h2></body></html>";
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
                if (code != null) codeFuture.complete(code);
                else codeFuture.completeExceptionally(new Exception("No code received"));
            } catch (Exception e) {
                codeFuture.completeExceptionally(e);
            }
        });
        server.start();

        String authUrl =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize" +
            "?client_id=" + CLIENT_ID +
            "&response_type=code" +
            "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8") +
            "&scope=" + URLEncoder.encode("XboxLive.signin offline_access", "UTF-8") +
            "&prompt=select_account";

        BrowserUtil.open(authUrl);
        statusCallback.accept("Complete login in your browser, then return here.");

        String code;
        try {
            code = codeFuture.get(5, TimeUnit.MINUTES);
        } finally {
            server.stop(0);
        }

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
