package com.rocketclient;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import java.io.*;
import java.net.UnixDomainSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

public class DiscordRPC {

    private static final String APP_ID  = "1505307149201576066";
    private static final Gson   GSON    = new Gson();
    private static SocketChannel socket = null;
    private static boolean running      = false;
    private static int nonce            = 1;

    public static void start(SettingsManager settings) {
        if (!settings.discordRpc) return;
        if (running) return;

        Thread thread = new Thread(() -> {
            try {
                socket = connectToDiscord();
                if (socket == null) {
                    System.out.println("Discord not running, RPC skipped.");
                    return;
                }

                // Handshake
                JsonObject handshake = new JsonObject();
                handshake.addProperty("v",        1);
                handshake.addProperty("client_id", APP_ID);
                send(0, handshake);

                // Read handshake response
                read();
                running = true;
                System.out.println("Discord RPC connected.");

                // Set initial presence
                setPresence("In the launcher", "Rocket Client bETA v0.3");

                // Keep alive loop
                while (running) {
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                System.out.println("Discord RPC error: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public static void setPresence(String details, String state) {
        if (!running || socket == null) return;
        try {
            JsonObject activity = new JsonObject();
            activity.addProperty("details", details);
            activity.addProperty("state",   state);

            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", System.currentTimeMillis() / 1000);
            activity.add("timestamps", timestamps);

            JsonObject args = new JsonObject();
            args.addProperty("pid", ProcessHandle.current().pid());
            args.add("activity", activity);

            JsonObject payload = new JsonObject();
            payload.addProperty("cmd",   "SET_ACTIVITY");
            payload.addProperty("nonce", String.valueOf(nonce++));
            payload.add("args", args);

            send(1, payload);
            read();
        } catch (Exception e) {
            System.out.println("Discord RPC presence error: " + e.getMessage());
            running = false;
        }
    }

    public static void updatePlaying(String version) {
        setPresence("Playing Minecraft " + version, "Rocket Client Alpha v0.1");
    }

    public static void stop() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }

    private static SocketChannel connectToDiscord() {
        String[] tmpDirs = {
            System.getenv("XDG_RUNTIME_DIR"),
            System.getenv("TMPDIR"),
            System.getenv("TMP"),
            System.getenv("TEMP"),
            "/tmp"
        };

        for (String dir : tmpDirs) {
            if (dir == null) continue;
            for (int i = 0; i < 10; i++) {
                try {
                    Path path = Path.of(dir, "discord-ipc-" + i);
                    UnixDomainSocketAddress addr = UnixDomainSocketAddress.of(path);
                    SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
                    ch.connect(addr);
                    return ch;
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static void send(int opcode, JsonObject payload) throws Exception {
        byte[] data = GSON.toJson(payload).getBytes("UTF-8");
        ByteBuffer buf = ByteBuffer.allocate(8 + data.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(opcode);
        buf.putInt(data.length);
        buf.put(data);
        buf.flip();
        socket.write(buf);
    }

    private static String read() throws Exception {
        ByteBuffer header = ByteBuffer.allocate(8);
        header.order(ByteOrder.LITTLE_ENDIAN);
        socket.read(header);
        header.flip();
        header.getInt(); // opcode
        int length = header.getInt();
        ByteBuffer body = ByteBuffer.allocate(length);
        socket.read(body);
        return new String(body.array(), "UTF-8");
    }
}
