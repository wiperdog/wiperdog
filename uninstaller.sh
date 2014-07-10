#!/bin/bash

BASEDIR="$( cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PARENT=$BASEDIR/..
echo BASEDIR $BASEDIR
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

#~ If there is wiperdog's service then confirm uninstall or not
uninstall_service=FALSE
if [ "$CHECKSERVICE" = "TRUE" ]
then
	while true; do
		read -p "Do you want to remove Wiperdog service? (y/n)" yn
		case $yn in 
			[Yy] ) uninstall_service=TRUE; break;;
			[Nn] ) uninstall_service=FALSE; break;;
			*);;
		esac
	done
fi

#~ Confirm remove files
delete_files=FALSE
while true; do
	read -p "Do you want to delete all wiperdog's files? (y/n)" ynfile
	case $ynfile in 
		[Yy] ) delete_files=TRUE; break;;
		[Nn] ) delete_files=FALSE; break;;
		*);;
	esac
done

#~ Confirm remove data in mongoDB
delete_data=FALSE
while true; do
	read -p "Do you want to delete all wiperdog's data in mongodb? (y/n)" ynmongo
	case $ynmongo in 
		[Yy] ) delete_data=TRUE; break;;
		[Nn] ) delete_data=FALSE; break;;
		*);;
	esac
done

#~ Final confirm 
echo "========================================="
if [ "$uninstall_service" = "TRUE" ] || [ "$uninstall_service" = "FALSE" ]
then
	echo "Uninstall service: " $uninstall_service
fi
echo "Delete data in mongodb: " $delete_data	
echo "Delete Wiperdog's files: " $delete_files
echo "========================================="
while true; do
	read -p "Continue? (y/n)" ynall
	case $ynall in 
		[Yy] ) continue=TRUE; break;;
		[Nn] ) echo "Bye."; exit;;
		*);;
	esac
done

if [ "$continue" = "TRUE" ]
then
    java -DWIPERDOG_HOME=$BASEDIR -classpath $BASEDIR/lib/java/bundle.a/groovy-all-2.2.1.jar:$BASEDIR/lib/java/bundle.a/ivy-2.4.0-rc1.jar groovy.ui.GroovyMain $BASEDIR/uninstall.groovy $uninstall_service $delete_data $delete_files

	#$BASEDIR/bin/groovy -DWIPERDOG_HOME=$BASEDIR -classpath lib/java/bundle.a/groovy-all-2.2.1.jar:lib/java/bundle.a/ivy-2.4.0-rc1.jar $BASEDIR/uninstall.groovy $uninstall_service $delete_data $delete_files
	#$BASEDIR/bin/groovy -DWIPERDOG_HOME=$BASEDIR $BASEDIR/uninstall.groovy $uninstall_service $delete_data $delete_files

fi

