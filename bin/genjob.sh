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
	echo "genjob -n <jobName> [-f <strFetchAction>] [-q <strQuery>] [-c <strCommand>] [-d <strDbExec>] [-fp <pathToFile>]"
	exit
}

haspath="n"

for var in "$@"
do
    if [ "$var" == "-fp" ]
    then
        haspath="y"
    fi
done

if [ $1 == "-n" ]; then
    if [ $haspath == "y" ]
    then
	"./groovy" "./genjob.groovy" "$@"
    else
        "./groovy" "./genjob.groovy" "$@" -fp "$path"
    fi
else
	helper
fi
