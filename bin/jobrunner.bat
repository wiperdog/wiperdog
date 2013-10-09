@echo off
setlocal

set DIRNAME=%~dp0

set isparamok=true

:: determine the home

set WIPERDOGHOME=%~dp0..
for %%i in ("%WIPERDOGHOME%") do set WIPERDOGHOME=%%~fsi

:: move to bin

cd "%WIPERDOGHOME%\bin"

IF [%5] EQU [] (
	IF [%4] EQU [] (
		set isparamok=false
	)
) ELSE (
	set isparamok=false
)
IF [%3] EQU [] (
	IF [%2] EQU [] (
		set isparamok=false
	) ELSE (
	set isparamok=true
	)
) 

IF "%isparamok%"=="true" (
	IF [%5] EQU [] (
			IF "%DIRNAME%"=="" set DIRNAME=.\
			"%DIRNAME%\groovy.bat" "%DIRNAME%\jobrunner.groovy" %1 %2 %3 %4
	) ELSE IF [%3] EQU [] (
		IF "%DIRNAME%" == "" set DIRNAME=.\
			"%DIRNAME%\groovy.bat" "%DIRNAME%\jobrunner.groovy" %1 %2
	)
) ELSE (
	ECHO           Incorrect parameters!
	ECHO			Example:
	ECHO			jobrunner -f var/job/testjob.job  :  Run job now and one time only
	ECHO			jobrunner -f var/job/testjob.job -s "<crontab>"  :  Run scheduled job with crontab format
	    PAUSE
)
setlocal