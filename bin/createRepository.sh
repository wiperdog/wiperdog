#!/bin/bash
#
#   <Usage>
#
#	./createRepository.sh [-l list file] [-o download directory]
#

# 引数にリストファイル[-l] が指定されないときのデフォルト値 
LISTBUNDLE=`cd $(dirname $0);cd ..;pwd`"/etc/ListBundle.csv"
POM="./pom.xml"

## /etc/ListBundle.csv からpom.xml を作成する
createPom(){

	## ListBundle.csv から pom.xml を作成
	echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" > $POM
	echo "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">" >> $POM
	echo "  <parent>" >> $POM
	echo "    <artifactId>wiperdog-parent</artifactId>" >> $POM
	echo "    <groupId>org.wiperdog</groupId>" >> $POM
	echo "    <version>0.1.0</version>" >> $POM
	echo "  </parent>" >> $POM
	echo "  <modelVersion>4.0.0</modelVersion>" >> $POM
	echo "  <groupId>org.wiperdog</groupId>" >> $POM
	echo "  <artifactId>jarcollection</artifactId>" >> $POM
	echo "  <version>0.1.0</version>" >> $POM
	echo " " >> $POM
	echo "  <name>\${bundle.symbolicName}</name>" >> $POM
	echo " " >> $POM
	echo "    <packaging>pom</packaging>" >> $POM
	echo " " >> $POM
	echo "    <description>directory watcher</description>" >> $POM
	echo "    <url>http://www.wiperdog.org/</url>" >> $POM
	echo "    <licenses>" >> $POM
	echo "      <license>" >> $POM
	echo "        <name>The Apache Software License, Version 2.0</name>" >> $POM
	echo "        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>" >> $POM
	echo "        <distribution>repo</distribution>" >> $POM
	echo "      </license>" >> $POM
	echo "    </licenses>" >> $POM
	echo "    <scm>" >> $POM
	echo "      <url>scm:git:https://github.com/wiperdog/org.wiperdog.directorywatcher.git</url>" >> $POM
	echo "      <connection>scm:git:https://github.com/wiperdog/org.wiperdog.directorywatcher.git</connection>" >> $POM
	echo "      <developerConnection>scm:git:https://github.com/wiperdog/org.wiperdog.directorywatcher.git</developerConnection>" >> $POM
	echo "    </scm>" >> $POM
	echo " " >> $POM
	echo "  <dependencies>" >> $POM
	while IFS=, read f1 f2 f3
	do
		## mvn, wrapmvn が対象
	    if [[ ${f1} =~ ^mvn|^wrapmvn ]] ; then
	##		echo "$f1"
	##		echo "$f2"
	##		org.wiperdog:org.wiperdog.directorywatcher:0.1.0
			echo "      <dependency>" >> $POM
			arr=( `echo $f2 | tr -s ':' ' '`)
			echo "          <groupId>"${arr[0]}"</groupId>" >> $POM
			echo "          <artifactId>"${arr[1]}"</artifactId>" >> $POM
			echo "          <version>"${arr[2]}"</version>" >> $POM
			echo "          <type>jar</type>" >> $POM
			echo "          <scope>compile</scope>" >> $POM
			## 依存するライブラリがある場合は、classifier を指定する
			## e.g. wrapmvn,net.sf.json-lib:json-lib:2.3:jar:jdk15,3,
			if [ -n "${arr[4]}" ] ; then
				echo "          <classifier>"${arr[4]}"</classifier>" >> $POM
			fi
			echo "      </dependency>" >> $POM
	    fi
	done < $LISTBUNDLE 
	echo "  </dependencies>" >> $POM
	echo "</project>" >> $POM
	return 0
}


## create Local Repository
execCommand() {

	if [ "$REPOSITORY" != "" ] ;then
		## ダウンロード先を指定する
		mvn dependency:go-offline -Dmaven.repo.local=${REPOSITORY}
	else
		## ダウンロード先を指定しない($M2_HOME/repository)
		mvn dependency:go-offline
	fi

	return 0
}


##----------------------------------------------------------------------------##
## start: main()

	LIST_FIND="FALSE"
	OUT_FIND="FALSE"
	REPOSITORY=""

	while [ "$1" != "" ]
	do
		if [ "$1" = "-l" ] ;then
			LIST_FIND="TRUE"
		elif [ "$1" = "-o" ] ;then
			OUT_FIND="TRUE"
		elif [ "$LIST_FIND" = "TRUE" ] ;then 
			PATH_CSV="$1"
			LIST_FIND="FALSE"
		elif [ "$OUT_FIND" = "TRUE" ] ;then
			REPOSITORY="$1"
			OUT_FIND="FALSE"
		else
			LIST_FIND="FLASE"
			OUT_FIND="FLASE"
		fi
		shift
	done

	if [ "$PATH_CSV" != "" ] ;then
		LISTBUNDLE="$PATH_CSV"
	fi

    	# ListBundle.csv 存在チェック
	if [ ! -e ${LISTBUNDLE} ] ; then
		echo [$LISTBUNDLE] " file not found."
		exit 1
	fi

	# ローカルリポジトリ存在チェック 
	if [ "$REPOSITORY" != "" ] ;then
		if [ ! -d "$REPOSITORY" ] ;then
			echo [$REPOSITORY] " directory not found."
			exit 1
		fi
	fi

	createPom
	echo "execute mvn dependency:go-offline....."
	execCommand

## end  : main()
##----------------------------------------------------------------------------##
