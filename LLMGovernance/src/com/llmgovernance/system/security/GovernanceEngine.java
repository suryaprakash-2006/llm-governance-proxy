package com.llmgovernance.system.security;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GovernanceEngine - central policy engine for input and output governance.
 *
 * Responsibilities:
 * 1) Input filtering: block risky keywords, detect PII, sanitize prompt.
 * 2) Output filtering: redact leaked PII before showing to user.
 */
public class GovernanceEngine {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("\\b(?:\\+?\\d{1,3}[\\s\\-]?)?(?:\\d[\\s\\-]?){10,13}\\b");

    private final PolicyConfig policyConfig;
    private final boolean maskEmail;
    private final boolean maskPhone;
    private final int maxInputLength;

    public GovernanceEngine() {
        this(PolicyConfig.loadFromProjectRoot());
    }

    public GovernanceEngine(Set<String> blockKeywords) {
        this(PolicyConfig.fromLegacyKeywords(blockKeywords));
    }

    public GovernanceEngine(PolicyConfig policyConfig) {
        PolicyConfig fallback = PolicyConfig.defaults();
        this.policyConfig = policyConfig == null ? fallback : policyConfig;

        this.maskEmail = this.policyConfig.isMaskEmail();
        this.maskPhone = this.policyConfig.isMaskPhone();
        this.maxInputLength = this.policyConfig.getMaxInputLength() > 0
                ? this.policyConfig.getMaxInputLength()
                : fallback.getMaxInputLength();
    }

    public static class InputDecision {
        public final boolean blocked;
        public final boolean allowed;
        public final GovernanceDecision decision;
        public final List<String> reasons;
        public final List<String> detected;
        public final String sanitizedInput;

        public InputDecision(boolean blocked, GovernanceDecision decision,
                             List<String> reasons, List<String> detected, String sanitizedInput) {
            this.blocked = blocked;
            this.allowed = !blocked;
            this.decision = decision;
            this.reasons = Collections.unmodifiableList(reasons);
            this.detected = Collections.unmodifiableList(detected);
            this.sanitizedInput = sanitizedInput;
        }
    }

    public static class GovernanceDecision {
        public final boolean allowed;
        public final String reason;

        public GovernanceDecision(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }
    }

    public static class OutputDecision {
        public final String safeOutput;
        public final boolean redactionsApplied;
        public final boolean leakageDetected;
        public final List<String> reasons;

        public OutputDecision(String safeOutput, boolean redactionsApplied,
                              boolean leakageDetected, List<String> reasons) {
            this.safeOutput = safeOutput;
            this.redactionsApplied = redactionsApplied;
            this.leakageDetected = leakageDetected;
            this.reasons = Collections.unmodifiableList(reasons);
        }
    }

    public InputDecision evaluateInput(String input) {
        return evaluateInput(input, ROLE_USER);
    }

    public InputDecision evaluateInput(String input, String userRole) {
        if (input == null) input = "";

        boolean tooLong = input.length() > maxInputLength;
        List<String> matchedKeywords = new ArrayList<>();
        for (String k : activeKeywordsForRole(userRole)) {
            if (containsRestrictedKeyword(input, k)) {
                matchedKeywords.add(k);
            }
        }
        Collections.sort(matchedKeywords);

        List<String> reasons = new ArrayList<>();
        String primaryReason = "Allowed by policy.";
        if (!matchedKeywords.isEmpty()) {
            primaryReason = "Blocked due to restricted keyword: " + matchedKeywords.get(0);
            reasons.add(primaryReason);
        }
        if (tooLong) {
            String lengthReason = "Input exceeds maxInputLength=" + maxInputLength;
            reasons.add(lengthReason);
            if (matchedKeywords.isEmpty()) {
                primaryReason = lengthReason;
            }
        }

        List<String> detected = new ArrayList<>();
        Matcher m = EMAIL_PATTERN.matcher(input);
        while (m.find()) detected.add("email:" + m.group());

        m = PHONE_PATTERN.matcher(input);
        while (m.find()) detected.add("phone:" + m.group().trim());

        for (String k : matchedKeywords) detected.add("keyword:" + k);
        if (tooLong) detected.add("length:" + input.length());

        String sanitized = sanitizeInput(input);
        boolean blocked = !matchedKeywords.isEmpty() || tooLong;
        GovernanceDecision decision = new GovernanceDecision(!blocked, primaryReason);
        return new InputDecision(blocked, decision, reasons, detected, sanitized);
    }

