"""SQLite storage for audit logs and request history."""

from __future__ import annotations

import sqlite3
from pathlib import Path
from typing import Any, Dict, List


DB_PATH = Path(__file__).resolve().parent / "governance.db"


class Storage:
    def __init__(self, db_path: Path | None = None) -> None:
        self.db_path = db_path or DB_PATH
        self._init_db()

    def _connect(self) -> sqlite3.Connection:
        return sqlite3.connect(self.db_path)

    def _init_db(self) -> None:
        with self._connect() as conn:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS requests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    user_prompt TEXT NOT NULL,
                    sanitized_prompt TEXT,
                    model_response TEXT,
                    safe_response TEXT,
                    input_blocked INTEGER NOT NULL DEFAULT 0,
                    output_redacted INTEGER NOT NULL DEFAULT 0,
                    reasons TEXT,
                    llm_provider TEXT,
                    execution_status TEXT
                )
                """
            )
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    event_type TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    details TEXT NOT NULL
                )
                """
            )
            conn.commit()

    def save_request(
        self,
        user_prompt: str,
        sanitized_prompt: str,
        model_response: str,
        safe_response: str,
        input_blocked: bool,
        output_redacted: bool,
        reasons: str,
        llm_provider: str,
        execution_status: str = "not-run",
    ) -> int:
        with self._connect() as conn:
            cur = conn.execute(
                """
                INSERT INTO requests (
                    user_prompt, sanitized_prompt, model_response, safe_response,
                    input_blocked, output_redacted, reasons, llm_provider, execution_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    user_prompt,
                    sanitized_prompt,
                    model_response,
                    safe_response,
                    int(input_blocked),
                    int(output_redacted),
                    reasons,
                    llm_provider,
                    execution_status,
                ),
            )
            conn.commit()
            return int(cur.lastrowid)

    def save_audit(self, event_type: str, severity: str, details: str) -> None:
        with self._connect() as conn:
            conn.execute(
                "INSERT INTO audit_logs (event_type, severity, details) VALUES (?, ?, ?)",
                (event_type, severity, details),
            )
            conn.commit()

    def fetch_recent_logs(self, limit: int = 100) -> List[Dict[str, Any]]:
        with self._connect() as conn:
            conn.row_factory = sqlite3.Row
            rows = conn.execute(
                """
                SELECT id, timestamp, event_type, severity, details
                FROM audit_logs
                ORDER BY id DESC
                LIMIT ?
                """,
                (limit,),
            ).fetchall()
        return [dict(r) for r in rows]
