@echo off
echo Building the Expense Splitter application...
call mvn clean package -DskipTests

if %ERRORLEVEL% neq 0 (
    echo Maven build failed.
    exit /b %ERRORLEVEL%
)

echo Packaging application with jpackage...
REM Create an input directory for jpackage to avoid packaging unnecessary files
mkdir target\jpackage-input
copy target\expense-splitter-1.0-SNAPSHOT.jar target\jpackage-input\

REM We use org.springframework.boot.loader.launch.JarLauncher as the main class for Spring Boot 3
jpackage ^
  --type app-image ^
  --name "Expense Splitter" ^
  --input target\jpackage-input ^
  --main-jar expense-splitter-1.0-SNAPSHOT.jar ^
  --main-class org.springframework.boot.loader.launch.JarLauncher

if %ERRORLEVEL% neq 0 (
    echo jpackage packaging failed.
    exit /b %ERRORLEVEL%
)

echo Successfully created Windows installer (Expense Splitter-1.0.exe)!
