@ECHO OFF
@REM INIT SCREEN
TITLE Config mongodb's information !!!
COLOR 0f
CLS

@REM ----- SET DEFAULT DIRECTORY -----
SET DIRNAME=%~dp0
IF "%DIRNAME%" == "" SET DIRNAME=.\

@REM RESET PARAMETERS
call :RESETCONTROL

@REM DISPLAY OPTIONS
:CHOOSEOPTIONS
ECHO --------------- CONFIG CONNECT INFORMATION OF MONGODB ---------------
ECHO.
ECHO 1. CONFIG MONGODB FOR COMMON CONNECT (For Servlets, Policy evaluate, Send Policy mail, ...)
ECHO.
ECHO 2. CONFIG MONGODB FOR MANUAL CONNECT (For send monitoring data to MongoDB directly)
ECHO.
ECHO 3. EXIT
ECHO.
SET /P option=YOUR CHOICE: 
ECHO.

@REM SET DATA FOR STATUS PARAMETER
IF %option%==1 (
	SET status=commonConfig
	GOTO :GETINPUT
) ELSE IF %option%==2 (
	SET status=manualConfig
	GOTO :GETINPUT
) ELSE IF %option%==3 (
	exit
)
CLS
GOTO :CHOOSEOPTIONS

@REM GET DATA INPUT FROM CONSOLE
:GETINPUT
SET /P host=Host name (Default 127.0.0.1): 
ECHO.
SET /P port=Port (Default 27017): 
ECHO.
SET /P dbName=Database name (Default wiperdog): 
ECHO.
SET /P user=Username (Default empty): 
ECHO.
SET /P pass=Password (Default empty): 
ECHO.

ECHO ========== INFORMATION ENTERED ==========
ECHO.

@REM SET DEFAULT VALUE WHEN NO CONFIG AND VALIDATE
IF %host%=="" (
	SET host=127.0.0.1
)
IF %port%=="" (
	SET port=27017
) ELSE (
	@REM VALIDATE, PORT MUST BE NUMBER
	ECHO %port%|findstr /r /c:"^[0-9][0-9]*$" >nul
	IF errorlevel 1 (
		ECHO Port is not a valid number. Please reconfig !!!
		ECHO.
		call :RESETCONTROL
		GOTO :GETINPUT
	)
)
IF %dbName%=="" (
	SET dbName=wiperdog
)

@REM CONFIRM INPUT DATA
:CONFIRM
ECHO Host: %host%, Port: %port%, Database name: %dbName%, Username: %user%, Password: %pass%
ECHO.
SET /P confirm=INFORMATION IS CORRECT ? [Y/y/N/n]?: 
ECHO.

@REM IF INCORRECT => BACK TO GET INPUT SCREEN, ELSE SEND DATA INPUT TO GROOVY FILE (gendbmongoinfo.groovy)
IF /I "%confirm%"=="n" 	set confirm=N
IF /I "%confirm%"=="y" 	set confirm=Y
IF /I "%confirm%"=="N" (
	call :RESETCONTROL
	GOTO :CHOOSEOPTIONS
) ELSE (
    IF /I "%confirm%"=="Y" (
	      GOTO :SEND2GROOVY
	) ELSE (
	      GOTO :CONFIRM
	)
)

@REM SEND DATA TO GROOVY FILE
:SEND2GROOVY
"%DIRNAME%\groovy.bat" "%DIRNAME%\gendbmongoinfo.groovy" %status% %host% %port% %dbName% %user% %pass%
exit

@REM RESET PARAMETERS (option, host, port, dbName, user, pass, status) TO ""
:RESETCONTROL
SET option=""
SET host=""
SET port=""
SET dbName=""
SET user=""
SET pass=""
GOTO :EOF