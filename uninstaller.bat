@ECHO OFF
SET BASEDIR=%~dp0
SET PARENT=$BASEDIR\..
:: Check Wiperdog service installed
for /f "tokens=4*" %%a in ('sc query Wiperdog ^| findstr STATE') do set WIPER_SERVICE_STATUS=%%a
IF "%WIPER_SERVICE_STATUS%"=="" (
  echo "Wiperdog service was not installed, abort removing Wiperdog service"
  SET CHECKSERVICE="FALSE"
) else (
  echo Wiperdog service status:  %WIPER_SERVICE_STATUS%
  SET CHECKSERVICE="TRUE"
)

if "%CHECKSERVICE%"=="TRUE" (
	SET uninstall_service=FALSE
	:WHILE_SERVICE	
	SET /P confirm_uninstall_service=Do you want to remove Wiperdog service? (y/n)
	IF "%confirm_uninstall_service%"=="Y" (
	  SET uninstall_service=TRUE
	  GOTO MAIN_PROG
	)
	IF "%confirm_uninstall_service%"=="y" (
	  SET uninstall_service=TRUE
	  GOTO MAIN_PROG
	)
	IF "%confirm_uninstall_service%"=="n" (
	  SET uninstall_service=FALSE
	  GOTO MAIN_PROG
	)
	IF "%confirm_uninstall_service%"=="N" (
	  SET uninstall_service=FALSE
	  GOTO MAIN_PROG
	)
	GOTO WHILE_SERVICE
)

:MAIN_PROG
::Confirm remove files

SET delete_files=FALSE
:WHILE_FILE
SET /P confirm_delete_files=Do you want to delete all wiperdog's files? (y/n)
IF "%confirm_delete_files%"=="Y" (
  SET delete_files=TRUE
  GOTO MAIN_PROG2
)
IF "%confirm_delete_files%"=="y" (
  SET delete_files=TRUE
  GOTO MAIN_PROG2
)
IF "%confirm_delete_files%"=="n" (
  SET delete_files=FALSE
  GOTO MAIN_PROG2
)
IF "%confirm_delete_files%"=="N" (
  SET delete_files=FALSE
  GOTO MAIN_PROG2
)
GOTO WHILE_FILE
:MAIN_PROG2	
SET delete_data=FALSE
:WHILE_MONGO
SET /P confirm_delete_data=Do you want to delete all wiperdog's data in mongodb? (y/n)
IF "%confirm_delete_data%"=="Y" (
  SET delete_data=TRUE
  GOTO MAIN_PROG3
)
IF "%confirm_delete_data%"=="y" (
  SET delete_data=TRUE
  GOTO MAIN_PROG3
)
IF "%confirm_delete_data%"=="n" (
  SET delete_data=FALSE
  GOTO MAIN_PROG3
)
IF "%confirm_delete_data%"=="N" (
  SET delete_data=FALSE
  GOTO MAIN_PROG3
)
GOTO WHILE_MONGO
:MAIN_PROG3

:: Final confirm 
echo ======================================================================================
echo You decide to uninstall the followings:
echo Uninstall service: %uninstall_service%
echo Delete data in mongodb: %delete_data%
echo Delete Wiperdog's files: %delete_files%
echo =======================================================================================
echo "Press any key to continue or CTRL+C to exit...
PAUSE

%BASEDIR%\bin\groovy.bat -DWIPERDOG_HOME=%BASEDIR% %BASEDIR%/lib/java/bundle.a/ivy-2.4.0-rc1.jar %BASEDIR%\uninstall.groovy %uninstall_service% %delete_data% %delete_files%

