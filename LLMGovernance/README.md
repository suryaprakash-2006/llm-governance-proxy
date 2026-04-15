# 🚀 LLMGovernance: Policy-Driven AI Control System for Local LLMs

LLMGovernance is a Java-based system that introduces a **governance layer between users and Large Language Models (LLMs)** to ensure safe, controlled, and explainable AI interactions.

Unlike traditional AI applications where prompts are directly sent to the model, this system enforces **policy validation, role-based access control (RBAC), rate limiting, and audit logging** before and after every model interaction.

This transforms a standard LLM into a **secure, auditable, and policy-compliant AI system**.

---

## 📄 Abstract

LLMGovernance is a desktop application designed to demonstrate responsible AI deployment with strong governance controls. Instead of directly sending user prompts to a model, every request passes through a governance pipeline that enforces policy rules, role-based access control (ADMIN and USER), input validation, output redaction, and per-user rate limiting.

The system uses a local LLM via Ollama, writes centralized logs, and provides an in-app audit viewer for transparency.

---

## 🧠 Key Idea

Instead of:

User → LLM  

We implement:

User → Governance Layer → LLM → Governance Layer → Output  

This ensures:
- Unsafe inputs are blocked  
- Outputs are controlled  
- Every decision is explainable  
- All actions are logged  

---

## ❓ Why Governance Is Needed in LLM Systems

Modern LLMs are powerful, but without controls they can:
- Leak sensitive data  
- Process harmful prompts  
- Be abused through repeated requests  

This system solves that by:
- Validating inputs before execution  
- Enforcing role-based restrictions  
- Limiting misuse with rate control  
- Providing explainable decisions  
- Enabling full audit visibility  

---

## ⚙️ Core Features

1. Governance pipeline for request and response safety  
2. Policy-driven filtering using JSON configuration  
3. Role-Based Access Control (ADMIN and USER)  
4. Explainable decisions (allowed + reason)  
5. Rate Limiting: 5 requests per minute per user  
6. Local LLM integration via Ollama (no paid APIs)  
7. Centralized audit logging system  
8. Built-in Log Viewer for monitoring  
9. SQL-based authentication (ADMIN / USER) with login screen  
10. Real MySQL tables for users and prompt records  

---

## 🔧 Architecture Flow

User Input  
→ ReplyService  
→ RateLimiter (5 req/min/user)  
→ GovernanceEngine (Policy + RBAC)  
→ Input Validation & Sanitization  
→ Local LLM (Ollama)  
→ Output Filtering & Redaction  
→ Explainable Decision (allowed + reason)  
→ Logging (Audit Trail)  
→ UI Display (Swing + Logs Tab)  

---

## 🧰 Tech Stack

- Java (JDK 11+) with Swing UI  
- Ollama (Local LLM runtime)  
- MySQL (real relational DB tables via JDBC)  
- JSON for policy configuration  
- Java logging utilities  
- In-memory rate limiting  

---

## ▶️ How to Run

### Prerequisites

1. Install JDK 11 or higher  
2. Install Ollama  
3. Pull a model:
   ollama pull llama3.2  
4. Run model:
   ollama run llama3.2  
5. Internet on first run to download MySQL Connector/J automatically (or place jar manually in `lib/`)  

### MySQL Setup

Create a MySQL database and user, then set these environment variables or Java system properties before running:

- `LLM_DB_HOST` / `llm.db.host` default: `localhost`
- `LLM_DB_PORT` / `llm.db.port` default: `3306`
- `LLM_DB_NAME` / `llm.db.name` default: `llm_governance`
- `LLM_DB_USER` / `llm.db.user` default: `root`
- `LLM_DB_PASSWORD` / `llm.db.password` default: empty

Example:

```sql
CREATE DATABASE llm_governance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Login Credentials (Default)

- ADMIN: `admin` / `admin123`  
- USER: `user` / `user123`  

Credentials are stored in the MySQL `users` table.

---

### Windows

1. Open terminal in project folder  
2. Run:
   build_and_run.bat  

---

### Manual Compile

1. Generate sources:
   dir /s /b src\*.java > sources.txt  

2. Compile:
   javac -cp "lib/*" -d out -sourcepath src @sources.txt  

3. Run:
   java -cp "out;lib/*" com.llmgovernance.system.main.MainApp  

---

### Linux / macOS

chmod +x build_and_run.sh  
./build_and_run.sh  

---

## 🧾 policy.json Configuration

Example:

{
  "global": {
    "maskEmail": true,
    "maskPhone": true,
    "maxInputLength": 500
  },
  "roles": {
    "ADMIN": {
      "blockedKeywords": []
    },
    "USER": {
      "blockedKeywords": ["hack", "bypass", "token"]
    }
  }
}

How it works:
- global rules apply to all users  
- role rules override behavior per role  
- safe defaults used if config fails  

---

## 📸 Screenshots

- Main Dashboard (add screenshot)  
- Allowed Prompt Response  
- Blocked Prompt with Reason  
- Rate Limit Trigger  
- Log Viewer Panel  

---

## 🎯 What Makes This Project Unique

- Dedicated **AI Governance Layer**  
- Fully **local-first AI system**  
- Combines **security + control + explainability**  
- Demonstrates **real-world AI safety architecture**  

---

## 🔮 Future Enhancements

- Dynamic policy updates without restart  
- Web-based monitoring dashboard  
- Exportable audit reports  
- Automated governance testing  

---