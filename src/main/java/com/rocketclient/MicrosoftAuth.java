package com.rocketclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MicrosoftAuth {

    private static final String CLIENT_ID    = "0a27dd74-6ba6-4c1b-9e21-c9ea44b91b64";
    private static final String REDIRECT_URI = "http://localhost:9876/auth";
    private static final String SCOPE        = "XboxLive.signin offline_access";
    private static final String AUTH_URL     = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";
    private static final String TOKEN_URL    = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String XBL_URL      = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_URL     = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MC_AUTH_URL  = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MC_PROFILE   = "https://api.minecraftservices.com/minecraft/profile";
    private static final Gson   GSON         = new Gson();

    public static CompletableFuture<AccountManager.Account> login() {
        CompletableFuture<AccountManager.Account> future = new CompletableFuture<>();

        Thread thread = new Thread(() -> {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(9876), 0);
                server.createContext("/auth", exchange -> {
                    try {
                        String query = exchange.getRequestURI().getQuery();
                        Map<String, String> params = parseQuery(query);

                        if (params.containsKey("error")) {
                            String html = "<html><body style='background:#080404;color:#f44336;font-family:monospace;text-align:center;padding:80px'>" +
                                "<h2>Login failed</h2><p>" + params.get("error") + "</p></body></html>";
                            exchange.sendResponseHeaders(200, html.getBytes().length);
                            exchange.getResponseBody().write(html.getBytes());
                            exchange.getResponseBody().close();
                            server.stop(0);
                            future.completeExceptionally(new Exception(params.get("error")));
                            return;
                        }

                        String code = params.get("code");
                        if (code == null) {
                            future.completeExceptionally(new Exception("No code received"));
                            server.stop(0);
                            return;
                        }

                        String html = "<html><body style='background:#080404;color:#ffffff;font-family:monospace;text-align:center;padding:80px'>" +
                            "<h2 style='color:#4caf50'>Login successful!</h2>" +
                            "<p style='color:#555'>You can close this tab and return to Rocket Client.</p></body></html>";
                        exchange.sendResponseHeaders(200, html.getBytes().length);
                        exchange.getResponseBody().write(html.getBytes());
                        exchange.getResponseBody().close();
                        server.stop(0);

                        AccountManager.Account account = exchangeCode(code);
                        future.complete(account);

                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
                server.start();

                String authUrl = AUTH_URL +
                    "?client_id=" + CLIENT_ID +
                    "&response_type=code" +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) +
                    "&prompt=select_account";

                BrowserUtil.open(authUrl);

            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        thread.setDaemon(true);
        thread.start();

        return future;
    }

    private static AccountManager.Account exchangeCode(String code) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("client_id",    CLIENT_ID);
        params.put("code",         code);
        params.put("redirect_uri", REDIRECT_URI);
        params.put("grant_type",   "authorization_code");
        params.put("scope",        SCOPE);

        String responseStr = post(TOKEN_URL, params);
        System.out.println("Token response: " + responseStr);
        JsonObject response = GSON.fromJson(responseStr, JsonObject.class);

        if (response.has("error")) {
            throw new Exception("Token error: " + response.get("error").getAsString());
        }

        if (!response.has("access_token")) {
            throw new Exception("No access_token: " + responseStr);
        }

        String msAccessToken = response.get("access_token").getAsString();
        return exchangeForMinecraft(msAccessToken);
    }

    private static AccountManager.Account exchangeForMinecraft(String msAccessToken) throws Exception {
        // XBL — use d= prefix for AAD tokens
        JsonObject xblBody  = new JsonObject();
        JsonObject xblProps = new JsonObject();
        xblProps.addProperty("AuthMethod", "RPS");
        xblProps.addProperty("SiteName",   "user.auth.xboxlive.com");
        xblProps.addProperty("RpsTicket",  "d=" + msAccessToken);
        xblBody.add("Properties",          xblProps);
        xblBody.addProperty("RelyingParty", "http://auth.xboxlive.com");
        xblBody.addProperty("TokenType",    "JWT");

        String xblStr  = postJson(XBL_URL, xblBody.toString());
        System.out.println("XBL: " + xblStr);
        JsonObject xblJson = GSON.fromJson(xblStr, JsonObject.class);

        if (!xblJson.has("Token")) {
            throw new Exception("XBL failed: " + xblStr);
        }

        String xblToken = xblJson.get("Token").getAsString();
        String userHash = xblJson.getAsJsonObject("DisplayClaims")
            .getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

        // XSTS
        JsonObject xstsBody  = new JsonObject();
        JsonObject xstsProps = new JsonObject();
        xstsProps.addProperty("SandboxId", "RETAIL");
        com.google.gson.JsonArray tokenArr = new com.google.gson.JsonArray();
        tokenArr.add(xblToken);
        xstsProps.add("UserTokens", tokenArr);
        xstsBody.add("Properties",          xstsProps);
        xstsBody.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        xstsBody.addProperty("TokenType",    "JWT");

        String xstsStr  = postJson(XSTS_URL, xstsBody.toString());
        System.out.println("XSTS: " + xstsStr);
        JsonObject xstsJson = GSON.fromJson(xstsStr, JsonObject.class);

        if (!xstsJson.has("Token")) {
            throw new Exception("XSTS failed: " + xstsStr);
        }

        String xstsToken = xstsJson.get("Token").getAsString();

        // MC auth
        JsonObject mcBody = new JsonObject();
        mcBody.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);
        String mcStr = postJson(MC_AUTH_URL, mcBody.toString());
        System.out.println("MC: " + mcStr);
        JsonObject mcJson = GSON.fromJson(mcStr, JsonObject.class);

        if (!mcJson.has("access_token")) {
            // Check if user owns Minecraft
            if (mcJson.has("errorMessage")) {
                throw new Exception(mcJson.get("errorMessage").getAsString());
            }
            throw new Exception("MC auth failed: " + mcStr);
        }

        String mcAccessToken = mcJson.get("access_token").getAsString();

        // Profile
        String profileStr = getWithAuth(MC_PROFILE, mcAccessToken);
        System.out.println("Profile: " + profileStr);
        JsonObject profile = GSON.fromJson(profileStr, JsonObject.class);

        if (!profile.has("id")) {
            throw new Exception("This account does not own Minecraft Java Edition.");
        }

        String uuid     = profile.get("id").getAsString();
        String username = profile.get("name").getAsString();

        return new AccountManager.Account("microsoft", uuid, username, mcAccessToken, null);
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private static String post(String urlStr, Map<String, String> params) throws Exception {
        String body = params.entrySet().stream()
            .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                      URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String postJson(String urlStr, String json) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept",       "application/json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));

        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String getWithAuth(String urlStr, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}
