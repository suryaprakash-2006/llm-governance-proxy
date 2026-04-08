package com.llmgovernance.system.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * GeminiClient – calls the Google Gemini API using pure Java (java.net.http).
 *
 * ── 100% FREE ──────────────────────────────────────────────────────────────
 * • No credit card required
 * • Get your free key at: https://aistudio.google.com/app/apikey
 *   (Sign in with Google → "Get API key" → Create API key → Copy)
 *
 * ── API Details ────────────────────────────────────────────────────────────
 * Endpoint : POST https://generativelanguage.googleapis.com/v1beta/models/
 *                       gemini-2.0-flash:generateContent?key=YOUR_KEY
 * Auth     : API key in URL query param (?key=...)
 * Model    : gemini-2.0-flash  (free tier: 15 RPM, 1500 RPD)
 *
 * ── Free Tier Limits (as of 2025) ─────────────────────────────────────────
 * • 15 requests per minute (RPM)
 * • 1,500 requests per day (RPD)
 * • 1,000,000 tokens per minute (TPM)
 * → Plenty for a college mini-project demo!
 *
 * ── No External Libraries ──────────────────────────────────────────────────
 * Uses only java.net.http.HttpClient (available since JDK 11).
 * JSON is built and parsed manually — no Gson or Jackson needed.
 *
 * ── Request Format (REST) ─────────────────────────────────────────────────
 * POST /v1beta/models/gemini-2.0-flash:generateContent?key=KEY
 * {
 *   "contents": [
 *     { "parts": [{ "text": "Your prompt here" }] }
 *   ],
 *   "generationConfig": { "maxOutputTokens": 1024 }
 * }
 *
 * ── Response Format ───────────────────────────────────────────────────────
 * {
 *   "candidates": [
 *     { "content": { "parts": [{ "text": "Gemini's reply" }] } }
 *   ]
 * }
 */
public class GeminiClient {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final String BASE_URL   =
            "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String MODEL      = "gemini-2.0-flash";
    private static final int    MAX_TOKENS = 1024;
    private static final int    TIMEOUT_S  = 30;

