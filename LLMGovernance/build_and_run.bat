@echo off
:: =============================================================
::  LLM Data Leak Prevention System - Build & Run Script
::  Works on Windows with JDK 11 or higher installed
:: =============================================================

setlocal
set "PROJECT_DIR=%~dp0"
set "SRC_DIR=%PROJECT_DIR%src"
set "OUT_DIR=%PROJECT_DIR%out"
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
javac -d "%OUT_DIR%" -sourcepath "%SRC_DIR%" @"%SOURCES_FILE%" 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed. See errors above.
    pause
    exit /b 1
)

echo [OK] Compilation successful.
echo.

:: ── Run ───────────────────────────────────────────────────────
echo [3/3] Launching application...
echo.
java -cp "%OUT_DIR%" %MAIN_CLASS%

endlocal
