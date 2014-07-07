#!/bin/bash
##############################################################################
# This test case use to test Wiperdog Installer for installing with -d option
# another options will be supplied by expect tool automatically.
# ############################################################################
	if [[ "$1" == "" ]]; then
	   echo "Incorrect parameter !"
	   echo "Usage: /bin/sh test_installer.sh /<path-to-wiperdog-installer-jar-file>"   
	   exit 1
	fi
	echo "*************************************************************************"
	echo "* TEST WIPERDOG INSTALLER "
	echo "**************************************************************************"

	# ========= CASE 2 =========
	echo  ">>>>> CASE 2: Test installer with  -d option only <<<<<"
	wiperdogPath="/home/nghia/wpd"
	installerJar="$1"
	javaCommand="$JAVA_HOME/bin/java"
	jarFileName=`/bin/basename $installerJar` 
	curDir=`pwd` 
	wiperDogDirName=`/bin/basename $installerJar -unix.jar` 
	
	nettyPort=11111
	jobDir="$wiperdogPath/var/job"
	triggerDir="$wiperdogPath/var/trigger"
	jobClassDir="$wiperdogPath/var/class"
	jobInstDir="$wiperdogPath/var/instances"
	mongoDb="somehost"
	mongoPort=9999
	mongoDbName="wiperdogMongoDbName"
	mongoDbUserPasswd="scretePassword"
	installAsService="no"
	confirmInput="y"
	policyEmail="anemail@anemail.com"
	mongoDbUser="anuser"
expect <<- DONE	
	puts "============== ENVIRONMENT ================="
	puts  "Wiperdog Path: $wiperdogPath"
	puts  "Installer jar: $installerJar"
	puts  "Installer jar file name: $jarFileName"
	puts  "Current directory: $curDir"
	puts  "Wiperdog folder name(default): $wiperDogDirName"
	puts  "============================================"

	# Remove old file
	catch { exec rm -rf $wiperdogPath } errorCode
	spawn $javaCommand -jar $installerJar -d $wiperdogPath
	expect "* -ni option"
	send " \r"
	
	#Confirm getting input parameter for pre-configure
	expect "Getting input parameters for pre-configured wiperdog*"
	send " \r"
	#Gathering netty port
	expect "Please input Netty port(default set to 13111):*"
	send "$nettyPort\r"
	#Job folder
	expect "Please input job directory:*"
	send  "$jobDir\r"

	#Trigger folder
	expect "Please input trigger directory:*"
	send  "$triggerDir\r"

	#Job class folder
	expect "Please input job class directory:*"
	send  "$jobClassDir\r"

	#Instance folder
	expect "Please input job instance directory:*"
	send  "$jobInstDir\r"

	#MongoDb
	expect "Please input database server (Mongodb) IP address*"
	send  "$mongoDb\r"

	#MongoDb port
	expect "Please input database server port*"
	send  "$mongoPort\r"

	#MongoDb database name
	expect "Please input database name*"
	send  "$mongoDbName\r"

	#MongoDb database user name
	expect "Please input database server user name*"
	send  "$mongoDbUser\r"

	#MongoDb database user password
	expect "Please input database server password*"
	send  "$mongoDbUserPasswd\r"

	#Set receiving mail for policy
	expect "Please input mail send data policy,*"
	send  "$policyEmail\r"

	#Option for install as OS Service
	expect "Do you want to install wiperdog as system service,*"
	send  "$installAsService\r"

	#Confirm input
	expect "Your input are correct(Y|y|N|n):*"
	send  "$confirmInput\r"
	exec sleep 1	
DONE
#After installation complete, check the following configuration
echo 
echo 
echo "==================== Check result ========================="
configFileName="$wiperdogPath/etc/monitorjobfw.cfg"
sysPropFileName="$wiperdogPath/etc/system.properties"
echo $configFileName
echo $sysPropFileName
ret="true"

#Check log file
if [ -s $curDir/WiperdogInstaller.log ]; then
	echo "Installer log PASSED"
else
	echo "Checking installer log FAILURE"
	ret="false"
fi
#Check install folder
if [ -s $configFileName ] && [ -s $sysPropFileName ] ;then
	echo "Check installer content PASSED"
else
	echo "Check installer content FAILURE"
	ret="false"
fi
# check nettyPort
nettyPortVal=`/bin/cat $sysPropFileName | /bin/grep netty.port=| /usr/bin/cut -d'=' -f 2` 	
 if [ "$nettyPortVal" == "$nettyPort" ]; then
   echo "Netty port setting PASSED"
 else 
	echo "Checking netty port FAILURE"
	ret="false"
 fi	 
