@echo off

if "%~1" == "" goto show_instructions
if "%~2" == "" goto show_instructions
if "%~3" == "" goto show_instructions
if "%~4" == "" goto show_instructions

echo.
echo.
echo INSTALLING %2
echo.
echo JavaService executable is "%~1"
echo Service name is "%~2"
echo GROOVY target is "%~3"
echo LOGS folder is "%~4"
echo.
echo JAVA_HOME is %JAVA_HOME%
echo.
echo.

rem copy "%~dp0javaservice.exe" "%~1"
"%~1" -uninstall "%~2"
rem --classpath .  E:\wiperdog\bin\startWiperdog.groovy ^
echo.
"%~1" -install "%~2" "%JAVA_HOME%"\jre\bin\server\jvm.dll ^
-Djava.class.path="E:\wiperdog\service\dasboot.jar" ^
-Dcom.lilypepper.groovy.localfolder="E:\wiperdog\service" ^
-Dcom.lilypepper.groovy.runner="com.lilypepper.groovy.ServiceRunner" ^
-Djava.system.class.loader="com.lilypepper.groovy.boot.GoshClassLoader" ^
-Dfelix.home=E:\wiperdog ^
-Dfelix.system.properties=file:E:\wiperdog\etc\system.properties        ^
-Djava.util.logging.config.file=E:\wiperdog\etc\java.util.logging.properties    ^
-Dlog4j.ignoreTCL=true ^
-Djava.ext.dirs=E:\wiperdog\lib\java\ext;E:\wiperdog\opt\jre\lib\ext     ^
-Dbin_home=E:\wiperdog\bin\ ^
-Dprogram.name="" ^
-Dgroovy.home="E:\wiperdog" ^
-Dgroovy.starter.conf="E:\wiperdog\etc\groovy-starter.conf" ^
-Dscript.name="E:\wiperdog\bin\startWiperdog.groovy"  ^
-classpath E:\wiperdog\lib\java\bundle.d\com.insight_tec.pi.scriptsupport.groovyrunner-3.1.0.jar;E:\wiperdog\bin\..\lib\java\bundle\*;E:\wiperdog\bin\..\lib\java\bundle.a\*;E:\wiperdog\bin\..\lib\java\bundle.d\*;E:\wiperdog\bin\..\lib\java\ext\* org.codehaus.groovy.tools.GroovyStarter --main groovy.ui.GroovyMain --conf E:\wiperdog\bin\..\etc\groovy-starter.conf ^
-verbose:gc ^
-Xmx1024M ^
-Xrs ^
-XX:CompileThreshold=100 ^
-XX:MaxPermSize=256m ^
-start com.lilypepper.groovy.boot.Bootstrap -method main -params "start" "%~3" ^
-stop com.lilypepper.groovy.boot.Bootstrap -method main -params "stop" ^
-out "%~4\stdout.log" ^
-err "%~4\stderr.log" ^
-current "%~4"

echo.
echo.

if "" == "" goto end

:show_instructions

echo.
echo.
echo GOSHSERVICE - Runs your GROOVY script as a service (JavaService.exe) using GOSH.
echo.
echo USAGE:
echo goshservice TARGETEXECUTABLE SERVICENAME GROOVYSCRIPT LOGSFOLDER
echo.
echo TARGETEXECUTABLE - This batch file will copy JavaService.exe to the file and path
echo                    you specify here.  Your service will run as this executable, 
echo                    so you will be able to see it in the Task Manager.
echo SERVICENAME      - Name of the service.  
echo GROOVYSCRIPT     - The full path to the groovy script you want to run.  Note that
echo                    you won't be able to change the name later without reinstalling
echo                    this service, or editing the registry.
echo LOGSFOLDER       - Path to the folder where you want logs to go.  Must exist!
echo.
echo Example:
echo     goshservice 
echo         "D:\tma-custom\bin\Canary.exe" 
echo         "Documentum Canary Service"
echo         "D:\tma-custom\project\tools\services\Canary.groovy"
echo         "D:\tma-custom\logs\canary"
echo.
echo You will probably want to create a simple batch file for each service you create. 
echo This setup script is pretty brittle, but once you get your service set up, it should
echo be very reliable!
echo.
echo The definition for the service can be found in the registry under:
echo     HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\<SERVICENAME>
echo If you are having problems getting the service to run, you may want to view the settings
echo in the registry to see if that sheds any light on the problem.
echo.
echo WARNING! You must shut down the Services console before making changes to WinNT services.
echo          Failure to do so may result in an "Overlapped IO" error, which usually requires
echo          a reboot to clear.
echo.
echo.

:end