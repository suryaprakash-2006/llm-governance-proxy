#!/bin/bash
# =============================================================
#  LLM Data Leak Prevention System - Build & Run Script
#  Works on Linux / macOS with JDK 11+ installed
# =============================================================

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/src"
OUT_DIR="$PROJECT_DIR/out"
LIB_DIR="$PROJECT_DIR/lib"
MYSQL_JAR="$LIB_DIR/mysql-connector-j-8.4.0.jar"
SLF4J_API_JAR="$LIB_DIR/slf4j-api-2.0.13.jar"
SLF4J_SIMPLE_JAR="$LIB_DIR/slf4j-simple-2.0.13.jar"
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

mkdir -p "$LIB_DIR"
if [ ! -f "$MYSQL_JAR" ]; then
    echo "[INFO] MySQL JDBC driver not found. Downloading..."
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar" -o "$MYSQL_JAR"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar" -O "$MYSQL_JAR"
    else
        echo "[ERROR] curl/wget not found to download mysql-connector-j jar."
        echo "        Download manually and place at: $MYSQL_JAR"
        exit 1
    fi
fi

if [ ! -f "$SLF4J_API_JAR" ]; then
    echo "[INFO] slf4j-api jar not found. Downloading..."
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar" -o "$SLF4J_API_JAR"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar" -O "$SLF4J_API_JAR"
    else
        echo "[ERROR] curl/wget not found to download slf4j-api jar."
        exit 1
    fi
fi

if [ ! -f "$SLF4J_SIMPLE_JAR" ]; then
    echo "[INFO] slf4j-simple jar not found. Downloading..."
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.13/slf4j-simple-2.0.13.jar" -o "$SLF4J_SIMPLE_JAR"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.13/slf4j-simple-2.0.13.jar" -O "$SLF4J_SIMPLE_JAR"
    else
        echo "[ERROR] curl/wget not found to download slf4j-simple jar."
        exit 1
    fi
fi

# ── Create output directory ───────────────────────────────────
mkdir -p "$OUT_DIR"

# ── Compile ───────────────────────────────────────────────────
echo "[2/3] Compiling all Java source files..."
find "$SRC_DIR" -name "*.java" > /tmp/llm_sources.txt

javac -cp "$LIB_DIR/*" -d "$OUT_DIR" -sourcepath "$SRC_DIR" @/tmp/llm_sources.txt
if [ $? -ne 0 ]; then
    echo
    echo "[ERROR] Compilation failed. See errors above."
    exit 1
fi

echo "[OK] Compilation successful."
echo

# ── MySQL connection settings ─────────────────────────────────
LLM_DB_HOST="${LLM_DB_HOST:-localhost}"
LLM_DB_PORT="${LLM_DB_PORT:-3306}"
LLM_DB_NAME="${LLM_DB_NAME:-llm_governance}"

if [ -z "$LLM_DB_USER" ]; then
        read -r -p "Enter MySQL username [llm_app]: " LLM_DB_USER
        LLM_DB_USER="${LLM_DB_USER:-llm_app}"
fi

if [ -z "$LLM_DB_PASSWORD" ]; then
        read -r -s -p "Enter MySQL password for user $LLM_DB_USER: " LLM_DB_PASSWORD
        echo
fi

LLM_DEMO_MODE="${LLM_DEMO_MODE:-true}"

echo "[DB] Host=$LLM_DB_HOST Port=$LLM_DB_PORT DB=$LLM_DB_NAME User=$LLM_DB_USER"
echo "[DEMO] LLM_DEMO_MODE=$LLM_DEMO_MODE"
echo

# ── Run ───────────────────────────────────────────────────────
echo "[3/3] Launching application..."
echo
java \
    -Dllm.db.host="$LLM_DB_HOST" \
    -Dllm.db.port="$LLM_DB_PORT" \
    -Dllm.db.name="$LLM_DB_NAME" \
    -Dllm.db.user="$LLM_DB_USER" \
    -Dllm.db.password="$LLM_DB_PASSWORD" \
    -Dllm.demo.mode="$LLM_DEMO_MODE" \
    -cp "$OUT_DIR:$LIB_DIR/*" "$MAIN_CLASS"
