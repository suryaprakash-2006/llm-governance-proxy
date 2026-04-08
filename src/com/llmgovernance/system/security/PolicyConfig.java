package com.llmgovernance.system.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PolicyConfig loads governance policy from policy.json in project root.
 *
 * Supported JSON:
 * {
 *   "global": {
 *     "maskEmail": true,
 *     "maskPhone": true,
 *     "maxInputLength": 500,
 *     "blockedKeywords": ["password"]
 *   },
 *   "roles": {
 *     "ADMIN": { "blockedKeywords": [] },
 *     "USER":  { "blockedKeywords": ["hack", "bypass"] }
 *   }
 * }
 *
 * Legacy top-level fields are still supported for compatibility.
 */
public class PolicyConfig {

    private static final Set<String> DEFAULT_BLOCKED = new HashSet<>(Arrays.asList(
            "password", "admin", "system", "root", "token", "api key", "secret", "confidential"
    ));
    private static final boolean DEFAULT_MASK_EMAIL = true;
    private static final boolean DEFAULT_MASK_PHONE = true;
    private static final int DEFAULT_MAX_INPUT_LENGTH = 4000;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";

    private final Set<String> blockedKeywords;
    private final Map<String, Set<String>> roleBlockedKeywords;
    private final boolean maskEmail;
    private final boolean maskPhone;
    private final int maxInputLength;

