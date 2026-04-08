"""Governance engine for prompt/response safety.

This module is framework-agnostic so it can be reused in Flask routes.
"""

from __future__ import annotations

from dataclasses import dataclass, field
import re
from typing import List, Tuple


EMAIL_PATTERN = re.compile(r"[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}")
PHONE_PATTERN = re.compile(r"\b(?:\+?\d{1,3}[\s\-]?)?(?:\d[\s\-]?){10,13}\b")


@dataclass
class GovernanceDecision:
    allowed: bool
    blocked: bool
    reasons: List[str] = field(default_factory=list)
    detected_entities: List[str] = field(default_factory=list)
    sanitized_input: str = ""


@dataclass
class OutputDecision:
    safe_output: str
    redactions_applied: bool
    leakage_detected: bool
    reasons: List[str] = field(default_factory=list)


class GovernanceEngine:
    """Rule-based governance checks for input and output."""

    DEFAULT_BLOCK_KEYWORDS = {
        "password",
        "admin",
        "system",
        "root",
        "token",
        "api key",
        "secret",
        "confidential",
    }

    def __init__(self, block_keywords: List[str] | None = None) -> None:
        keywords = block_keywords if block_keywords is not None else list(self.DEFAULT_BLOCK_KEYWORDS)
        self.block_keywords = {k.strip().lower() for k in keywords if k and k.strip()}

    def evaluate_input(self, user_text: str) -> GovernanceDecision:
        if user_text is None:
            user_text = ""

        reasons: List[str] = []
        detected: List[str] = []

        lowered = user_text.lower()
        matched_keywords = [k for k in self.block_keywords if k in lowered]
        if matched_keywords:
            reasons.append(f"Blocked keyword(s): {', '.join(sorted(matched_keywords))}")
            detected.extend([f"keyword:{k}" for k in sorted(matched_keywords)])

        emails = EMAIL_PATTERN.findall(user_text)
        if emails:
            detected.extend([f"email:{e}" for e in emails])

        phones = [p.strip() for p in PHONE_PATTERN.findall(user_text)]
        if phones:
            detected.extend([f"phone:{p}" for p in phones])

        sanitized = self._sanitize_input(user_text)
        blocked = len(matched_keywords) > 0

        return GovernanceDecision(
            allowed=not blocked,
            blocked=blocked,
            reasons=reasons,
            detected_entities=detected,
            sanitized_input=sanitized,
        )

    def filter_output(self, model_output: str) -> OutputDecision:
        if model_output is None:
            model_output = ""

        reasons: List[str] = []
        redactions = False
        leakage = False

        safe_output, has_email = self._mask_emails(model_output)
        if has_email:
            redactions = True
            leakage = True
            reasons.append("Email pattern detected and redacted from output")

        safe_output, has_phone = self._mask_phones(safe_output)
        if has_phone:
            redactions = True
            leakage = True
            reasons.append("Phone pattern detected and redacted from output")

        return OutputDecision(
            safe_output=safe_output,
            redactions_applied=redactions,
            leakage_detected=leakage,
            reasons=reasons,
        )

    def _sanitize_input(self, text: str) -> str:
        text, _ = self._mask_emails(text)
        text, _ = self._mask_phones(text)
        return text

    @staticmethod
    def _mask_emails(text: str) -> Tuple[str, bool]:
        found = False

        def repl(match: re.Match[str]) -> str:
            nonlocal found
            found = True
            email = match.group(0)
            at_idx = email.find("@")
            local = email[:at_idx]
            domain = email[at_idx:]
            prefix = local[:1] if local else "*"
            return f"{prefix}***{domain}"

        return EMAIL_PATTERN.sub(repl, text), found

    @staticmethod
    def _mask_phones(text: str) -> Tuple[str, bool]:
        found = False

        def repl(match: re.Match[str]) -> str:
            nonlocal found
            found = True
            raw = match.group(0)
            digits = "".join(ch for ch in raw if ch.isdigit())
            if len(digits) < 4:
                return "[PHONE_REDACTED]"
            return f"[PHONE_REDACTED:{digits[-4:]}]"

        return PHONE_PATTERN.sub(repl, text), found
