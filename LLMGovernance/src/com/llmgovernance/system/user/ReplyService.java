package com.llmgovernance.system.user;

import com.llmgovernance.system.llm.OllamaClient;
import com.llmgovernance.system.llm.OllamaClient.OllamaApiException;
import com.llmgovernance.system.security.GovernanceEngine;
import com.llmgovernance.system.security.SecurityFilter;
import com.llmgovernance.system.util.HashUtil;
import com.llmgovernance.system.util.AppLogger;
import com.llmgovernance.system.db.DataDAO;
import com.llmgovernance.system.db.DBConnection;
import com.llmgovernance.system.model.Prompt;

import java.util.logging.Logger;

/**
 * ReplyService – orchestrates the full processing pipeline:
 *
 *   Input -> Detect -> Mask -> [Local LLM (Ollama)] -> Hash -> Save
 *
 * The LLM only ever sees the FILTERED (masked) text — never raw PII.
 * This is the core "LLM Data Leak Prevention" guarantee.
 *
 * Uses local Ollama model (offline/local-first).
 */
public class ReplyService {

    private static final Logger LOG = AppLogger.getLogger(ReplyService.class);
    private static final String DEFAULT_USER_ROLE = GovernanceEngine.ROLE_USER;
    private static final String DEFAULT_USER_ID = resolveDefaultUserId();

    private final SecurityFilter filter     = new SecurityFilter();
    private final GovernanceEngine governance = new GovernanceEngine();
    private final DataDAO        dao        = new DataDAO();
    private final RateLimiter rateLimiter = new RateLimiter(5, 60_000);

    // Local LLM client (Ollama)
    private final OllamaClient ollamaClient = new OllamaClient("llama3.2");

    // ── Local model configuration ────────────────────────────────────────────

    // Backward-compatible method retained to avoid breaking existing UI wiring.
    public void setApiKey(String apiKey) {
        // Not used in local-only mode.
    }

    public boolean isApiKeySet() {
        return true;
    }

    public void setLocalModel(String model) {
        ollamaClient.setModel(model);
    }

    public String getLocalModel() {
        return ollamaClient.getModel();
    }

    // ── Result DTOs ───────────────────────────────────────────────────────────

    public static class ProcessResult {
        public String  originalText;
        public String  detectionSummary;
        public String  filteredText;
        public String  compressedText;
        public String  originalHash;
        public String  llmResponse;
        public boolean sensitiveFound;
        public boolean llmCalled;
        public int     savedId;
        public double  compressionRatioPercent;
    }

    public static class DecompressResult {
        public String  decompressedText;
        public String  decompressedHash;
        public String  originalHash;
        public boolean integrityOk;
        public String  integrityReport;
    }

    // ── Full pipeline ─────────────────────────────────────────────────────────

    /**
     * Runs the full pipeline:
     *  1. Detect sensitive data (regex)
     *  2. Mask sensitive data
    *  3. Send MASKED text to local Ollama model -> LLM response
    *  4. Hash original text (SHA-256)
    *  5. Persist everything to flat-file DB
     */
    public ProcessResult analyze(String inputText) {
        return analyze(inputText, DEFAULT_USER_ID, DEFAULT_USER_ROLE);
    }

    public ProcessResult analyze(String inputText, String userRole) {
        return analyze(inputText, DEFAULT_USER_ID, userRole);
    }

