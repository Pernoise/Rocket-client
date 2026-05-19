package com.rocketclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ElyByAuth {

    private static final String AUTH_URL = "https://authserver.ely.by/auth/authenticate";
    private static final Gson GSON = new Gson();

    public static AccountManager.Account login(String username, String password) throws Exception {
        JsonObject agent = new JsonObject();
        agent.addProperty("name", "Minecraft");
        agent.addProperty("version", 1);

        JsonObject payload = new JsonObject();
        payload.add("agent", agent);
        payload.addProperty("username", username);
        payload.addProperty("password", password);
        payload.addProperty("clientToken", UUID.randomUUID().toString());
        payload.addProperty("requestUser", true);

        String responseBody = post(AUTH_URL, payload.toString());
        JsonObject response = GSON.fromJson(responseBody, JsonObject.class);

        if (response.has("error")) {
            String error = response.get("errorMessage").getAsString();
            throw new Exception(error);
        }

        String accessToken = response.get("accessToken").getAsString();
        String clientToken = response.get("clientToken").getAsString();
        JsonObject selectedProfile = response.getAsJsonObject("selectedProfile");
        String uuid = selectedProfile.get("id").getAsString();
        String name = selectedProfile.get("name").getAsString();

        return new AccountManager.Account("elyby", uuid, name, accessToken, clientToken);
    }

    private static String post(String urlStr, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        java.io.InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        conn.disconnect();
        return response;
    }
}
