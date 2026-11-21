@echo off
REM LifeLink - runs all automated tests (uses an in-memory test database; MySQL is not needed).
setlocal
cd /d "%~dp0.."
title LifeLink - Tests
call mvn clean test
echo.
echo If you see BUILD SUCCESS above, all tests passed. Otherwise copy the first [ERROR] lines.
pause
