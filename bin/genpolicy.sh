#!/bin/bash

#Set default directory
self="$0"
dir=`dirname "$self"`
path=`pwd $0`
if [ "$dir" == "." ]; then
	export dirname=`pwd $0`
else
	export dirname=$path"/"$dir
fi
	export currentdir=`pwd`/
PREFIX=`cd "$dir/.." && pwd`

cd "$PREFIX/bin"
$PREFIX/bin/groovy $PREFIX/bin/genpolicy.groovy "$dir"