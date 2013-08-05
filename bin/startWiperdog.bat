@echo off
setlocal
:: Wiperdog Service Startup Script for Windows

:: determine the home

set WIPERDOGHOME=%~dp0..
for %%i in ("%WIPERDOGHOME%") do set WIPERDOGHOME=%%~fsi

:: move to bin

cd "%WIPERDOGHOME%\bin"

:: execute
"%WIPERDOGHOME%\bin\groovy.bat" "%WIPERDOGHOME%\bin\startWiperdog.groovy"
endlocal