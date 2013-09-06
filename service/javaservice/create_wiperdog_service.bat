SET JAVASERVICE_STUB="E:\wiperdog\bin\wiperdog_service.exe"
SET SERVICE_NAME="Wiperdog"
SET GROOVY_SCRIPT="E:\wiperdog\bin\service.groovy"
SET LOGS_FOLDER="E:\wiperdog\log"

goshservice.bat %JAVASERVICE_STUB% %SERVICE_NAME% %GROOVY_SCRIPT% %LOGS_FOLDER%