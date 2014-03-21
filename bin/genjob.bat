@ECHO OFF
setlocal
@REM ----- INIT SCREEN -----
TITLE Using command line to create new job !!!
COLOR 0f
CLS

@REM ----- SET WIPERDOG HOME DIRECTORY -----
set WIPERDOGHOME=%~dp0..
for %%i in ("%WIPERDOGHOME%") do set WIPERDOGHOME=%%~fsi
cd "%WIPERDOGHOME%\bin"

@REM ----- GET INFORMATION FOR JOB FROM CONSOLE -----
ECHO --------------- CREATE JOB: %2 ---------------
ECHO.

SET inputResult=%*
IF "%1"=="-n" (
	"%WIPERDOGHOME%\bin\groovy.bat" "%WIPERDOGHOME%\bin\genjob.groovy" %inputResult%
) ELSE (
	ECHO "Incorrect format !!!"
	ECHO "Correct format of command: "
	ECHO "genjob -n <jobName> [-f <strFetchAction>] [-q <strQuery>] [-c <strCommand>] [-d <strDbExec>] [-fp <pathToFile>]"
)
endlocal