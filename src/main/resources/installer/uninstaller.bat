@ECHO OFF
SET BASEDIR=%~dp0
SET PARENT=%BASEDIR%\..
:: Check Wiperdog service installed
for /f "tokens=4*" %%a in ('sc query Wiperdog ^| findstr STATE') do set WIPER_SERVICE_STATUS=%%a
IF "%WIPER_SERVICE_STATUS%"=="" (
  echo "Wiperdog service was not installed, abort removing Wiperdog service"
  SET CHECKSERVICE="FALSE"
) else (
  echo Wiperdog service status:  %WIPER_SERVICE_STATUS%
  SET CHECKSERVICE="TRUE"
)

%PARENT%\bin\groovy.bat -DWIPERDOG_HOME=%PARENT% --classpath %PARENT%\lib\java\bundle.a %PARENT%\installer\uninstall.groovy %CHECKSERVICE%