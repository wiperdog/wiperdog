@echo off
setlocal
:: Wiperdog Service Startup Script for Windows

:: determine the home

set WIPERDOGHOME=%~dp0..
for %%i in ("%WIPERDOGHOME%") do set WIPERDOGHOME=%%~fsi

:: move to bin

cd "%WIPERDOGHOME%\bin"
:: temporary memory size setting, change this if this is too small(or too big).
set JAVA_OPTS=-Xmx256m
:: execute
"%WIPERDOGHOME%\bin\groovy.bat" "%WIPERDOGHOME%\bin\startWiperdog.groovy" %*
endlocal