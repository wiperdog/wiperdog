#!/bin/bash
TITLE="Create Template For Job !!!"

#Set default directory
self="$0"
dir=`dirname "$self"`
path=`pwd $0`
if [ "$dir" == "." ]
then
	export dirname=`pwd $0`
else
	export dirname=$path"/"$dir
fi
	export currentdir=`pwd`/
PREFIX=`cd "$dir/.." && pwd`
cd "$PREFIX/bin"
##
 # helper: help when enter an incorrect format
##
function helper() {
	echo "Incorrect format !!!"
	echo "Correct format of command: "
	echo "genjob --n <jobName> [--f <strFetchAction>] [--q <strQuery>] [--c <strCommand>] [--d <strDbExec>] [--fp <pathToFile>]"
	exit
}

if [ $1 == "--n" ]; then
	"./groovy" "./genjob.groovy" "$@"
else
	helper
fi
