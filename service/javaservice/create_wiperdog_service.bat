@echo off
SETLOCAL EnableDelayedExpansion
if "%~1" == "" goto show_instructions
SET WIPERDOG_HOME=%~1
echo Param %WIPERDOG_HOME% "%~1"

REM Check OS 64 bit or 32 bit
if "%PROCESSOR_ARCHITECTURE%"=="AMD64" goto WIN_64bit
SET JAVASERVICE_STUB=%WIPERDOG_HOME%\bin\wiperdog_service_32bit.exe
GOTO SET_OTHER_PARAM

:WIN_64bit
SET JAVASERVICE_STUB=%WIPERDOG_HOME%\bin\wiperdog_service_64bit.exe

:SET_OTHER_PARAM
SET SERVICE_NAME=Wiperdog
SET GROOVY_SCRIPT=%WIPERDOG_HOME%\bin\service.groovy
SET LOGS_FOLDER=%WIPERDOG_HOME%\log

echo.
echo.
echo INSTALLING %SERVICE_NAME%
echo.
echo JavaService executable is %JAVASERVICE_STUB%
echo Service name is %SERVICE_NAME%
echo GROOVY target is %GROOVY_SCRIPT%
echo LOGS folder is %LOGS_FOLDER%
echo.
echo JAVA_HOME is %JAVA_HOME%
echo.
echo.
if not exist "%WIPERDOG_HOME%"\log md "%WIPERDOG_HOME%"\log
REM stoping service
net stop "%SERVICE_NAME%"
"%JAVASERVICE_STUB%" -uninstall "%SERVICE_NAME%"

REM check JDK/JRE
if exist "%JAVA_HOME%"\bin\client GOTO SET_JRE_CLIENT_DLL
if exist "%JAVA_HOME%"\bin\server GOTO SET_JRE_SERVER_DLL

REM set JDK client jvm.dll path 
if not exist "%JAVA_HOME%"\jre\bin\client GOTO SET_JDK_SERVER_DLL
SET JVM_DLL=%JAVA_HOME%\jre\bin\client\jvm.dll
GOTO START_MAIN_APP

REM set JDK server jvm.dll path 
:SET_JDK_SERVER_DLL
if not exist "%JAVA_HOME%"\jre\bin\server GOTO NOTFOUND_JVM
SET JVM_DLL=%JAVA_HOME%\jre\bin\server\jvm.dll
SET JVM_EXT_LIB=%JAVA_HOME%\jre\lib\ext
GOTO START_MAIN_APP

:NOTFOUND_JVM
echo.
echo.
echo Can not found JVM. Exit install service process.
echo.
echo.
goto end

:SET_JRE_CLIENT_DLL
REM set JRE jvm.dll path
SET JVM_DLL=%JAVA_HOME%\bin\client\jvm.dll
SET JVM_EXT_LIB=%JAVA_HOME%\lib\ext
GOTO START_MAIN_APP

:SET_JRE_SERVER_DLL
REM set JRE jvm.dll path
SET JVM_DLL=%JAVA_HOME%\bin\server\jvm.dll
SET JVM_EXT_LIB=%JAVA_HOME%\lib\ext
GOTO START_MAIN_APP

:START_MAIN_APP
echo.
REM for JDK only "%JAVASERVICE_STUB%" -install "%SERVICE_NAME%" "%JAVA_HOME%"\jre\bin\server\jvm.dll ^
REM below for using JRE
"%JAVASERVICE_STUB%" -install "%SERVICE_NAME%" "%JVM_DLL%" ^
-Djava.class.path="%WIPERDOG_HOME%\service\dasboot.jar" ^
-Dcom.lilypepper.groovy.localfolder="%WIPERDOG_HOME%\service" ^
-Dcom.lilypepper.groovy.runner="com.lilypepper.groovy.ServiceRunner" ^
-Djava.system.class.loader="com.lilypepper.groovy.boot.GoshClassLoader" ^
-Dfelix.home="%WIPERDOG_HOME%" ^
-Dfelix.system.properties="file:%WIPERDOG_HOME%\etc\system.properties" ^
-Djava.util.logging.config.file="%WIPERDOG_HOME%\etc\java.util.logging.properties"    ^
-Dlog4j.ignoreTCL=true ^
-Djava.ext.dirs="%WIPERDOG_HOME%\lib\java\ext;%JVM_EXT_LIB%" ^
-Dbin_home="%WIPERDOG_HOME%"\bin\ ^
-Dprogram.name="" ^
-Dgroovy.home="%WIPERDOG_HOME%" ^
-Dgroovy.starter.conf="%WIPERDOG_HOME%\etc\groovy-starter.conf" ^
-Dscript.name="%WIPERDOG_HOME%\bin\startWiperdog.groovy"  ^
-classpath "%WIPERDOG_HOME%"\bin\..\lib\java\bundle\*;"%WIPERDOG_HOME%"\bin\..\lib\java\bundle.a\*;"%WIPERDOG_HOME%"\bin\..\lib\java\bundle.d\*;"%WIPERDOG_HOME%"\bin\..\lib\java\ext\* org.codehaus.groovy.tools.GroovyStarter --main groovy.ui.GroovyMain --conf "%WIPERDOG_HOME%"\bin\..\etc\groovy-starter.conf ^
-verbose:gc ^
-Xmx1024M ^
-Xrs ^
-XX:CompileThreshold=100 ^
-XX:MaxPermSize=256m ^
-start com.lilypepper.groovy.boot.Bootstrap -method main -params "start" "%GROOVY_SCRIPT%" ^
-stop com.lilypepper.groovy.boot.Bootstrap -method main -params "stop" ^
-out "%LOGS_FOLDER%"\stdout.log ^
-err "%LOGS_FOLDER%"\stderr.log ^
-current "%LOGS_FOLDER%"

echo.
echo.

if "" == "" goto end

:show_instructions
echo Error: command must start with parameter WIPERDOG_HOME
echo E.g.: create_wiperdog_service.bat E:\wiperdog
echo NOTICE: you must use JRE instead of using JDK
:end
ENDLOCAL