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
$PREFIX/bin/groovy $PREFIX/bin/genpolicy.groovy