@echo off
setlocal
:: Wiperdog Service Startup Script for Windows

:: determine the home

set WIPERDOGHOME=%~dp0..
for %%i in ("%WIPERDOGHOME%") do set WIPERDOGHOME=%%~fsi

:: move to bin

cd "%WIPERDOGHOME%\bin"

REM TODO: MULTI WIPERDOG INSTANCE
set FORK_PREFIX="%WIPERDOGHOME%"
set FELIX_PORT="%port%"
set BUNDLE_LIST="%bundle%"
REM Override felix port and bundle if user specify -f and -b flag for port and bundle parameters 
IF "%1%" == "-f" (
	SET FELIX_PORT="%2%"
)
IF "%3%" == "-f" (
	SET FELIX_PORT="%4%"
)
IF "%1%" == "-b" (
	SET BUNDLE_LIST="%2%" 
)
IF "%3%" == "-b" ( 
	SET BUNDLE_LIST="%4%"
)
echo Felix port set to %FELIX_PORT%
echo Bundle list set to %BUNDLE_LIST%

:: Test if felix port not set
IF %FELIX_PORT% == "" GOTO FELIX_PORT_UNSET_BLOCK

SET FORK_PREFIX=%WIPERDOGHOME%/fork/%FELIX_PORT%

IF EXIST "%FORK_PREFIX%" GOTO MAIN_PROG
echo Fork folder does not exists for port %FELIX_PORT%, try to create fork now
CALL "%WIPERDOGHOME%"\bin\createFork.bat -f %FELIX_PORT%
GOTO CHECK_CREATE

:FELIX_PORT_UNSET_BLOCK
  echo Start wiperdog without specified an instance
  echo NOTICE: You can start with specific instance by syntax:
  echo ./startWiperdog.sh -f xxxyyy -b ListBundleX.csv

:CHECK_CREATE
IF NOT EXIST "%FORK_PREFIX%" GOTO :EOF

:MAIN_PROG
REM END TODO

:: temporary memory size setting, change this if this is too small(or too big).
set JAVA_OPTS=-Xmx256m

:: execute
"%WIPERDOGHOME%\bin\groovy.bat" "%WIPERDOGHOME%\bin\startWiperdog.groovy" -f %FELIX_PORT% -b %BUNDLE_LIST%
endlocal