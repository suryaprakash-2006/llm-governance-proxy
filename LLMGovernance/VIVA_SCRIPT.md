# Viva Script: LLMGovernance

## 2-Minute Explanation
Our project is LLMGovernance, a Java Swing based application for safe and controlled use of local LLMs. The key idea is that user prompts do not go directly to the model. We place a GovernanceEngine in between.

This governance layer reads policy rules from JSON, applies role-based access control for ADMIN and USER, checks input length and restricted keywords, and returns explainable decisions with reason. If input is unsafe, it is blocked before model execution.

We also implemented Rate Limiting at 5 requests per minute per user to prevent misuse. For model execution, we use Ollama locally, so the system is privacy-focused and does not depend on paid APIs. Finally, all major events are logged and visible in a built-in LogViewerPanel for auditing.

So the project demonstrates responsible AI usage with governance, transparency, and control.

## 5-Minute Detailed Explanation

### 1. Problem Statement
Many LLM applications focus on response quality but ignore governance. Without safeguards, malicious prompts, sensitive data leakage, and request abuse become risks.

### 2. Project Goal
Build a local LLM system that is secure, explainable, and auditable using simple Java architecture.

### 3. Architecture Overview
1. User enters prompt in Swing UI.
2. ReplyService handles orchestration.
3. RateLimiter enforces 5 requests/min/user.
4. GovernanceEngine applies policy and RBAC.
5. Safe input goes to local Ollama model.
6. Output is checked and redacted.
7. Results and events are logged.
8. Logs are viewed in LogViewerPanel.

### 4. Governance Logic
GovernanceEngine supports:
1. Policy-based filtering from policy.json
2. Role-based keyword restrictions (ADMIN/USER)
3. Explainable decisions (allowed + reason)
4. Input safety validation and output redaction

### 5. RBAC
USER role uses stricter blocked keyword rules.
ADMIN role can have relaxed policy rules based on configuration.
This allows controlled access without changing code.

### 6. Rate Limiting
RateLimiter stores per-user request timestamps in memory and allows max 5 requests in a rolling one-minute window. Excess requests are blocked with a clear message.

### 7. Logging and Audit
Centralized logging writes safety, block, and runtime events. LogViewerPanel provides read-only visibility inside the UI, enabling easy monitoring for demo and viva.

### 8. Outcome
We achieved a practical AI Governance solution that is local-first, policy-driven, explainable, and suitable for academic evaluation and real-world secure AI workflows.

## Key Technical Concepts (Simple)
1. Governance Layer: Safety gate between user and model
2. RBAC: Different rules for different user roles
3. Explainable Decision: Every block has a clear reason
4. Rate Limiting: Abuse prevention through request throttling
5. Audit Logging: Traceable actions for review and compliance

## Common Viva Questions with Answers

### Q1. Why did you use a Local LLM instead of cloud API?
We used Ollama locally for privacy, control, and cost independence. It also allows offline demos and avoids exposing prompts to external services.

### Q2. How do you ensure user safety?
Safety is enforced through policy-based filtering, role restrictions, rate limiting, and output redaction. Unsafe prompts are blocked before model execution.

### Q3. What happens for malicious input?
GovernanceEngine detects restricted patterns, blocks the request, and returns an explainable reason. The event is also logged for auditing.

### Q4. How is your policy managed?
Policy is stored in policy.json with global settings and role-specific rules. It is easy to update without changing source code.

### Q5. What if policy file is missing or invalid?
The system uses safe fallback defaults so behavior remains stable and secure.
