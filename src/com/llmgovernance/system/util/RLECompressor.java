package com.llmgovernance.system.util;

/**
 * RLECompressor – Run-Length Encoding (RLE) compression utility.
 *
 * Encoding:
 *   Consecutive identical characters are replaced by <count><char>.
 *   Single characters are stored as-is (count "1" is omitted for readability).
 *   Example:  "AAAABBBCC" → "4A3B2C"
 *             "HELLO"     → "HE2L O"   (wait – H,E appear once → "HE2LO")
 *
 * Special characters:
 *   Digits in the original text are escaped as {<digit>} so the decoder
 *   can distinguish them from run-length counts.
 *   Example:  "A3B" → "A{3}B"  (because '3' is a literal, not a count)
 *
 * Decoding:
 *   Sequences of digits followed by a non-digit → expand that character.
 *   {<digit>} sequences → restore the original digit character.
 */
public class RLECompressor {

    // ── Compress ──────────────────────────────────────────────────────────────

    /**
     * Compresses text using Run-Length Encoding.
     * @param text Raw input text (may be null/empty).
     * @return RLE-compressed string.
     */
    public String compress(String text) {
        if (text == null || text.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < text.length()) {
            char c = text.charAt(i);

            // Escape digit characters so decoder won't mistake them for counts
            if (Character.isDigit(c)) {
                result.append('{').append(c).append('}');
                i++;
                continue;
            }

            // Count consecutive occurrences of c
            int count = 1;
            while (i + count < text.length() && text.charAt(i + count) == c) {
                count++;
            }

            if (count > 1) {
                result.append(count).append(c);
            } else {
                result.append(c);
            }
            i += count;
        }

        return result.toString();
    }

    // ── Decompress ────────────────────────────────────────────────────────────

    /**
     * Decompresses an RLE-encoded string back to the original text.
     * @param compressed RLE-encoded input.
     * @return Decompressed original text.
     * @throws IllegalArgumentException if the format is invalid.
     */
    public String decompress(String compressed) {
        if (compressed == null || compressed.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < compressed.length()) {
            char c = compressed.charAt(i);

            // Handle escaped digit: {<digit>}
            if (c == '{') {
                int close = compressed.indexOf('}', i);
                if (close == -1) {
                    throw new IllegalArgumentException(
                            "Invalid RLE format: unmatched '{' at position " + i);
                }
                String inner = compressed.substring(i + 1, close);
                if (inner.length() != 1 || !Character.isDigit(inner.charAt(0))) {
                    throw new IllegalArgumentException(
                            "Invalid escape sequence: {" + inner + "}");
                }
                result.append(inner.charAt(0));
                i = close + 1;
                continue;
            }

            // Read run-length count (sequence of digits)
            if (Character.isDigit(c)) {
                StringBuilder countStr = new StringBuilder();
                while (i < compressed.length() && Character.isDigit(compressed.charAt(i))) {
                    countStr.append(compressed.charAt(i));
                    i++;
                }
                if (i >= compressed.length()) {
                    throw new IllegalArgumentException(
                            "Invalid RLE format: count with no following character.");
                }
                char ch    = compressed.charAt(i);
                int  count = Integer.parseInt(countStr.toString());
                if (count <= 0) {
                    throw new IllegalArgumentException(
                            "Invalid RLE count: " + count);
                }
                result.append(String.valueOf(ch).repeat(count));
                i++;
                continue;
            }

            // Regular character (count=1, implied)
            result.append(c);
            i++;
        }

        return result.toString();
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    /**
     * Calculates compression ratio (compressed length / original length).
     * @return ratio as a percentage string, e.g. "72.3%"
     */
    public String compressionRatio(String original, String compressed) {
        if (original == null || original.isEmpty()) return "N/A";
        double ratio = (double) compressed.length() / original.length() * 100;
        return String.format("%.1f%%", ratio);
    }
}
