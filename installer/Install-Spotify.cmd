@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Install-Spotify.ps1"
echo.
echo Keep any error above if the installation stopped.
pause
