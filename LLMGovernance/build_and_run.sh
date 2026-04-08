#!/bin/bash
# =============================================================
#  LLM Data Leak Prevention System - Build & Run Script
#  Works on Linux / macOS with JDK 11+ installed
# =============================================================

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/src"
OUT_DIR="$PROJECT_DIR/out"
MAIN_CLASS="com.llmgovernance.system.main.MainApp"

echo "============================================================="
echo "  LLM Data Leak Prevention System"
echo "  Build Script for Linux / macOS"
echo "============================================================="
echo

# ── Check Java ────────────────────────────────────────────────
if ! command -v javac &> /dev/null; then
    echo "[ERROR] javac not found! Install JDK 11+:"
    echo "        Ubuntu/Debian : sudo apt install default-jdk"
    echo "        macOS (Brew)  : brew install openjdk"
    echo "        Or download   : https://adoptium.net/"
    exit 1
fi

echo "[1/3] Java compiler found:"
javac -version
echo

# ── Create output directory ───────────────────────────────────
mkdir -p "$OUT_DIR"

# ── Compile ───────────────────────────────────────────────────
echo "[2/3] Compiling all Java source files..."
find "$SRC_DIR" -name "*.java" > /tmp/llm_sources.txt

javac -d "$OUT_DIR" -sourcepath "$SRC_DIR" @/tmp/llm_sources.txt
if [ $? -ne 0 ]; then
    echo
    echo "[ERROR] Compilation failed. See errors above."
    exit 1
fi

echo "[OK] Compilation successful."
echo

# ── Run ───────────────────────────────────────────────────────
echo "[3/3] Launching application..."
echo
java -cp "$OUT_DIR" "$MAIN_CLASS"