jobDirVal=`/bin/cat $configFileName | /bin/grep monitorjobfw.directory.job= |/usr/bin/cut -d'=' -f 2` 
if [ "$jobDirVal" == "$jobDir" ]; then
	echo "Job directory setting  PASSED"
 else
   echo "Case 1 failure, job directory FAILURE"
   echo "$jobDirVal <> monitorjobfw.directory.job=$jobDir"
   ret="false"
 fi

triggerDirVal=`/bin/cat $configFileName | /bin/grep monitorjobfw.directory.trigger= | /usr/bin/cut -d'=' -f 2` 
 if [ "$triggerDirVal" == "$triggerDir" ]; then
   echo "Trigger directory setting  PASSED"
 else
   echo "Case 1 failure, trigger directory FAILURE"
   echo "$triggerDirVal <> monitorjobfw.directory.trigger=$triggerDir" 
   ret="false"
 fi
 
 jobClassDirVal=`/bin/cat $configFileName | /bin/grep torjobfw.directory.jobcls= | /usr/bin/cut -d'=' -f 2` 
 if [ "$jobClassDirVal" == "$jobClassDir" ]; then
   echo "Jobclass directory setting  PASSED"
 else
   echo "Case 1 failure, job class directory FAILURE"
   echo "$jobClassDirVal <> monitorjobfw.directory.jobcls=$jobClassDir"
   ret="false"
 fi
jobInstDirVal=`/bin/cat $configFileName | /bin/grep monitorjobfw.directory.instances= | /usr/bin/cut -d'=' -f 2`
 if [ "$jobInstDirVal" == "$jobInstDir" ]; then
   echo "Job instance directory setting  PASSED"
 else
   echo "Case 1 failure, job instance directory FAILURE"
   echo "$jobInstDirVal <> monitorjobfw.directory.instances=$jobInstDir"
   ret="false"
 fi
mongoDbVal=`/bin/cat $configFileName | /bin/grep monitorjobfw.mongodb.host= | /usr/bin/cut -d'=' -f 2`
 if [ "$mongoDbVal" == "$mongoDb" ];then
   echo "Mongo DB  setting  PASSED"
 else
   echo "Case 1 failure, Mongo DB setting FAILURE"
   echo "$mongoDbVal <> monitorjobfw.mongodb.host=$mongoDb"
   ret="false"
 fi

	
mongoPortVal=`/bin/cat $configFileName | /bin/grep monitorjobfw.mongodb.port= | /usr/bin/cut -d'=' -f 2`
 if [ "$mongoPortVal" == "$mongoPort" ];then
   echo "Mongo DB Port  setting  PASSED"
 else
   echo "Case 1 failure, Mongo DB Port setting FAILURE"
   echo "$mongoPortVal <> monitorjobfw.mongodb.port=$mongoPort"
   ret="false"
 fi
mongoDbNameVal=`/bin/cat $configFileName | /bin/grep monitorjobfw.mongodb.dbName= | /usr/bin/cut -d'=' -f 2`
 if [ "$mongoDbNameVal" == "$mongoDbName" ]; then
   echo "Mongo DB Name  setting  PASSED"
 else
   echo "Case 1 failure, Mongo DB Name setting FAILURE"
   echo "$mongoDbNameVal <> monitorjobfw.mongodb.dbName=$mongoDbName"
   ret="false"
 fi
mongoDbUserPasswdVal=`/bin/cat $configFileName | /bin/grep monitorjobfw.mongodb.pass= | /usr/bin/cut -d'=' -f 2`
 if [ "$mongoDbUserPasswdVal" == "$mongoDbUserPasswd" ]; then
   echo "Mongo DB User Password  setting  PASSED"
 else
   echo "Case 1 failure, Mongo DB User Password setting FAILURE"
   echo "$mongoDbUserPasswdVal <> monitorjobfw.mongodb.pass=$mongoDbUserPasswd"
   ret="false"
 fi
policyEmailVal=`/bin/cat $configFileName | /bin/grep monitorjobfw.mail.toMail= |/usr/bin/cut -d'=' -f 2`
 if [ "$policyEmailVal" == "$policyEmail" ];then
   echo "Policy Email  setting  PASSED"
 else
   echo "Case 1 failure, policy email setting FAILURE"
   echo "$policyEmailVal <> monitorjobfw.mail.toMail=$policyEmail"
   ret="false"
 fi
mongoDbUserVal=`/bin/cat $configFileName | /bin/grep monitorjobfw.mongodb.user= | /usr/bin/cut -d'=' -f 2` 
 if [ "$mongoDbUserVal" == "$mongoDbUser" ]; then
   echo "Mongo DB User Password  setting  PASSED"
 else
   echo "Case 1 failure, Mongo DB User Password setting FAILURE"
   echo "$mongoDbUserVal <> monitorjobfw.mongodb.user=$mongoDbUser"
   ret="false"
 fi
echo "FINAL CHECKING RESULT PASS: $ret"
echo "================== End check result ======================="
# Check more 