    // ── HTTP client (reusable, thread-safe) ───────────────────────────────────

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_S))
            .build();

    private final String apiKey;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * @param apiKey  Your free Gemini API key from https://aistudio.google.com
     *                Format: AIzaSy...
     */
    public GeminiClient(String apiKey) {
        this.apiKey = (apiKey == null) ? "" : apiKey.trim();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Sends a prompt to Gemini and returns the text response.
     *
     * @param filteredPrompt  The ALREADY-MASKED text (no raw PII).
     * @param systemContext   Optional instruction prepended to the prompt.
     * @return Gemini's response text.
     * @throws GeminiApiException on HTTP or API-level errors.
     */
    public String chat(String filteredPrompt, String systemContext)
            throws GeminiApiException {

        if (apiKey.isEmpty()) {
            throw new GeminiApiException(
                "API key is not set.\n\n" +
                "Get your FREE key (no credit card):\n" +
                "  1. Go to https://aistudio.google.com/app/apikey\n" +
                "  2. Sign in with your Google account\n" +
                "  3. Click 'Create API key'\n" +
                "  4. Paste it in the API Key field above");
        }
        if (filteredPrompt == null || filteredPrompt.trim().isEmpty()) {
            throw new GeminiApiException("Prompt is empty after filtering.");
        }

        // Combine system context + user prompt into one message
        String system = (systemContext == null || systemContext.isEmpty())
                ? "You are a helpful AI assistant. " +
                  "Note: sensitive data in the user message has been " +
                  "masked by a Data Leak Prevention system before reaching you. " +
                  "Respond helpfully based on the (masked) content provided."
                : systemContext;

        String fullPrompt = system + "\n\n" + filteredPrompt;

        // Build endpoint URL with API key
        String url = BASE_URL + MODEL + ":generateContent?key=" + apiKey;

        // Build JSON request body
        String requestBody = buildRequestBody(fullPrompt);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_S))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int    status = response.statusCode();
            String body   = response.body();

            if (status == 200) {
                return extractText(body);
            } else {
                String errMsg = extractErrorMessage(body);
                throw new GeminiApiException("Gemini API Error " + status + ": " + errMsg);
            }

        } catch (GeminiApiException e) {
            throw e;
        } catch (java.net.ConnectException e) {
            throw new GeminiApiException(
                "Cannot connect to Gemini API.\nCheck your internet connection.");
        } catch (java.net.http.HttpTimeoutException e) {
            throw new GeminiApiException(
                "Request timed out after " + TIMEOUT_S + " seconds.");
        } catch (Exception e) {
            throw new GeminiApiException("Unexpected error: " + e.getMessage());
        }
    }

    // ── JSON building (manual — no external library) ──────────────────────────

    /**
     * Builds the Gemini API request JSON body.
     *
     * Format:
     * {
     *   "contents": [{ "parts": [{ "text": "..." }] }],
     *   "generationConfig": { "maxOutputTokens": 1024 }
     * }
     */
    private String buildRequestBody(String prompt) {
        return "{"
            + "\"contents\": ["
            +   "{"
            +     "\"parts\": [{"
            +       "\"text\": " + jsonString(prompt)
            +     "}]"
            +   "}"
            + "],"
            + "\"generationConfig\": {"
            +   "\"maxOutputTokens\": " + MAX_TOKENS
            + "}"
            + "}";
    }

    /**
     * Safely escapes a Java string into a JSON string literal.
     */
    private String jsonString(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else          sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    // ── JSON parsing (manual — no external library) ────────────────────────────

    /**
     * Extracts the response text from Gemini's JSON response.
     *
     * Expected shape:
     * {
     *   "candidates": [
     *     {
     *       "content": {
     *         "parts": [{ "text": "Hello!" }]
     *       }
     *     }
     *   ]
     * }
     */
    private String extractText(String json) throws GeminiApiException {
        // Find the first "text" field inside "parts"
        // Strategy: find "parts" → then "text" after it
        int partsIdx = json.indexOf("\"parts\"");
        if (partsIdx == -1) {
            // Check for blocked content
            if (json.contains("SAFETY") || json.contains("finishReason")) {
                return "[Response blocked by Gemini safety filters. "
                     + "Try rephrasing your input.]";
            }
            throw new GeminiApiException(
                "Unexpected Gemini response format. Raw:\n" + truncate(json, 300));
        }

        int textKey = json.indexOf("\"text\"", partsIdx);
        if (textKey == -1) {
            throw new GeminiApiException(
                "No 'text' field in Gemini response. Raw:\n" + truncate(json, 300));
        }

        // Parse the string value after "text":
        int colon  = json.indexOf(':', textKey);
        int quote1 = json.indexOf('"', colon + 1);
        if (colon == -1 || quote1 == -1) {
            throw new GeminiApiException("Malformed 'text' field in Gemini response.");
        }

        // Read the JSON string character by character (handles escapes)
        StringBuilder result = new StringBuilder();
        int i = quote1 + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"'  -> result.append('"');
                    case '\\' -> result.append('\\');
                    case 'n'  -> result.append('\n');
                    case 'r'  -> result.append('\r');
                    case 't'  -> result.append('\t');
                    default   -> result.append(next);
                }
                i += 2;
            } else if (c == '"') {
                break; // end of string
            } else {
                result.append(c);
                i++;
            }
        }

        String text = result.toString().trim();
        return text.isEmpty() ? "[Gemini returned an empty response.]" : text;
    }

    /**
     * Extracts the error message from a Gemini API error response.
     * Shape: { "error": { "code": 400, "message": "...", "status": "..." } }
     */
    private String extractErrorMessage(String json) {
        try {
            int msgIdx = json.indexOf("\"message\"");
            if (msgIdx == -1) return truncate(json, 200);
            int colon  = json.indexOf(':', msgIdx);
            int quote1 = json.indexOf('"', colon + 1);
            int quote2 = json.indexOf('"', quote1 + 1);
            if (colon == -1 || quote1 == -1 || quote2 == -1) return truncate(json, 200);
            return json.substring(quote1 + 1, quote2);
        } catch (Exception e) {
            return truncate(json, 200);
        }
    }

    private String truncate(String s, int maxLen) {
        return (s != null && s.length() > maxLen) ? s.substring(0, maxLen) + "…" : s;
    }

    // ── Exception ─────────────────────────────────────────────────────────────

    public static class GeminiApiException extends Exception {
        public GeminiApiException(String message) { super(message); }
    }
}
