@ECHO OFF
@REM ----- INIT SCREEN -----
TITLE Using command line to create new job !!!
COLOR 0f
CLS

@REM ----- SET DEFAULT DIRECTORY -----
SET DIRNAME=%~dp0
IF "%DIRNAME%" == "" SET DIRNAME=.\

@REM ----- GET INFORMATION FOR JOB FROM CONSOLE -----
ECHO --------------- CREATE JOB: %2 ---------------
ECHO.

SET inputResult=%*
IF "%1"=="--n" (
	"%DIRNAME%\groovy.bat" "%DIRNAME%\genjob.groovy" %inputResult%
) ELSE (
	ECHO "Incorrect format !!!"
	ECHO "Correct format of command: "
	ECHO "genjob --n <jobName> [--f <strFetchAction>] [--q <strQuery>] [--c <strCommand>] [--d <strDbExec>] [--fp <pathToFile>]"
)