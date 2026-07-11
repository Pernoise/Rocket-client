package com.rocketclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

public class DiscordRPC {

    private static final String APP_ID = "1505307149201576066";
    private static final Gson GSON = new Gson();
    private static boolean running = false;
    private static int nonce = 1;
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    private static SocketChannel unixSocket = null;
    private static OutputStream pipeOut = null;
    private static InputStream pipeIn = null;

    public static void start(SettingsManager settings) {
        if (!settings.discordRpc) return;
        if (running) return;

        new Thread(() -> {
            try {
                if (IS_WINDOWS) {
                    if (!connectWindows()) {
                        System.out.println("Discord not running, RPC skipped.");
                        return;
                    }
                } else {
                    if (!connectUnix()) {
                        System.out.println("Discord not running, RPC skipped.");
                        return;
                    }
                }

                sendPacket(0, buildHandshake());
                readResponse();
                running = true;
                System.out.println("Discord RPC connected.");
                setPresence("In the launcher", "Rocket Client Beta v0.6");

                while (running) Thread.sleep(15000);

            } catch (Exception e) {
                System.out.println("Discord RPC error: " + e.getMessage());
            }
        }, "discord-rpc").start();
    }

    private static boolean connectWindows() {
        for (int i = 0; i < 10; i++) {
            try {
                String name = "\\\\.\\pipe\\discord-ipc-" + i;
                Process p = Runtime.getRuntime().exec(new String[]{"cmd"});
                RandomAccessFile raf = new RandomAccessFile(name, "rw");
                pipeOut = new FileOutputStream(raf.getFD());
                pipeIn  = new FileInputStream(raf.getFD());
                p.destroy();
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static boolean connectUnix() {
        String[] dirs = { System.getenv("XDG_RUNTIME_DIR"), System.getenv("TMPDIR"), "/tmp" };
        for (String dir : dirs) {
            if (dir == null) continue;
            for (int i = 0; i < 10; i++) {
                try {
                    var addr = java.net.UnixDomainSocketAddress.of(Path.of(dir, "discord-ipc-" + i));
                    unixSocket = SocketChannel.open(java.net.StandardProtocolFamily.UNIX);
                    unixSocket.connect(addr);
                    return true;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    private static JsonObject buildHandshake() {
        JsonObject hs = new JsonObject();
        hs.addProperty("v", 1);
        hs.addProperty("client_id", APP_ID);
        return hs;
    }

    public static synchronized void setPresence(String details, String state) {
        if (!running) return;
        try {
            JsonObject ts = new JsonObject();
            ts.addProperty("start", System.currentTimeMillis() / 1000);

            JsonObject activity = new JsonObject();
            activity.addProperty("details", details);
            activity.addProperty("state", state);
            activity.add("timestamps", ts);

            JsonObject args = new JsonObject();
            args.addProperty("pid", (int) ProcessHandle.current().pid());
            args.add("activity", activity);

            JsonObject payload = new JsonObject();
            payload.addProperty("cmd", "SET_ACTIVITY");
            payload.addProperty("nonce", String.valueOf(nonce++));
            payload.add("args", args);

            sendPacket(1, payload);
            readResponse();
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
        try { if (unixSocket != null) unixSocket.close(); } catch (Exception ignored) {}
        try { if (pipeOut    != null) pipeOut.close();    } catch (Exception ignored) {}
        try { if (pipeIn     != null) pipeIn.close();     } catch (Exception ignored) {}
    }

    private static synchronized void sendPacket(int op, JsonObject payload) throws Exception {
        byte[] data = GSON.toJson(payload).getBytes("UTF-8");
        ByteBuffer buf = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(op);
        buf.putInt(data.length);
        buf.put(data);

        if (IS_WINDOWS) {
            pipeOut.write(buf.array());
            pipeOut.flush();
        } else {
            unixSocket.write(ByteBuffer.wrap(buf.array()));
        }
    }

    private static synchronized String readResponse() throws Exception {
        if (IS_WINDOWS) {
            byte[] header = new byte[8];
            int total = 0;
            while (total < 8) {
                int r = pipeIn.read(header, total, 8 - total);
                if (r == -1) throw new IOException("Pipe closed");
                total += r;
            }
            ByteBuffer hbuf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            hbuf.getInt();
            int len = hbuf.getInt();
            byte[] body = new byte[len];
            total = 0;
            while (total < len) {
                int r = pipeIn.read(body, total, len - total);
                if (r == -1) throw new IOException("Pipe closed");
                total += r;
            }
            String resp = new String(body, "UTF-8");
            System.out.println("Discord: " + resp);
            return resp;
        } else {
            ByteBuffer hdr = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            unixSocket.read(hdr);
            hdr.flip();
            hdr.getInt();
            int len = hdr.getInt();
            ByteBuffer body = ByteBuffer.allocate(len);
            unixSocket.read(body);
            return new String(body.array(), "UTF-8");
        }
    }
}
