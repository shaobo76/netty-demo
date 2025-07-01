package com.kilo.transfer.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

public class HashUtil {

    /**
     * Calculates the CRC32 checksum for a byte array.
     *
     * @param bytes The data to checksum.
     * @return The CRC32 checksum as a hex string.
     */
    public static String crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return Long.toHexString(crc.getValue());
    }

    /**
     * Calculates the SHA-256 hash for a file.
     *
     * @param path The path to the file.
     * @return The SHA-256 hash as a hex string.
     * @throws IOException If an I/O error occurs reading the file.
     * @throws NoSuchAlgorithmException If the SHA-256 algorithm is not available.
     */
    public static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                sha256.update(buffer, 0, bytesRead);
            }
        }
        return bytesToHex(sha256.digest());
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}