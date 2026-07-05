package com.rocketclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class MicrosoftAuth {

    private static final String CLIENT_ID = "dc74ccfb-45bd-41c1-b948-1462a516e4c6";
    private static final Gson GSON = new Gson();

    public static AccountManager.Account login(Consumer<String> statusCallback) throws Exception {
        String deviceCodeUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
        String body = "client_id=" + CLIENT_ID + "&scope=" + URLEncoder.encode("XboxLive.signin offline_access", "UTF-8");

        JsonObject deviceResponse = post(deviceCodeUrl, body, "application/x-www-form-urlencoded", null);
        System.out.println("Device response: " + GSON.toJson(deviceResponse));

        String userCode   = deviceResponse.get("user_code").getAsString();
        String deviceCode = deviceResponse.get("device_code").getAsString();
        String verifyUrl  = deviceResponse.get("verification_uri").getAsString();
        int interval      = deviceResponse.has("interval") ? deviceResponse.get("interval").getAsInt() : 5;

        statusCallback.accept("Go to " + verifyUrl + " and enter code: " + userCode);
        BrowserUtil.open(verifyUrl);

        String tokenUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
        String pollBody = "client_id=" + CLIENT_ID +
            "&grant_type=urn:ietf:params:oauth:grant-type:device_code" +
            "&device_code=" + URLEncoder.encode(deviceCode, "UTF-8");

        for (int i = 0; i < 60; i++) {
            Thread.sleep(interval * 1000L);
            JsonObject tokenResponse = post(tokenUrl, pollBody, "application/x-www-form-urlencoded", null);

            if (tokenResponse.has("access_token")) {
                String accessToken  = tokenResponse.get("access_token").getAsString();
                String refreshToken = tokenResponse.has("refresh_token") ? tokenResponse.get("refresh_token").getAsString() : "";
                statusCallback.accept("Authenticating with Xbox...");
                return exchangeForAccount(accessToken, refreshToken);
            }

            if (tokenResponse.has("error")) {
                String error = tokenResponse.get("error").getAsString();
                if (error.equals("authorization_pending")) continue;
                if (error.equals("slow_down")) { Thread.sleep(5000); continue; }
                throw new Exception(tokenResponse.has("error_description") ?
                    tokenResponse.get("error_description").getAsString() : error);
            }
        }

        throw new Exception("Login timed out.");
    }

    private static AccountManager.Account exchangeForAccount(String accessToken, String refreshToken) throws Exception {
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
