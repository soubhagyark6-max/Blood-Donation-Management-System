@echo off
setlocal
cd /d "%~dp0.."

title LifeLink Blood Donation System

echo.
echo ========================================
echo       LIFELINK BLOOD DONATION SYSTEM
echo ========================================
echo.
echo Starting LifeLink...
echo Please wait.
echo.

if not exist "C:\apache-maven-3.10.0\bin\mvn.cmd" (
    echo ERROR: Maven not found.
    pause
    exit /b 1
)

start "" cmd /c "timeout /t 20 /nobreak >nul & start http://localhost:8081"

call "C:\apache-maven-3.10.0\bin\mvn.cmd" "-Dmaven.test.skip=true" spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"

echo.
echo LifeLink has stopped.
pause