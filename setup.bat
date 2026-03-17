@echo off
setlocal

echo ==========================================
echo    Expense Splitter Pro - Setup Script
echo ==========================================
echo.

:: Check for Java
echo [1/3] Checking for Java JDK 17+...
set "JAVA_VER="
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VER=%%g
)

if "%JAVA_VER%"=="" (
    echo [ERROR] Java not found. Please install JDK 17 or higher.
    echo Visit: https://openjdk.org/
    pause
    exit /b 1
)
echo Found Java: %JAVA_VER%

:: Check for Maven
echo.
echo [2/3] Checking for Maven...
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Maven not found. Please install Maven 3.6 or higher.
    echo Visit: https://maven.apache.org/
    pause
    exit /b 1
)
mvn -version | findstr /i "Apache Maven"

:: Build and Install
echo.
echo [3/3] Installing dependencies and building project...
echo This may take a few minutes for the first setup...
echo.
call mvn clean install -DskipTests

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Build failed. Please check the logs above.
    pause
    exit /b 1
)

echo.
echo ==========================================
echo    Setup completed successfully!
echo    You can now run the app using start.bat
echo ==========================================
echo.
pause
