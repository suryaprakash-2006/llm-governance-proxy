package com.llmproxy.detection.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RegexDetector {

    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9+_.-]+@(.+)");

    private static final Pattern PHONE =
            Pattern.compile("\\b\\d{10}\\b");

    private static final Pattern CREDIT_CARD =
            Pattern.compile("\\b\\d{16}\\b");

    public List<String> detect(String text) {
        List<String> results = new ArrayList<>();

        if (EMAIL.matcher(text).find()) {
            results.add("EMAIL");
        }

        if (PHONE.matcher(text).find()) {
            results.add("PHONE");
        }

        if (CREDIT_CARD.matcher(text).find()) {
            results.add("CREDIT_CARD");
        }

        return results;
    }
}