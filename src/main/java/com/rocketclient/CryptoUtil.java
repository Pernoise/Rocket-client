package com.rocketclient;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * AES-256-GCM encryption for secrets we persist to disk (access tokens, client tokens).
 * Not a defense against someone with full access to the machine and this launcher's
 * install - the key lives next to the data it protects, same as most desktop apps
 * (browsers, other launchers). What it does protect against is the token showing up
 * in plaintext if someone glances at the file, screenshots their folder, pastes it
 * into a support ticket, or a backup/sync tool scoops the directory up unencrypted.
 */
public class CryptoUtil {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int KEY_SIZE_BITS = 256;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String ENC_PREFIX = "enc:v1:";

    private static SecretKey cachedKey;

    private static synchronized SecretKey key(Path keyFile) {
        if (cachedKey != null) return cachedKey;
        try {
            if (Files.exists(keyFile)) {
                byte[] raw = Base64.getDecoder().decode(Files.readAllBytes(keyFile));
                cachedKey = new SecretKeySpec(raw, "AES");
            } else {
                KeyGenerator kg = KeyGenerator.getInstance("AES");
                kg.init(KEY_SIZE_BITS, new SecureRandom());
                cachedKey = kg.generateKey();
                Files.createDirectories(keyFile.getParent());
                Files.write(keyFile, Base64.getEncoder().encode(cachedKey.getEncoded()));
                lockDownPermissions(keyFile);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load/create encryption key", e);
        }
        return cachedKey;
    }

    private static void lockDownPermissions(Path file) {
        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(file, perms);
        } catch (UnsupportedOperationException e) {
            // Windows has no POSIX permission model - NTFS ACLs already restrict
            // this to the owning user by default, so there's nothing extra to do.
        } catch (Exception e) {
            System.out.println("Could not restrict key file permissions: " + e.getMessage());
        }
    }

    /** Encrypts plaintext, returning a self-describing base64 string safe to store in JSON. */
    public static String encrypt(String plaintext, Path keyFile) {
        if (plaintext == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGO);
            byte[] iv = new byte[GCM_IV_BYTES];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key(keyFile), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes("UTF-8"));

            ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
            buf.put(iv).put(ct);
            return ENC_PREFIX + Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            System.out.println("Encryption failed, refusing to store plaintext token: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Decrypts a value produced by encrypt(). If the value doesn't carry our prefix
     * (e.g. it's an older plaintext token from before encryption was added), it's
     * returned as-is so existing logins keep working until the next save re-encrypts it.
     */
    public static String decrypt(String stored, Path keyFile) {
        if (stored == null) return null;
        if (!stored.startsWith(ENC_PREFIX)) return stored;
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(ENC_PREFIX.length()));
            byte[] iv = new byte[GCM_IV_BYTES];
            byte[] ct = new byte[all.length - GCM_IV_BYTES];
            System.arraycopy(all, 0, iv, 0, GCM_IV_BYTES);
            System.arraycopy(all, GCM_IV_BYTES, ct, 0, ct.length);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key(keyFile), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ct), "UTF-8");
        } catch (Exception e) {
            System.out.println("Could not decrypt stored token (key changed or data corrupt): " + e.getMessage());
            return null;
        }
    }
}
