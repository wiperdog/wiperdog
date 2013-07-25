#!/bin/sh
# cv=`cat cv4job4.dat 2> /dev/null`
# if [ "$cv" = "" ]; then
# 	cv=1
# fi
# cv=`expr $cv + 1`
# rv=1
# if [ $cv -gt 4 ];then
# 	rv=0
# fi
# 
# echo $cv > cv4job4.dat
# 
rv=1
echo `date`" $0 done" >> jobs.sh.log
exit $rv
