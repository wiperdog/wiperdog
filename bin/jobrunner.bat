
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

set isparamok=true

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
