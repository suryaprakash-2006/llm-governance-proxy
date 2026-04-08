package com.llmgovernance.system.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * OllamaClient - local LLM client for Ollama server.
 *
 * Default endpoint: http://localhost:11434/api/generate
 */
public class OllamaClient {

    private static final String ENDPOINT = "http://localhost:11434/api/generate";
    private static final int TIMEOUT_S = 60;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_S))
            .build();

    private String model;

    public OllamaClient(String model) {
        this.model = (model == null || model.isBlank()) ? "llama3.2" : model.trim();
    }

    public void setModel(String model) {
        if (model != null && !model.isBlank()) {
            this.model = model.trim();
        }
    }

    public String getModel() {
        return model;
    }

    public String chat(String prompt) throws OllamaApiException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new OllamaApiException("Prompt is empty after filtering.");
        }

        String requestBody = "{" +
                "\"model\":" + jsonString(model) + "," +
                "\"prompt\":" + jsonString(prompt) + "," +
                "\"stream\":false" +
                "}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(TIMEOUT_S))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new OllamaApiException("Ollama error " + response.statusCode() + ": "
                        + truncate(response.body(), 300));
            }

            return extractResponseText(response.body());

        } catch (OllamaApiException e) {
            throw e;
        } catch (java.net.ConnectException e) {
            throw new OllamaApiException(
                    "Cannot connect to local Ollama server at http://localhost:11434. "
                            + "Start Ollama and run a model, e.g. 'ollama run llama3.2'.");
        } catch (Exception e) {
            throw new OllamaApiException("Unexpected local LLM error: " + e.getMessage());
        }
    }

    private String extractResponseText(String json) throws OllamaApiException {
        int key = json.indexOf("\"response\"");
        if (key == -1) {
            throw new OllamaApiException("Unexpected Ollama response: " + truncate(json, 300));
        }

        int colon = json.indexOf(':', key);
        int quote1 = json.indexOf('"', colon + 1);
        if (colon == -1 || quote1 == -1) {
            throw new OllamaApiException("Malformed response field from Ollama.");
        }

        StringBuilder sb = new StringBuilder();
        int i = quote1 + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(i + 1);
                switch (n) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default -> sb.append(n);
                }
                i += 2;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
                i++;
            }
        }

        String out = sb.toString().trim();
        return out.isEmpty() ? "[Local model returned empty output.]" : out;
    }

    private String jsonString(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    private String truncate(String s, int maxLen) {
        return (s != null && s.length() > maxLen) ? s.substring(0, maxLen) + "..." : s;
    }

    public static class OllamaApiException extends Exception {
        public OllamaApiException(String message) {
            super(message);
        }
    }
}
