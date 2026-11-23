@echo off
rem Builds and runs the whole app on http://localhost:8080 in this one window.
rem Close the window (or press Ctrl+C) to stop it. Needs Java 17+ and Node.js 18+.
setlocal EnableExtensions
title Ember ^& Oak - Restaurant Booking
cd /d "%~dp0"

echo ============================================================
echo   Ember ^& Oak - Restaurant Booking
echo   Close this window or press Ctrl+C to stop the app.
echo ============================================================

where java >nul 2>&1 || (echo [ERROR] Java was not found. Install Java 17 or later from https://adoptium.net & goto :fail)
where npm >nul 2>&1 || (echo [ERROR] Node.js was not found. Install the LTS version from https://nodejs.org & goto :fail)

echo.
echo [1/3] Building the web interface...
pushd "src\frontend"
if not exist "node_modules" (
  call npm install --no-audit --no-fund || (popd & goto :fail)
)
call npm run build || (popd & goto :fail)
popd

echo.
echo [2/3] Building the server. The first run downloads Maven and libraries, which takes a few minutes...
pushd "src\backend"
call ".\mvnw.cmd" -q -DskipTests package || (popd & goto :fail)
set "JAR="
for %%J in (target\restaurant-booking-*.jar) do set "JAR=%%J"
if not defined JAR (popd & echo [ERROR] The server build produced no jar file. & goto :fail)

rem MySQL when the developer has put a login in application-local.properties, otherwise the in-memory demo database
set "PROFILE="
if exist "application-local.properties" (
  echo Database: MySQL, login from src\backend\application-local.properties
) else (
  set "PROFILE=--spring.profiles.active=h2"
  echo Database: H2 in memory. Sample data is created again on every start.
)

echo.
echo [3/3] Starting on http://localhost:8080 - the browser opens when the app is ready.
echo       Sample logins: manager / Manager@2026, waiter / Waiter@2026, kitchen / Kitchen@2026
echo.
rem Waits in the background until the server answers, then opens the browser once
if not defined NO_BROWSER start "" /b powershell -NoProfile -ExecutionPolicy Bypass -Command "for($i=0;$i -lt 180;$i++){try{(New-Object Net.Sockets.TcpClient('127.0.0.1',8080)).Close(); Start-Sleep -Seconds 2; Start-Process 'http://localhost:8080'; exit}catch{Start-Sleep -Seconds 1}}"
java -jar "%JAR%" %PROFILE% --spring.web.resources.static-locations=file:../frontend/dist/
popd

echo.
echo The app has stopped. If it stopped by itself, read the messages above,
echo for example "Port 8080 was already in use": close the other window that runs the app.
pause
exit /b 0

:fail
echo.
echo The app could not start. Read the messages above, fix the problem, then run start.bat again.
pause
exit /b 1
