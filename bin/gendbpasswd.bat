@echo off
setlocal
:: current dir
set CURRENT_DIR=%cd%/

:: determine the home

set WIPERDOGHOME=%~dp0..
for %%i in ("%WIPERDOGHOME%") do set WIPERDOGHOME=%%~fsi

:: move to bin

cd "%WIPERDOGHOME%\bin"

SET /A ARGS_COUNT=0    
FOR %%A in (%*) DO SET /A ARGS_COUNT+=1
if %ARGS_COUNT% == 2 (
	"%WIPERDOGHOME%\bin\groovy.bat" "%WIPERDOGHOME%\bin\gendbpasswd.groovy" %1 %2 "%CURRENT_DIR%"
) ELSE (
	if %ARGS_COUNT% == 4  (
	"%WIPERDOGHOME%\bin\groovy.bat" "%WIPERDOGHOME%\bin\gendbpasswd.groovy" %1 %2 %3 %4
	) ELSE (
		if %ARGS_COUNT% == 6 (
			"%WIPERDOGHOME%\bin\groovy.bat" "%WIPERDOGHOME%\bin\gendbpasswd.groovy" %1 %2 %3 %4 %5 %6
		) ELSE (
			if %ARGS_COUNT% == 8 (
				"%WIPERDOGHOME%\bin\groovy.bat" "%WIPERDOGHOME%\bin\gendbpasswd.groovy" %1 %2 %3 %4 %5 %6 %7 %8
			) ELSE (
				 ECHO Incorrect parameters !!!
				 ECHO Correct format of commmand: gendbpasswd -t DBType -u username [-h hostId] [-s sid]  
				 ECHO								OR gendbpasswd -f "File csv contains user/password information"
				 ECHO     DBType may accept following value:  @ORA , @MYSQL ,@PGSQL ,@MSSQL
				 ECHO      Example : gendbpasswd -t @ORA -u username -h hostId -s piex
				 ECHO                gendbpasswd -f "C:\path\dbpassword.csv"
				 PAUSE
			)
		)
	)
)
endlocal