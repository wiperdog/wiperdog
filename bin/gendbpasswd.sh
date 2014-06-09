#!/bin/sh
self="$0"
dir=`dirname "$self"`

path=`pwd $0`
if [ "$dir" = "." ]  
then
  export dirname=`pwd $0`
else
  export dirname=$path"/"$dir
fi
export currentdir=`pwd`/

# move to the Wiperdog home/bin
PREFIX=`cd "$dir/.." && pwd`
cd "$PREFIX/bin"

if [ "$#" -eq 2 ]; then 
"./groovy" "./gendbpasswd.groovy" "$1" "$2" "$currentdir"
else
if [ "$#" -eq 4 ]; then 
"./groovy" "./gendbpasswd.groovy" "$1" "$2" "$3" "$4"
else
if [ "$#" -eq 6 ]; then 
"./groovy" "./gendbpasswd.groovy" "$1" "$2" "$3" "$4" "$5" "$6"
else
if [ "$#" -eq 8 ]; then 
"./groovy" "./gendbpasswd.groovy" "$1" "$2" "$3" "$4" "$5" "$6" "$7" "$8"
else
	echo Incorrect parameters !!!
	echo Correct format of commmand: gendbpasswd -t DBType -u username [-h hostId] [-s sid]
	echo								OR gendbpasswd -f "File csv contains user/password information"
	echo     DBType may accept following value:  @ORA , @MYSQL ,@PGSQL ,@MSSQL
	echo      Example : gendbpasswd -t @ORA -u username -h hostId -s piex
fi
fi
fi
fi
