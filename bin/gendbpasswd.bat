
@echo off
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
::   Copyright 2013 Insight technology,inc. All rights reserved.
::
::   Licensed under the Apache License, Version 2.0 (the "License");
::   you may not use this file except in compliance with the License.
::   You may obtain a copy of the License at
::
::       http://www.apache.org/licenses/LICENSE-2.0
::
::   Unless required by applicable law or agreed to in writing, software
::   distributed under the License is distributed on an "AS IS" BASIS,
::   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
::   See the License for the specific language governing permissions and
::   limitations under the License.
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

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
