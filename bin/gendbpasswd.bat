
@echo off
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\

SET /A ARGS_COUNT=0    
FOR %%A in (%*) DO SET /A ARGS_COUNT+=1    
if %ARGS_COUNT% == 4  (
"%DIRNAME%\groovy.bat" "%DIRNAME%\gendbpasswd.groovy" %1 %2 %3 %4
) ELSE (
	if %ARGS_COUNT% == 6 (
		"%DIRNAME%\groovy.bat" "%DIRNAME%\gendbpasswd.groovy" %1 %2 %3 %4 %5 %6
	) ELSE (
		if %ARGS_COUNT% == 8 (
			"%DIRNAME%\groovy.bat" "%DIRNAME%\gendbpasswd.groovy" %1 %2 %3 %4 %5 %6 %7 %8
		) ELSE (
			 ECHO Incorrect parameters !!!
			 ECHO Correct format of commmand: gendbpasswd -t DBType -u username [-h hostId] [-s sid]
			 ECHO     DBType may accept following value:  @ORA , @MYSQL ,@PGSQL ,@MSSQL
			 ECHO      Example : gendbpasswd -t @ORA -u username -h hostId -s piex
			 PAUSE
		)
	)
)
