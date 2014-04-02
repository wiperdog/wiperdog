#!/bin/sh
# Wiperdog Service Startup Script for Unix

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

# temporary memory size setting, change this if this is too small(or too big).
export JAVA_OPTS=-Xmx256m

"./groovy" "./startWiperdog.groovy" "$@"
