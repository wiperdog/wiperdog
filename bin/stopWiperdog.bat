@echo off
rem  BASEDIR is Wiperdoghome/bin
set BASEDIR=%~dp0
set /a count=0
for /f %%f in ('dir /s/b %BASEDIR% ^| findstr pid ') do (
	if exist %%f (
		FOR /F "tokens=*" %%i IN (%%f) DO ( 
			set /a count+=1
			TASKKILL /PID %%i /T /F  >nul 2>&1
		rem	TASKKILL /PID %pid% /T /F  >nul 2>&1
			if errorlevel 1 (
				echo "Failed to stop Wiperdog with pid %%i !"
			) else ( 
				echo Wiperdog process with pid %%i is stopped !
				del %%f /f >nul 2>&1
				
			)
		)
	)
)
if  %count% == 0 (
	echo Wiperdog not running
) 