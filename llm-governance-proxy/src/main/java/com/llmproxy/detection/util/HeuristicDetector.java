package com.llmproxy.detection.util;

import java.util.ArrayList;
import java.util.List;

public class HeuristicDetector {

    public List<String> detect(String text) {
        List<String> results = new ArrayList<>();

        if (text.contains("class") || text.contains("{") || text.contains(";")) {
            results.add("CODE");
        }

        if (text.toLowerCase().contains("password") ||
            text.toLowerCase().contains("api_key") ||
            text.toLowerCase().contains("token")) {
            results.add("SECRET");
        }

        return results;
    }
}