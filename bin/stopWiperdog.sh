#!/bin/sh
# Wiperdog Service Stop Script for Unix

# determine the prefix

self="$0"
while [ -h "$self" ]; do
	res=`ls -ld "$self"`
	ref=`expr "$res" : '.*-> \(.*\)$'`
	if expr "$ref" : '/.*' > /dev/null; then
		self="$ref"
	else
		self="`dirname \"$self\"`/$ref"
	fi
done
dir=`dirname "$self"`
PREFIX=`cd "$dir/.." && pwd`

# move to the Wiperdog home/bin

cd "$PREFIX/bin"

count=0
for f in *.pid
do 
	if [ -f $f ];then
		pid=`cat $f`
		kill_output=$(kill -9 $pid 2>&1)
		if [ "$kill_output" = "" ]; then
			echo "Wiperdog process with pid $pid stopped !"
			rm -rf $f
			count=$count+1
		else
			echo "Failed to stop wiperdog process with pid $pid !\n $kill_output"
		fi
	fi
done
if [ $count = 0 ];then
	echo "Wiperdog not running "
fi
