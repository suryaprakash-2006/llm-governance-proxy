"""Secure execution module with allowlisted command execution."""

from __future__ import annotations

import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import List


@dataclass
class ExecutionResult:
    success: bool
    stdout: str
    stderr: str
    return_code: int


class SecureExecutor:
    """Runs only allowlisted tools with timeout and no shell expansion."""

    ALLOWLIST = {"python", "python3", "dosbox", "echo"}

    def __init__(self, workspace_root: Path) -> None:
        self.workspace_root = workspace_root

    def run(self, command: List[str], timeout_sec: int = 20) -> ExecutionResult:
        if not command:
            return ExecutionResult(False, "", "No command provided", -1)

        exe = command[0].lower()
        if exe not in self.ALLOWLIST:
            return ExecutionResult(False, "", f"Command blocked by policy: {exe}", -1)

        try:
            proc = subprocess.run(
                command,
                cwd=str(self.workspace_root),
                capture_output=True,
                text=True,
                timeout=timeout_sec,
                shell=False,
            )
            return ExecutionResult(
                success=(proc.returncode == 0),
                stdout=proc.stdout,
                stderr=proc.stderr,
                return_code=proc.returncode,
            )
        except subprocess.TimeoutExpired:
            return ExecutionResult(False, "", "Execution timed out", -1)
        except Exception as exc:
            return ExecutionResult(False, "", f"Execution error: {exc}", -1)
