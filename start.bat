@echo off

echo Building the project...
call mvn compile

echo.
echo Starting Expense Splitter Pro...
echo.
call mvn javafx:run

echo.
pause
