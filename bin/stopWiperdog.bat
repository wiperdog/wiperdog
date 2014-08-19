@echo off
set WIPERDOGHOME=%~dp0
set pidFile=%WIPERDOGHOME%\wiperdog.pid
echo %pidFile%
if exist %pidFile% (
    FOR /F "tokens=*" %%i IN (%pidFile%) DO TASKKILL /PID %%i /T /F  >nul 2>&1
	rem TASKKILL /PID %pid% /T /F  >nul 2>&1
	if errorlevel 1 (
		echo "Failed to stop Wiperdog !"
	) else ( 
		echo "Wiperdog is stopped !"
		del %pidFile% /f >nul 2>&1
	)
) else (
	echo Wiperdog not running
)
