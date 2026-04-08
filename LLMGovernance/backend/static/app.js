async function askLLM() {
  const prompt = document.getElementById("prompt").value.trim();
  const responseBox = document.getElementById("response");
  const detectedBox = document.getElementById("detected");

  if (!prompt) {
    responseBox.textContent = "Please enter a prompt.";
    return;
  }

  responseBox.textContent = "Processing...";

  const res = await fetch("/ask", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ prompt }),
  });

  const data = await res.json();

  if (!data.ok && data.blocked) {
    responseBox.textContent = "Blocked by governance policy.\n" + (data.reasons || []).join("\n");
    detectedBox.textContent = (data.detected || []).join("\n") || "None";
    return;
  }

  if (!data.ok) {
    responseBox.textContent = "Error: " + (data.error || "Unknown error");
    return;
  }

  responseBox.textContent = data.response || "[Empty response]";
  detectedBox.textContent = (data.detected || []).join("\n") || "None";
}

async function loadLogs() {
  const logsBox = document.getElementById("logs");
  logsBox.textContent = "Loading logs...";

  const res = await fetch("/logs?limit=30");
  const data = await res.json();

  if (!data.ok) {
    logsBox.textContent = "Could not load logs.";
    return;
  }

  if (!data.logs.length) {
    logsBox.textContent = "No logs yet.";
    return;
  }

  logsBox.textContent = data.logs
    .map((row) => `${row.timestamp} | ${row.severity} | ${row.event_type} | ${row.details}`)
    .join("\n");
}

async function runExecution() {
  const raw = document.getElementById("execCmd").value.trim();
  const resultBox = document.getElementById("execResult");

  if (!raw) {
    resultBox.textContent = "Enter command array. Example: [\"echo\", \"hello\"]";
    return;
  }

  let command;
  try {
    command = JSON.parse(raw);
    if (!Array.isArray(command)) throw new Error("not array");
  } catch (_) {
    resultBox.textContent = "Invalid JSON array format.";
    return;
  }

  resultBox.textContent = "Running...";

  const res = await fetch("/execute", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ command }),
  });
  const data = await res.json();

  resultBox.textContent = JSON.stringify(data, null, 2);
  loadLogs();
}

document.getElementById("askBtn").addEventListener("click", askLLM);
document.getElementById("logsBtn").addEventListener("click", loadLogs);
document.getElementById("execBtn").addEventListener("click", runExecution);

loadLogs();
