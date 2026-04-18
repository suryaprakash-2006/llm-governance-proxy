# Resume Project Description

## Short Version (3 Lines)
Built a Java-based AI Governance system with Swing UI and Local LLM integration using Ollama.
Implemented policy-driven RBAC, explainable safety decisions, and per-user Rate Limiting (5 requests/min).
Added centralized Audit Logging with an in-app log viewer for transparent monitoring.

## Detailed Version (6-8 Lines)
Developed a production-style Java desktop application focused on AI Governance for secure LLM usage.
Integrated Local LLM inference through Ollama to avoid external API dependency and improve privacy.
Implemented GovernanceEngine with policy-based filtering, explainable decisions, and role-specific RBAC rules.
Designed PolicyConfig to load global and role-based governance controls from JSON with safe fallback behavior.
Built an in-memory Rate Limiting module to restrict abuse to 5 requests per minute per user.
Added centralized Audit Logging and a LogViewerPanel for read-only runtime visibility and traceability.
Optimized the flow for maintainability, modularity, and clear demo readiness for academic and interview settings.
Demonstrated end-to-end controlled AI workflow with AI Governance, RBAC, Rate Limiting, Local LLM, and Audit Logging.
