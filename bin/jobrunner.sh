#!/bin/bash
self="$0"
dir=`dirname "$self"`
path=`pwd $0`
if [ "$dir" == "." ]  
then
  export dir=`pwd $0`
else
  export dir=$path"/"$dir	
fi
export PREFIX=`cd "$dir/.." && pwd`
cd "$PREFIX"/bin

wiperdog_status=$(lsof -wni tcp:8089)
if [ "$wiperdog_status" == "" ];then
	echo "Wiperdog not running, please start wiperdog before run jobrunner!"
	exit
fi

if [[ "$#" -eq 2  && "$1" == "-f" ]]; then
	jobFile=$2
	if [[ ! "$jobFile" = /* ]];then
		if [[ ! "$jobFile" = */* ]];then
			jobFile=$PREFIX/var/job/$jobFile
		else
			jobFile=$PREFIX/$jobFile
		fi
	fi
		
	if [ -f $jobFile ] ;then
		data="{\"job\":\"$jobFile\"}"
		echo "Running..."
		content=$(curl -s -X POST localhost:8089/runjob -H "Content-type: application/json" -d $data )
		echo "Finished !"
		if [ "$content" != "" ] ;then
			echo "------------------------"
			echo "Job result: "
			echo -e $content | sed 's/\\//g'
		else
			echo -e $content | sed 's/\\//g'
			echo "Error occurred ! Please check wiperdog log or console output !"
		fi
	else
		if [ -d $jobFile ];then
			echo "Input is not a file : $jobFile" 
		else
			echo "Job file does not exists ! : $jobFile "
		fi
	fi
else
	 echo           Incorrect parameters!
	 echo			Example:
	 echo			jobrunner -f var/job/testjob.job
fi

