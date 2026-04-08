LLM Governance Backend (Local, Open Source)

Overview
- Flask backend with routes: /ask, /logs, /execute
- Governance engine for input/output filtering
- Local LLM via Ollama (no paid APIs)
- Secure execution with allowlisted commands
- SQLite + file logging for audit trail

Run
1. Install Python 3.10+
2. Install dependencies:
   pip install -r requirements.txt
3. Start backend:
   python app.py
4. Open browser:
   http://127.0.0.1:5000

Ollama Setup
1. Install Ollama from official site
2. Pull a local model:
   ollama pull llama3.2
3. Keep Ollama running locally

API
- POST /ask
  body: {"prompt": "..."}
- GET /logs?limit=30
- POST /execute
  body: {"command": ["echo", "hello"]}

Notes
- Existing Java project is untouched.
- This backend is added in parallel inside backend/.
