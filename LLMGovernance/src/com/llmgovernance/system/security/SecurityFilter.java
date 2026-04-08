package com.llmgovernance.system.security;

import java.util.*;
import java.util.regex.*;

/**
 * SecurityFilter – detects and masks sensitive data in text.
 *
 * Patterns detected:
 *   1. Email addresses       → s****@domain.com
 *   2. Phone numbers (10 d.) → ××××××XXXX  (last 4 digits shown)
 *   3. Sensitive keywords    → [REDACTED]
 */
public class SecurityFilter {

    // ── Regex patterns ────────────────────────────────────────────────────────

    /** RFC-5322 simplified email pattern */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    /** Exactly 10 consecutive digits (optionally separated by spaces/dashes) */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("\\b(\\d{3}[\\s\\-]?\\d{3}[\\s\\-]?\\d{4})\\b");

    /** Case-insensitive sensitive keywords */
    private static final Pattern KEYWORD_PATTERN =
            Pattern.compile("\\b(password|secret|confidential|api[_\\-]?key|token|ssn|credit[_\\-]?card)\\b",
                    Pattern.CASE_INSENSITIVE);

    // ── Detection ─────────────────────────────────────────────────────────────

    /**
     * Detection result DTO returned to the UI.
     */
    public static class DetectionResult {
        public final boolean hasEmail;
        public final boolean hasPhone;
        public final boolean hasKeyword;
        public final List<String> detectedItems;

        public DetectionResult(boolean hasEmail, boolean hasPhone,
                               boolean hasKeyword, List<String> detectedItems) {
            this.hasEmail      = hasEmail;
            this.hasPhone      = hasPhone;
            this.hasKeyword    = hasKeyword;
            this.detectedItems = Collections.unmodifiableList(detectedItems);
        }

        public boolean hasSensitiveData() {
            return hasEmail || hasPhone || hasKeyword;
        }
    }

    /**
     * Scans text and returns a DetectionResult listing all found sensitive items.
     */
    public DetectionResult detect(String text) {
        if (text == null || text.isEmpty()) {
            return new DetectionResult(false, false, false, Collections.emptyList());
        }

        List<String> found = new ArrayList<>();
        boolean hasEmail   = false;
        boolean hasPhone   = false;
        boolean hasKeyword = false;

        // Emails
        Matcher m = EMAIL_PATTERN.matcher(text);
        while (m.find()) {
            hasEmail = true;
            found.add("EMAIL: " + m.group());
        }

        // Phones
        m = PHONE_PATTERN.matcher(text);
        while (m.find()) {
            hasPhone = true;
            found.add("PHONE: " + m.group());
        }

        // Keywords
        m = KEYWORD_PATTERN.matcher(text);
        while (m.find()) {
            hasKeyword = true;
            found.add("KEYWORD: " + m.group());
        }

        return new DetectionResult(hasEmail, hasPhone, hasKeyword, found);
    }

    // ── Masking ───────────────────────────────────────────────────────────────

    /**
     * Returns a copy of text with all sensitive data masked.
     *
     * Masking rules:
     *   email   → first letter + "****@" + domain   e.g. john@gmail.com → j****@gmail.com
     *   phone   → "××××××" + last 4 digits           e.g. 9876543210  → ××××××3210
     *   keyword → [REDACTED]
     */
    public String mask(String text) {
        if (text == null) return "";

        // 1. Mask emails first (before keyword pass could touch them)
        StringBuffer sb = new StringBuffer();
        Matcher m = EMAIL_PATTERN.matcher(text);
        while (m.find()) {
            String email  = m.group();
            int atIdx     = email.indexOf('@');
            String local  = email.substring(0, atIdx);
            String domain = email.substring(atIdx);           // includes '@'
            String masked = (local.isEmpty() ? "*" : local.charAt(0) + "****") + domain;
            m.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        m.appendTail(sb);
        text = sb.toString();

        // 2. Mask phone numbers
        sb = new StringBuffer();
        m  = PHONE_PATTERN.matcher(text);
        while (m.find()) {
            String digits = m.group().replaceAll("[\\s\\-]", ""); // strip separators
            String masked = "\u00d7\u00d7\u00d7\u00d7\u00d7\u00d7"   // ××××××
                    + digits.substring(digits.length() - 4);
            m.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        m.appendTail(sb);
        text = sb.toString();

        // 3. Mask sensitive keywords
        sb = new StringBuffer();
        m  = KEYWORD_PATTERN.matcher(text);
        while (m.find()) {
            m.appendReplacement(sb, "[REDACTED]");
        }
        m.appendTail(sb);
        text = sb.toString();

        return text;
    }

    // ── Summary ───────────────────────────────────────────────────────────────

    /**
     * Returns a human-readable summary of what was detected.
     */
    public String summarize(DetectionResult result) {
        if (!result.hasSensitiveData()) {
            return "✅ No sensitive data detected.";
        }
        StringBuilder sb = new StringBuilder("⚠️  Sensitive data found:\n");
        for (String item : result.detectedItems) {
            sb.append("  • ").append(item).append("\n");
        }
        return sb.toString().trim();
    }
}
