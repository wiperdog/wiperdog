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

setlocal enabledelayedexpansion
  set SELF=%~n0
  set JOB_NAME=%~1
  
  for %%i in ("%~dps0..") do set PIEX_HOME=%%~fi
  set MONITOR_JOB_DATA=%PIEX_HOME%\tmp\monitorjobdata
  set LAST_EXECUTION=%MONITOR_JOB_DATA%\LastExecution
  set PERSISTENT_DATA=%MONITOR_JOB_DATA%\PersistentData
  set PREV_OUTPUT=%MONITOR_JOB_DATA%\PrevOUTPUT
  
  if "%JOB_NAME%" == "" (
    echo Usage: %SELF% JOB_NAME >&2
    echo.
    echo Job name list
    echo.
    
    set f1=%TEMP%\%SELF%.1
    set f2=%TEMP%\%SELF%.2
    set f3=%TEMP%\%SELF%.3
    
    type NUL > !f1!
    for /f "tokens=*" %%i in ('dir /b %LAST_EXECUTION%\*.txt 2^> NUL') do ((echo %%~ni) >> !f1!)
    for /f "tokens=*" %%i in ('dir /b %PERSISTENT_DATA%\*.txt 2^> NUL') do ((echo %%~ni) >> !f1!)
    for /f "tokens=*" %%i in ('dir /b %PREV_OUTPUT%\*.txt 2^> NUL') do ((echo %%~ni) >> !f1!)
    sort "!f1!" /o "!f2!" && call :uniq "!f2!" "!f3!"
    for /f "tokens=*" %%i in ('sort !f3!') do (echo ^ %%i)
    
    del /q "%TEMP%\%SELF%.?"
    
    exit /b 1
  )
  
  for %%i in ("%LAST_EXECUTION%" "%PERSISTENT_DATA%" "%PREV_OUTPUT%") do (
    if not exist "%%~i\*" mkdir "%%~i" > NUL 2>&1
    if exist "%%~i\%JOB_NAME%.txt" (
      echo Job file ^(%JOB_NAME%^) in %%~ni removing ...
      del "%%~i\%JOB_NAME%.txt" > NUL 2>&1
    ) else (
      echo Job file ^(%JOB_NAME%^) in %%~ni doesn't exist.
    )
  )
endlocal
goto :EOF

:uniq
setlocal
  set f1=%~1
  set f2=%~2
  
  type NUL > %f2%
  for /f "tokens=*" %%i in (%f1%) do findstr /x /c:"%%i" %f2% > NUL || (echo %%i) >> %f2%
endlocal & exit /b 0
goto :EOF

