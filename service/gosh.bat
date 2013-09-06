@echo off

rem ENVIRONMENT VARIABLES
rem   JAVA_HOME         - The Java HOME folder.

:check_JAVA_HOME
@rem Make sure we have a valid JAVA_HOME
if not "%JAVA_HOME%" == "" goto have_JAVA_HOME
echo.
echo ERROR: Environment variable JAVA_HOME has not been set.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
echo.
goto end 
:have_JAVA_HOME

REM This will copy the JAVA.EXE file to 'GOSH!!!.EXE' in the JAVA\bin folder.
REM Kinda groovy!

:check_GOSH_EXE
if exist "%JAVA_HOME%\bin\Gosh!!!.exe" goto have_GOSH_EXE
copy "%JAVA_HOME%\bin\java.exe" "%JAVA_HOME%\bin\Gosh!!!.exe" 
:have_GOSH_EXE

REM The last part of this command redirects STDERR to STDOUT, then pipes it to a VBS to be reprinted.
REM This is just about the best I can do in DOS, without resorting to a C++ or .NET wrapper.
REM Java just refuses to redirect STDOUT and STDERR otherwise.

REM to be added maybe 6u4??? -XX:+AlwaysRestoreFPU

"%JAVA_HOME%\bin\Gosh!!!.exe" -cp "%~dp0\dasboot.jar" -client -XX:CompileThreshold=250 -Xmx768m -Xmn4m -Xms8m -XX:MaxPermSize=128M -Dcom.lilypepper.groovy.localfolder="%~dp0." -Xcheck:jni -Dcom.lilypepper.groovy.runner="com.lilypepper.groovy.CommandRunner" -Djava.system.class.loader="com.lilypepper.groovy.boot.GoshClassLoader" com.lilypepper.groovy.boot.Bootstrap %* 2>&1 | cscript //NOLOGO %~dp0outpipe.vbs

:end