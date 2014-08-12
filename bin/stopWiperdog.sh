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
pidFile="wiperdog.pid"
if [ -f wiperdog.pid ];then
	pid=`cat $pidFile`
	kill_output=$(kill -9 $pid 2>&1)
	if [ "$kill_output" = "" ]; then
		echo "Wiperdog stopped !"
		rm -rf $pidFile
	else
		echo "Failed to stop wiperdog !\n $kill_output"
	fi
else 
	echo "Wiperdog not running"
fi
