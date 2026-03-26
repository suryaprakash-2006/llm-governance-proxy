package com.llmproxy.detection.util;

public class MaskingUtil {

    // Mask email
    public static String maskEmail(String text) {
        return text.replaceAll(
                "([a-zA-Z0-9])[^@]*(@.*)",
                "$1***$2"
        );
    }

    // Mask password
    public static String maskPassword(String text) {
        return text.replaceAll(
                "(?i)password\\s*is\\s*\\w+",
                "password is ****"
        );
    }

    // Apply all masking
    public static String applyMasking(String text) {
        text = maskEmail(text);
        text = maskPassword(text);
        return text;
    }
}
