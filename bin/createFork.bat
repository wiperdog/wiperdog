@echo off
setlocal
:: current dir
set CURRENT_DIR=%cd%/

:: determine the home

set WIPERDOGHOME=%~dp0..
for %%i in ("%WIPERDOGHOME%") do set WIPERDOGHOME=%%~fsi

:: move to bin

cd "%WIPERDOGHOME%\bin"

SET /A ARGS_COUNT=0    
FOR %%A in (%*) DO SET /A ARGS_COUNT+=1
if %ARGS_COUNT% == 2 (
	"%WIPERDOGHOME%\bin\groovy.bat" "%WIPERDOGHOME%\bin\createFork.groovy" %1 %2
) ELSE (
	ECHO Incorrect parameters !!!
	ECHO Correct format of commmand: createFork -f portForFork
    ECHO Example: createFork.sh -f 23111
	PAUSE
)
endlocal