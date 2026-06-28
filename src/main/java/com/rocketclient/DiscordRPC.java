package com.rocketclient;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

public class DiscordRPC {

    private static final String APP_ID  = "1505307149201576066";
    private static final Gson   GSON    = new Gson();
    private static SocketChannel      socket = null;
    private static RandomAccessFile   pipe   = null;
    private static boolean running           = false;
    private static boolean isWindows         = System.getProperty("os.name").toLowerCase().contains("win");
    private static int nonce                 = 1;

    public static void start(SettingsManager settings) {
        if (!settings.discordRpc) return;
        if (running) return;

        Thread thread = new Thread(() -> {
            try {
                if (isWindows) {
                    pipe = connectToDiscordWindows();
                    if (pipe == null) {
                        System.out.println("Discord not running, RPC skipped.");
                        return;
                    }
                } else {
                    socket = connectToDiscordUnix();
                    if (socket == null) {
                        System.out.println("Discord not running, RPC skipped.");
                        return;
                    }
                }

                JsonObject handshake = new JsonObject();
                handshake.addProperty("v",         3);
                handshake.addProperty("client_id", APP_ID);
                send(0, handshake);
                read();
                running = true;
                System.out.println("Discord RPC connected.");

                setPresence("In the launcher", "Rocket Client Beta v0.4");

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

    public static synchronized void setPresence(String details, String state) {
        if (!running) return;
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
        setPresence("Playing Minecraft " + version, "Rocket Client Beta v0.4");
    }

    public static void stop() {
        running = false;
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        try { if (pipe   != null) pipe.close();   } catch (Exception ignored) {}
    }

    private static RandomAccessFile connectToDiscordWindows() {
        for (int i = 0; i < 10; i++) {
            try {
                return new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static SocketChannel connectToDiscordUnix() {
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
                    java.net.UnixDomainSocketAddress addr = java.net.UnixDomainSocketAddress.of(path);
                    SocketChannel ch = SocketChannel.open(java.net.StandardProtocolFamily.UNIX);
                    ch.connect(addr);
                    return ch;
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static synchronized void send(int opcode, JsonObject payload) throws Exception {
        byte[] data = GSON.toJson(payload).getBytes("UTF-8");
        ByteBuffer buf = ByteBuffer.allocate(8 + data.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(opcode);
        buf.putInt(data.length);
        buf.put(data);

        if (isWindows && pipe != null) {
            pipe.write(buf.array());
        } else if (socket != null) {
            buf.flip();
            socket.write(buf);
        }
    }

    private static synchronized String read() throws Exception {
        if (isWindows && pipe != null) {
            byte[] header = new byte[8];
            pipe.readFully(header);
            ByteBuffer hbuf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            hbuf.getInt();
            int length = hbuf.getInt();
            byte[] body = new byte[length];
            pipe.readFully(body);
            return new String(body, "UTF-8");
        } else {
            ByteBuffer header = ByteBuffer.allocate(8);
            header.order(ByteOrder.LITTLE_ENDIAN);
            socket.read(header);
            header.flip();
            header.getInt();
            int length = header.getInt();
            ByteBuffer body = ByteBuffer.allocate(length);
            socket.read(body);
            return new String(body.array(), "UTF-8");
        }
    }
}
