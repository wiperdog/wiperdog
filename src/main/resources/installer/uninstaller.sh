#!/bin/bash
BASEDIR="$( cd "$(dirname "${BASH_SOURCE[0]}")" && cd .. && pwd)"
PARENT=$BASEDIR/..
#~ Check if wiperdog service has been installed
CHECKSERVICE="unrecognized"
CHECKSERVICE=$(service wiperdog status)
echo "Checking:" $CHECKSERVICE
if [ "$CHECKSERVICE" = "" ] || [ "$CHECKSERVICE" = "*unrecognized*" ]
then
	echo "No wiperdog service!"
	CHECKSERVICE=FALSE
else
	echo "There is wiperdog service installed!"
	CHECKSERVICE=TRUE
fi

java -DWIPERDOG_HOME=$BASEDIR -classpath ".:$BASEDIR/lib/java/bundle.a/*" groovy.ui.GroovyMain $BASEDIR/installer/uninstall.groovy $CHECKSERVICE

#$BASEDIR/bin/groovy -DWIPERDOG_HOME=$BASEDIR -classpath lib/java/bundle.a/groovy-all-2.2.1.jar:lib/java/bundle.a/ivy-2.4.0-rc1.jar $BASEDIR/uninstall.groovy $uninstall_service $delete_data $delete_files
#$BASEDIR/bin/groovy -DWIPERDOG_HOME=$BASEDIR $BASEDIR/uninstall.groovy $uninstall_service $delete_data $delete_files
