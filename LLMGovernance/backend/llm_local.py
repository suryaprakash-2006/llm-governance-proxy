"""Local LLM adapter supporting Ollama (default) and optional HuggingFace."""

from __future__ import annotations

import json
import urllib.request
import urllib.error


class LocalLLM:
    def __init__(self, provider: str = "ollama", model: str = "llama3.2") -> None:
        self.provider = provider.lower().strip()
        self.model = model.strip()

    def generate(self, prompt: str) -> str:
        if self.provider == "ollama":
            return self._generate_ollama(prompt)
        if self.provider == "huggingface":
            return self._generate_hf_fallback(prompt)
        raise ValueError(f"Unsupported local provider: {self.provider}")

    def _generate_ollama(self, prompt: str) -> str:
        url = "http://localhost:11434/api/generate"
        payload = {
            "model": self.model,
            "prompt": prompt,
            "stream": False,
        }

        req = urllib.request.Request(
            url=url,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )

        try:
            with urllib.request.urlopen(req, timeout=120) as resp:
                data = json.loads(resp.read().decode("utf-8"))
            return data.get("response", "").strip() or "[Local model returned empty output]"
        except urllib.error.URLError as exc:
            return (
                "[Ollama unavailable. Start Ollama and pull a model, for example: "
                "ollama pull llama3.2] "
                f"Details: {exc}"
            )

    def _generate_hf_fallback(self, prompt: str) -> str:
        return (
            "[HuggingFace local pipeline is not configured in this minimal build. "
            "Use provider=ollama for now.]\n"
            f"Prompt preview: {prompt[:120]}"
        )
