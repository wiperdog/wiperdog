#!/bin/sh
# Create a fork of Wiperdog

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

if [ "$#" -eq 2 ]; then 
"./groovy" "./createFork.groovy" "$1" "$2"
else
	echo Incorrect parameters !!!
	echo Correct format of commmand: createFork -f portForFork
        echo Example: createFork.sh -f 23111
fi