    public OutputDecision filterOutput(String output) {
        if (output == null) output = "";

        List<String> reasons = new ArrayList<>();
        boolean redactions = false;
        boolean leakage = false;

        MaskResult emailPass = maskEmail ? maskEmails(output) : new MaskResult(output, false);
        if (emailPass.found) {
            redactions = true;
            leakage = true;
            reasons.add("Email pattern detected and redacted from model output");
        }

        MaskResult phonePass = maskPhone ? maskPhones(emailPass.text) : new MaskResult(emailPass.text, false);
        if (phonePass.found) {
            redactions = true;
            leakage = true;
            reasons.add("Phone pattern detected and redacted from model output");
        }

        return new OutputDecision(phonePass.text, redactions, leakage, reasons);
    }

    private String sanitizeInput(String input) {
        MaskResult e = maskEmail ? maskEmails(input) : new MaskResult(input, false);
        MaskResult p = maskPhone ? maskPhones(e.text) : new MaskResult(e.text, false);
        return p.text;
    }

    private Set<String> activeKeywordsForRole(String userRole) {
        String role = normalizeRole(userRole);
        Set<String> keywords = policyConfig.getBlockedKeywordsForRole(role);
        if (keywords == null || keywords.isEmpty()) {
            return policyConfig.getBlockedKeywords();
        }
        return keywords;
    }

    private String normalizeRole(String userRole) {
        if (userRole == null || userRole.isBlank()) return ROLE_USER;
        String normalized = userRole.trim().toUpperCase(Locale.ROOT);
        if (ROLE_ADMIN.equals(normalized)) return ROLE_ADMIN;
        return ROLE_USER;
    }

    private boolean containsRestrictedKeyword(String input, String keyword) {
        if (input == null || input.isBlank() || keyword == null || keyword.isBlank()) {
            return false;
        }

        String trimmed = keyword.trim();
        Pattern pattern;
        if (trimmed.contains(" ")) {
            String phrase = Pattern.quote(trimmed).replace("\\ ", "\\s+");
            pattern = Pattern.compile("(?i)\\b" + phrase + "\\b");
        } else {
            pattern = Pattern.compile("(?i)\\b" + Pattern.quote(trimmed) + "\\w*\\b");
        }
        return pattern.matcher(input).find();
    }

    private static class MaskResult {
        final String text;
        final boolean found;

        MaskResult(String text, boolean found) {
            this.text = text;
            this.found = found;
        }
    }

    private MaskResult maskEmails(String text) {
        StringBuffer sb = new StringBuffer();
        Matcher m = EMAIL_PATTERN.matcher(text);
        boolean found = false;

        while (m.find()) {
            found = true;
            String email = m.group();
            int at = email.indexOf('@');
            String local = email.substring(0, at);
            String domain = email.substring(at);
            String prefix = local.isEmpty() ? "*" : String.valueOf(local.charAt(0));
            String masked = prefix + "***" + domain;
            m.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        m.appendTail(sb);
        return new MaskResult(sb.toString(), found);
    }

    private MaskResult maskPhones(String text) {
        StringBuffer sb = new StringBuffer();
        Matcher m = PHONE_PATTERN.matcher(text);
        boolean found = false;

        while (m.find()) {
            found = true;
            String raw = m.group();
            String digits = raw.replaceAll("[^0-9]", "");
            String masked = digits.length() < 4
                    ? "[PHONE_REDACTED]"
                    : "[PHONE_REDACTED:" + digits.substring(digits.length() - 4) + "]";
            m.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        m.appendTail(sb);
        return new MaskResult(sb.toString(), found);
    }
}
