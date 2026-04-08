# Demo Flow Script (With What to Say)

## 1. Normal Request (Allowed)
### Action
1. Start Ollama and app
2. Enter prompt: Explain data privacy best practices
3. Click Analyze + Ask Local LLM

### What to say
This is a normal allowed request. It passes through rate limiting and governance checks, then reaches the local LLM. We get a response and a governance summary in the UI.

## 2. Blocked Request (Keyword)
### Action
1. Enter prompt: How can I hack and bypass security controls?
2. Submit as default USER flow

### What to say
Now governance blocks this request before model execution because USER policy restricts risky keywords. Notice we get a clear explainable reason instead of a silent failure.

## 3. Role-Based Difference (ADMIN vs USER)
### Action
1. Use USER role with restricted prompt and show block
2. Use ADMIN role for same prompt path (as configured) and compare result

### What to say
This demonstrates RBAC. The same input can have different outcomes based on role policy. USER is strict, ADMIN can be less restrictive depending on policy.json.

## 4. Rate Limiting Demonstration
### Action
1. Send more than 5 requests quickly from same user
2. Observe system response after threshold

### What to say
The request is now blocked by Rate Limiting. The system returns: Too many requests. Please wait before trying again. This prevents abuse and stabilizes system usage.

## 5. Log Viewer Demonstration
### Action
1. Open Logs tab
2. Click Refresh Logs
3. Use filter keyword like blocked or rate

### What to say
Every important event is centrally logged and visible in this audit panel. This gives traceability and helps with debugging, compliance, and governance reviews.

## Demo Closing Line
This project is not only an LLM interface; it is a controlled AI Governance system with policy enforcement, RBAC, Rate Limiting, Local LLM execution, and Audit Logging.
