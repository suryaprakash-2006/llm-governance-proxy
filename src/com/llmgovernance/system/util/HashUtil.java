package com.llmgovernance.system.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HashUtil – SHA-256 hashing utility for integrity verification.
 *
 * Used to:
 *   1. Hash the original text before compression.
 *   2. Hash the decompressed text after decompression.
 *   3. Compare both hashes to verify data integrity.
 */
public class HashUtil {

    // ── SHA-256 ───────────────────────────────────────────────────────────────

    /**
     * Computes the SHA-256 hash of the given text.
     * @param text Input string (UTF-8 encoded).
     * @return Lowercase hexadecimal hash string (64 chars), or empty on error.
     */
    public static String sha256(String text) {
        if (text == null) text = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JVM spec – this should never happen.
            System.err.println("[HashUtil] SHA-256 not available: " + e.getMessage());
            return "";
        }
    }

    // ── Integrity check ───────────────────────────────────────────────────────

    /**
     * Compares two hash strings for equality (case-insensitive).
     * @return true if hashes match (data integrity confirmed).
     */
    public static boolean verify(String hashA, String hashB) {
        if (hashA == null || hashB == null) return false;
        return hashA.equalsIgnoreCase(hashB);
    }

    /**
     * Returns a formatted integrity report.
     * @param originalHash    Hash of the original text.
     * @param decompressedHash Hash of the decompressed text.
     */
    public static String integrityReport(String originalHash, String decompressedHash) {
        boolean ok = verify(originalHash, decompressedHash);
        return "Original Hash    : " + originalHash + "\n"
             + "Decompressed Hash: " + decompressedHash + "\n"
             + "Integrity        : " + (ok ? "✅ MATCH – Data is intact"
                                           : "❌ MISMATCH – Data may be corrupted");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
