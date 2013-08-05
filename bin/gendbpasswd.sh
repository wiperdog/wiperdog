#!/bin/sh
self="$0"
dir=`dirname "$self"`
# 2013-03-06 Luvina insert start
path=`pwd $0`
if [ "$dir" == "." ]  
then
  export dirname=`pwd $0`
else
  export dirname=$path"/"$dir	
fi
# 2013-03-06 Luvina insert end
if [ "$#" -eq 4 ]; then 
"$dir/groovy" "$dir/gendbpasswd.groovy" "$1" "$2" "$3" "$4"
else
if [ "$#" -eq 6 ]; then 
"$dir/groovy" "$dir/gendbpasswd.groovy" "$1" "$2" "$3" "$4" "$5" "$6"
else
if [ "$#" -eq 8 ]; then 
"$dir/groovy" "$dir/gendbpasswd.groovy" "$1" "$2" "$3" "$4" "$5" "$6" "$7" "$8"
else
	echo Incorrect parameters !!!
	echo Correct format of commmand: gendbpasswd -t DBType -u username [-h hostId] [-s sid]
	echo     DBType may accept following value:  @ORA , @MYSQL ,@PGSQL ,@MSSQL
	echo      Example : gendbpasswd -t @ORA -u username -h hostId -s piex
fi
fi
fi