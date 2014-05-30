#!/bin/bash
TITLE="Edit default.params file !!!"

#SET DEFAULT DIRECTORY
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

# MOVE TO THE WIPERDOG HOME/BIN
PREFIX=`cd "$dir/.." && pwd`
cd "$PREFIX/bin"

##
 # helper: help when enter an incorrect format
##
function helper() {
	echo ">>>>> INCORRECT FORMAT !!! <<<<<"
	echo "Correct format of command: "
	echo "    - gendefaultparam dbinfo"
	echo "    - gendefaultparam dest"
	echo "    - gendefaultparam datadirectory"
	echo "    - gendefaultparam programdirectory"
	echo "    - gendefaultparam dbmsversion"
	echo "    - gendefaultparam dblogdir"
	exit
}

if [[ $# -ne 2 ]]; then
	if [ "$1" == "dbinfo" ] || [ "$1" == "dest" ] || [ "$1" == "datadirectory" ] || [ "$1" == "programdirectory" ] || [ "$1" == "dbmsversion" ] || [ "$1" == "dblogdir" ] ; then
		"$PREFIX/bin/groovy" "$PREFIX/bin/gendefaultparam.groovy" "$@"
	else
		helper
	fi
else
	helper
fi