#!/bin/sh
self="$0"
dir=`dirname "$self"`
# 2013-03-06 Luvina insert start
path=`pwd $0`
if [ "$dir" == "." ]  
then
  export dir=`pwd $0`
else
  export dir=$path"/"$dir	
fi
export PREFIX=`cd "$dir/.." && pwd`

# move to the Wiperdog home/bin
cd "$PREFIX"/bin

if [ "$#" -eq 2 ]; then 
"$dir/groovy" "$dir/jobrunner.groovy" "$1" "$2"
elif [ "$#" -eq 4 ]; then
  "$dir/groovy" "$dir/jobrunner.groovy" "$1" "$2" "$3" "$4" 
else
 echo           Incorrect parameters!
 echo			Example:
 echo			jobrunner -f var/job/testjob.job  :  Run job now and one time only
 echo			jobrunner -f var/job/testjob.job -s "<crontab>"  :  Run scheduled job with crontab format
fi
