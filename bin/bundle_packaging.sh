#!/bin/bash
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

if [ "$#" -ne 5 ]; then
	echo "------------ERROR-----------------------"
    echo "  Illegal number of parameters"
    echo "  Usage: ./bundle_packaging.sh [resource directory] [destination] [groupId] [artifactId] [version]"
else
	pom_tpl=""
	command -v mvn >/dev/null 2>&1 || { echo "Maven not installed. Aborting." >&2; exit 1;}	
	read -r -d '' pom_tpl <<Endfile
	<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>@groupid@</groupId>
  <artifactId>@artifactid@</artifactId>
  <version>@version@</version>
  
    <build>
	<resources>
      <resource>
		  <directory>@resourcesDir@</directory>
		 <excludes>
          <exclude>pom.tpl</exclude>
          <exclude>pom.xml</exclude>
        </excludes>
      </resource>
    </resources>

    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <version>2.4</version>
        <configuration>
          <archive>
            <index>true</index>
            <manifest>
              <addClasspath>true</addClasspath>
            </manifest>
            <manifestEntries>
              <Destination>@extractDestination@</Destination>
            </manifestEntries>
          </archive>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
Endfile
	PREFIX=`cd "$dir/.." && pwd`
	$PREFIX/bin/groovy $PREFIX/bin/bundle_packaging.groovy $1 $2 $3 $4 $5 "$pom_tpl"
fi