    PolicyConfig(Set<String> blockedKeywords,
                 Map<String, Set<String>> roleBlockedKeywords,
                 boolean maskEmail,
                 boolean maskPhone,
                 int maxInputLength) {
        this.blockedKeywords = Collections.unmodifiableSet(new HashSet<>(blockedKeywords));
        Map<String, Set<String>> safeMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : roleBlockedKeywords.entrySet()) {
            safeMap.put(entry.getKey().toUpperCase(Locale.ROOT),
                    Collections.unmodifiableSet(new HashSet<>(entry.getValue())));
        }
        this.roleBlockedKeywords = Collections.unmodifiableMap(safeMap);
        this.maskEmail = maskEmail;
        this.maskPhone = maskPhone;
        this.maxInputLength = maxInputLength;
    }

    public static PolicyConfig defaults() {
        Map<String, Set<String>> roleRules = new HashMap<>();
        roleRules.put(ROLE_ADMIN, Collections.emptySet());
        roleRules.put(ROLE_USER, new HashSet<>(DEFAULT_BLOCKED));

        return new PolicyConfig(
                new HashSet<>(DEFAULT_BLOCKED),
                roleRules,
                DEFAULT_MASK_EMAIL,
                DEFAULT_MASK_PHONE,
                DEFAULT_MAX_INPUT_LENGTH
        );
    }

    public static PolicyConfig fromLegacyKeywords(Set<String> keywords) {
        PolicyConfig fallback = defaults();
        Set<String> normalized = normalizeKeywords(keywords);
        if (normalized.isEmpty()) {
            normalized = new HashSet<>(fallback.getBlockedKeywords());
        }

        Map<String, Set<String>> roleRules = new HashMap<>();
        roleRules.put(ROLE_ADMIN, Collections.emptySet());
        roleRules.put(ROLE_USER, new HashSet<>(normalized));

        return new PolicyConfig(
                normalized,
                roleRules,
                fallback.isMaskEmail(),
                fallback.isMaskPhone(),
                fallback.getMaxInputLength()
        );
    }

    public static PolicyConfig loadFromProjectRoot() {
        Path policyPath = Path.of(System.getProperty("user.dir"), "policy.json");
        return load(policyPath);
    }

    public static PolicyConfig load(Path policyPath) {
        PolicyConfig fallback = defaults();

        try {
            if (policyPath == null || !Files.exists(policyPath)) {
                return fallback;
            }

            String json = Files.readString(policyPath, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) {
                return fallback;
            }

            String globalSection = extractObjectByKey(json, "global").orElse(json);
            String rolesSection = extractObjectByKey(json, "roles").orElse(json);

            Set<String> blocked = parseBlockedKeywords(globalSection);
            if (blocked.isEmpty()) {
                blocked = parseBlockedKeywords(json);
            }
            if (blocked.isEmpty()) {
                blocked = new HashSet<>(fallback.getBlockedKeywords());
            }

            Map<String, Set<String>> roleRules = parseRoleBlockedKeywords(rolesSection, json, blocked);

            boolean maskEmail = parseBoolean(globalSection, "maskEmail",
                    parseBoolean(json, "maskEmail", fallback.isMaskEmail()));
            boolean maskPhone = parseBoolean(globalSection, "maskPhone",
                    parseBoolean(json, "maskPhone", fallback.isMaskPhone()));
            int maxInputLength = parseInt(globalSection, "maxInputLength",
                    parseInt(json, "maxInputLength", fallback.getMaxInputLength()));
            if (maxInputLength <= 0) {
                maxInputLength = fallback.getMaxInputLength();
            }

            return new PolicyConfig(blocked, roleRules, maskEmail, maskPhone, maxInputLength);
        } catch (IOException e) {
            return fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    public Set<String> getBlockedKeywords() {
        return blockedKeywords;
    }

    public Set<String> getBlockedKeywordsForRole(String role) {
        String key = (role == null || role.isBlank()) ? ROLE_USER : role.trim().toUpperCase(Locale.ROOT);
        return roleBlockedKeywords.getOrDefault(key,
                roleBlockedKeywords.getOrDefault(ROLE_USER, blockedKeywords));
    }

    public boolean isMaskEmail() {
        return maskEmail;
    }

    public boolean isMaskPhone() {
        return maskPhone;
    }

    public int getMaxInputLength() {
        return maxInputLength;
    }

    public Map<String, Set<String>> getRoleBlockedKeywords() {
        return roleBlockedKeywords;
    }

    private static Set<String> parseBlockedKeywords(String json) {
        return parseBlockedKeywordsInSection(json);
    }

    private static Map<String, Set<String>> parseRoleBlockedKeywords(String rolesSection,
                                                                      String wholeJson,
                                                                      Set<String> globalBlocked) {
        Map<String, Set<String>> out = new HashMap<>();

        Set<String> admin = parseRoleBlock(rolesSection, ROLE_ADMIN)
                .or(() -> parseRoleBlock(wholeJson, ROLE_ADMIN))
                .orElse(Collections.emptySet());

        Set<String> user = parseRoleBlock(rolesSection, ROLE_USER)
                .or(() -> parseRoleBlock(wholeJson, ROLE_USER))
                .orElse(globalBlocked);

        out.put(ROLE_ADMIN, new HashSet<>(admin));
        out.put(ROLE_USER, new HashSet<>(user));
        return out;
    }

    private static Optional<Set<String>> parseRoleBlock(String json, String roleName) {
        Optional<String> section = extractObjectByKey(json, roleName);
        if (section.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(parseBlockedKeywordsInSection(section.get()));
    }

    private static Set<String> parseBlockedKeywordsInSection(String jsonSection) {
        Set<String> out = new HashSet<>();
        Pattern keyArray = Pattern.compile("\\\"blockedKeywords\\\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher m = keyArray.matcher(jsonSection);
        if (!m.find()) {
            return out;
        }

        String items = m.group(1);
        Pattern stringItem = Pattern.compile("\\\"(.*?)\\\"");
        Matcher sm = stringItem.matcher(items);
        while (sm.find()) {
            String keyword = unescapeJson(sm.group(1)).trim().toLowerCase(Locale.ROOT);
            if (!keyword.isEmpty()) {
                out.add(keyword);
            }
        }
        return out;
    }

    private static Set<String> normalizeKeywords(Set<String> keywords) {
        Set<String> normalized = new HashSet<>();
        if (keywords == null) return normalized;
        for (String k : keywords) {
            if (k != null && !k.trim().isEmpty()) {
                normalized.add(k.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private static boolean parseBoolean(String json, String field, boolean fallback) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(json);
        if (!m.find()) {
            return fallback;
        }
        return Boolean.parseBoolean(m.group(1).toLowerCase());
    }

    private static int parseInt(String json, String field, int fallback) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(json);
        if (!m.find()) {
            return fallback;
        }
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String unescapeJson(String s) {
        return s
                .replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private static Optional<String> extractObjectByKey(String json, String key) {
        if (json == null || json.isBlank() || key == null || key.isBlank()) {
            return Optional.empty();
        }

        Pattern keyPattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:");
        Matcher m = keyPattern.matcher(json);
        if (!m.find()) {
            return Optional.empty();
        }

        int idx = m.end();
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) {
            idx++;
        }
        if (idx >= json.length() || json.charAt(idx) != '{') {
            return Optional.empty();
        }

        int start = idx;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
                continue;
            }

            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return Optional.of(json.substring(start, i + 1));
                }
            }
        }

        return Optional.empty();
    }
}
