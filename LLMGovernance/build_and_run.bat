@echo off
:: =============================================================
::  LLM Data Leak Prevention System - Build & Run Script
::  Works on Windows with JDK 11 or higher installed
:: =============================================================

setlocal
set "PROJECT_DIR=%~dp0"
set "SRC_DIR=%PROJECT_DIR%src"
set "OUT_DIR=%PROJECT_DIR%out"
set "LIB_DIR=%PROJECT_DIR%lib"
set "MYSQL_JAR=%LIB_DIR%\mysql-connector-j-8.4.0.jar"
set "SLF4J_API_JAR=%LIB_DIR%\slf4j-api-2.0.13.jar"
set "SLF4J_SIMPLE_JAR=%LIB_DIR%\slf4j-simple-2.0.13.jar"
set "MAIN_CLASS=com.llmgovernance.system.main.MainApp"

echo =============================================================
echo   LLM Data Leak Prevention System
echo   Build Script for Windows
echo =============================================================
echo.

:: ── Check Java ────────────────────────────────────────────────
where javac >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] javac not found! Please install JDK 11 or higher.
    echo         Download: https://adoptium.net/
    echo         Then add JAVA_HOME\bin to your PATH.
    pause
    exit /b 1
)

echo [1/3] Java compiler found:
javac -version
echo.

if not exist "%LIB_DIR%" mkdir "%LIB_DIR%"
if not exist "%MYSQL_JAR%" (
    echo [INFO] MySQL JDBC driver not found. Downloading...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar' -OutFile '%MYSQL_JAR%'"
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Failed to download mysql-connector-j jar.
        echo         Download manually and place at:
        echo         %MYSQL_JAR%
        pause
        exit /b 1
    )
)

if not exist "%SLF4J_API_JAR%" (
    echo [INFO] slf4j-api jar not found. Downloading...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar' -OutFile '%SLF4J_API_JAR%'"
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Failed to download slf4j-api jar.
        pause
        exit /b 1
    )
)

if not exist "%SLF4J_SIMPLE_JAR%" (
    echo [INFO] slf4j-simple jar not found. Downloading...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.13/slf4j-simple-2.0.13.jar' -OutFile '%SLF4J_SIMPLE_JAR%'"
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Failed to download slf4j-simple jar.
        pause
        exit /b 1
    )
)

:: ── Create output directory ───────────────────────────────────
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

:: ── Collect all .java source files ───────────────────────────
echo [2/3] Compiling all Java source files...
set "SOURCES_FILE=%PROJECT_DIR%sources.txt"
if exist "%SOURCES_FILE%" del "%SOURCES_FILE%"

for /r "%SRC_DIR%" %%f in (*.java) do (
    echo %%f >> "%SOURCES_FILE%"
)

:: ── Compile ───────────────────────────────────────────────────
javac -cp "%LIB_DIR%\*" -d "%OUT_DIR%" -sourcepath "%SRC_DIR%" @"%SOURCES_FILE%" 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed. See errors above.
    pause
    exit /b 1
)

echo [OK] Compilation successful.
echo.

:: ── MySQL connection settings ─────────────────────────────────
if "%LLM_DB_HOST%"=="" set "LLM_DB_HOST=localhost"
if "%LLM_DB_PORT%"=="" set "LLM_DB_PORT=3306"
if "%LLM_DB_NAME%"=="" set "LLM_DB_NAME=llm_governance"

if "%LLM_DB_USER%"=="" (
    set /p LLM_DB_USER=Enter MySQL username [llm_app]: 
    if "%LLM_DB_USER%"=="" set "LLM_DB_USER=llm_app"
)

if "%LLM_DB_PASSWORD%"=="" (
    set /p LLM_DB_PASSWORD=Enter MySQL password for user %LLM_DB_USER%: 
)

if "%LLM_DEMO_MODE%"=="" set "LLM_DEMO_MODE=true"

echo [DB] Host=%LLM_DB_HOST% Port=%LLM_DB_PORT% DB=%LLM_DB_NAME% User=%LLM_DB_USER%
echo [DEMO] LLM_DEMO_MODE=%LLM_DEMO_MODE%
echo.

:: ── Run ───────────────────────────────────────────────────────
echo [3/3] Launching application...
echo.
java -Dllm.db.host=%LLM_DB_HOST% -Dllm.db.port=%LLM_DB_PORT% -Dllm.db.name=%LLM_DB_NAME% -Dllm.db.user=%LLM_DB_USER% -Dllm.db.password=%LLM_DB_PASSWORD% -Dllm.demo.mode=%LLM_DEMO_MODE% -cp "%OUT_DIR%;%LIB_DIR%\*" %MAIN_CLASS%

endlocal
