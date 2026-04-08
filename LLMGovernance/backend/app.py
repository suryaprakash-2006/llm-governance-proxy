from __future__ import annotations

from pathlib import Path
from flask import Flask, jsonify, render_template, request

from governance import GovernanceEngine
from logger_setup import get_logger
from llm_local import LocalLLM
from executor import SecureExecutor
from storage import Storage


app = Flask(__name__, template_folder="templates", static_folder="static")

logger = get_logger()
storage = Storage()
governance = GovernanceEngine()
llm = LocalLLM(provider="ollama", model="llama3.2")
executor = SecureExecutor(workspace_root=Path(__file__).resolve().parent)


@app.route("/", methods=["GET"])
def home():
    return render_template("index.html")


@app.route("/ask", methods=["POST"])
def ask():
    payload = request.get_json(silent=True) or {}
    user_prompt = (payload.get("prompt") or "").strip()

    if not user_prompt:
        return jsonify({"ok": False, "error": "Prompt is required"}), 400

    input_decision = governance.evaluate_input(user_prompt)

    if input_decision.blocked:
        reason = "; ".join(input_decision.reasons) or "Blocked by policy"
        storage.save_audit("input_blocked", "WARNING", reason)
        storage.save_request(
            user_prompt=user_prompt,
            sanitized_prompt=input_decision.sanitized_input,
            model_response="",
            safe_response="",
            input_blocked=True,
            output_redacted=False,
            reasons=reason,
            llm_provider="local-ollama",
            execution_status="not-run",
        )
        logger.warning("Blocked prompt: %s", reason)
        return jsonify(
            {
                "ok": False,
                "blocked": True,
                "reasons": input_decision.reasons,
                "detected": input_decision.detected_entities,
                "safe_prompt": input_decision.sanitized_input,
            }
        ), 200

    model_response = llm.generate(input_decision.sanitized_input)
    output_decision = governance.filter_output(model_response)

    reasons = input_decision.reasons + output_decision.reasons
    reason_text = "; ".join(reasons)

    request_id = storage.save_request(
        user_prompt=user_prompt,
        sanitized_prompt=input_decision.sanitized_input,
        model_response=model_response,
        safe_response=output_decision.safe_output,
        input_blocked=False,
        output_redacted=output_decision.redactions_applied,
        reasons=reason_text,
        llm_provider="local-ollama",
        execution_status="not-run",
    )

    storage.save_audit(
        "ask_processed",
        "INFO",
        f"request_id={request_id}, redactions={output_decision.redactions_applied}",
    )
    logger.info("Processed prompt request_id=%s", request_id)

    return jsonify(
        {
            "ok": True,
            "blocked": False,
            "request_id": request_id,
            "safe_prompt": input_decision.sanitized_input,
            "response": output_decision.safe_output,
            "detected": input_decision.detected_entities,
            "output_flags": {
                "leakage_detected": output_decision.leakage_detected,
                "redactions_applied": output_decision.redactions_applied,
                "reasons": output_decision.reasons,
            },
        }
    )


@app.route("/execute", methods=["POST"])
def execute():
    payload = request.get_json(silent=True) or {}
    command = payload.get("command")

    if not isinstance(command, list) or not command:
        return jsonify({"ok": False, "error": "command must be a non-empty list"}), 400

    result = executor.run(command)
    severity = "INFO" if result.success else "WARNING"
    details = f"cmd={command}, return_code={result.return_code}"
    storage.save_audit("execution", severity, details)

    return jsonify(
        {
            "ok": result.success,
            "stdout": result.stdout,
            "stderr": result.stderr,
            "return_code": result.return_code,
        }
    )


@app.route("/logs", methods=["GET"])
def logs():
    limit = request.args.get("limit", default=100, type=int)
    limit = max(1, min(limit, 500))
    rows = storage.fetch_recent_logs(limit=limit)
    return jsonify({"ok": True, "count": len(rows), "logs": rows})


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000, debug=True)
