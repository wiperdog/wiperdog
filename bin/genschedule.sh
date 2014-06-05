#!/bin/sh
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

if [ "$#" -eq 6 ] ; then
	$PREFIX/bin/groovy $PREFIX/bin/genschedule.groovy $1 $2 $3 $4 $5 $6
else
	if [ "$#" -eq 4 ] ;then
		$PREFIX/bin/groovy $PREFIX/bin/genschedule.groovy $1 $2 $3 $4
	else
		echo $#
		echo "Incorrect parameter: "
		echo "Usage : "
		echo "genschedule [-f] [file_name] -j [job_name] -s [schedule]"
	fi
fi