    public ProcessResult analyze(String inputText, String userId, String userRole) {
        ProcessResult result   = new ProcessResult();
        result.originalText    = inputText;
        result.llmResponse     = "";
        result.llmCalled       = false;

        String effectiveUserId = (userId == null || userId.isBlank()) ? DEFAULT_USER_ID : userId.trim();
        String effectiveRole = (userRole == null || userRole.isBlank()) ? DEFAULT_USER_ROLE : userRole;

        if (!rateLimiter.isAllowed(effectiveUserId)) {
            result.sensitiveFound = false;
            result.detectionSummary = "⚠️ Rate limit exceeded for user '" + effectiveUserId + "'.";
            result.filteredText = inputText == null ? "" : inputText;
            result.compressedText = result.filteredText;
            result.compressionRatioPercent = 100.0;
            result.originalHash = HashUtil.sha256(inputText);
            result.savedId = -1;
            result.llmResponse = "Too many requests. Please wait before trying again.";
            LOG.warning("Rate limiter blocked request for userId=" + effectiveUserId);
            return result;
        }

        GovernanceEngine.InputDecision governanceInput = governance.evaluateInput(inputText, effectiveRole);

        // Step 1: Detect
        SecurityFilter.DetectionResult detection = filter.detect(inputText);
        result.sensitiveFound   = detection.hasSensitiveData();
        result.detectionSummary = filter.summarize(detection);

        if (!governanceInput.detected.isEmpty()) {
            result.detectionSummary += "\n\nGovernance Detected:\n  • "
                    + String.join("\n  • ", governanceInput.detected);
        }

        // Step 2: Mask
        result.filteredText = governanceInput.sanitizedInput;

        if (governanceInput.blocked) {
            result.llmResponse = governanceInput.decision.reason;
            LOG.warning("Blocked prompt by governance rules: " + governanceInput.decision.reason
                + " | role=" + effectiveRole + " | userId=" + effectiveUserId);
        }

        // Step 3: Call local Ollama model with MASKED text (NEVER raw PII)
        if (!governanceInput.blocked) {
            try {
            String rawResponse = ollamaClient.chat(result.filteredText);
                GovernanceEngine.OutputDecision out = governance.filterOutput(rawResponse);
                result.llmResponse = out.safeOutput;
                result.llmCalled   = true;

                if (out.redactionsApplied) {
                    result.detectionSummary += "\n\nOutput Governance:\n  • "
                            + String.join("\n  • ", out.reasons);
                    LOG.info("LLM output redacted by governance layer.");
                }
            } catch (OllamaApiException e) {
                result.llmResponse = "LLM Error: " + e.getMessage();
                result.llmCalled   = false;
                LOG.warning("Local Ollama call failed: " + e.getMessage());
            }
        }

        // Step 4: No compression in the current project scope.
        // Keep the field populated for compatibility with existing storage/UI paths.
        result.compressedText = result.filteredText;
        result.compressionRatioPercent = 100.0;

        // Step 5: Hash original text
        result.originalHash = HashUtil.sha256(inputText);

        // Step 6: Persist to SQL DB
        Prompt prompt = new Prompt(
            inputText, result.filteredText, result.compressedText,
            result.originalHash, "", DBConnection.getInstance().now());
        prompt.setUserId(effectiveUserId);
        prompt.setUserRole(effectiveRole);
        result.savedId = dao.savePrompt(prompt);

        LOG.info("Request processed. Record ID=" + result.savedId
            + ", llmCalled=" + result.llmCalled
            + ", blocked=" + governanceInput.blocked);

        return result;
    }

    private static String resolveDefaultUserId() {
        String osUser = System.getProperty("user.name");
        if (osUser == null || osUser.isBlank()) {
            return "anonymous";
        }
        return osUser.trim();
    }

    // ── Decompress + verify ───────────────────────────────────────────────────

    public DecompressResult decompressAndVerify(String compressedText, String originalHash) {
        String decompressed     = compressedText == null ? "" : compressedText;
        DecompressResult result = new DecompressResult();
        result.decompressedText  = decompressed;
        result.decompressedHash  = HashUtil.sha256(decompressed);
        result.originalHash      = (originalHash != null) ? originalHash : "";
        result.integrityOk       = HashUtil.verify(result.originalHash, result.decompressedHash);
        result.integrityReport   = HashUtil.integrityReport(result.originalHash, result.decompressedHash);
        return result;
    }

    public int recordCount() { return dao.countRecords(); }
}
