#!/bin/bash
TITLE="Config mongodb's information !!!"

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

# MAIN FUNCTION
function main() {
    chooseOption
    getDataInput
    setDefaultValue
    confirm=""
    while [[ $confirm != "N" ]] && [[ $confirm != "Y" ]]
    do
        # CONFIRM INPUT DATA
        echo ========== INFORMATION ENTERED ==========
        echo "Host: $host, Port: $port, Database name: $dbName, Username: $user, Password: $pass"
        echo -n "INFORMATION IS CORRECT ? [Y | y | N | n]?: "
        read confirm
        if [ "$confirm" == "n" ]
        then
            confirm="N"
        fi
        if [ "$confirm" == "y" ]
        then
            confirm="Y"
        fi
    done
    
    if [ "$confirm" == "N" ]
    then
        main
    elif [ "$confirm" == "Y" ]
    then
        sendToGroovy
    fi
}

# DISPLAY OPTIONS
function chooseOption() {
    clear
    echo ========== CONFIG CONNECT INFORMATION OF MONGODB ==========
    echo '1. CONFIG MONGODB FOR COMMON CONNECT (For Servlets, Policy evaluate, Send Policy mail, ...)'
    echo '2. CONFIG MONGODB FOR MANUAL CONNECT (For send monitoring data to MongoDB directly)'
    echo '3. EXIT'

    echo -n "YOUR CHOICE: "
    read option
    setDataStatus option
}

# SET DATA FOR STATUS PARAMETER
function setDataStatus() {
    if [ $option == 1 ]
    then
        export status="commonConfig"
    elif [ $option == 2 ]
    then
        export status="manualConfig"
    elif [ $option == 3 ]
    then
        exit
    else
        export option=""
        chooseOption
    fi
}

# GET DATA INPUT FROM CONSOLE
function getDataInput() {
    # host
    echo -n "Host name (Default 127.0.0.1): "
    read host
    # port
    echo -n "Port (Default 27017): "
    read port
    checkPort $port
    while [ $isNumber -eq 0 ]
    do	
        echo "Port is not a valid number. Please reconfig !!!"
        echo -n "Port (Default 27017): "
        read port
        checkPort $port
    done
    # db name
    echo -n "Database name (Default wiperdog): "
    read dbName
    # user
    echo -n "Username (Default empty): "
    read user
    # pass
    echo -n "Password (Default empty): "
    read pass
}

# SET DEFAULT VALUE WHEN NO CONFIG AND VALIDATE
function setDefaultValue() {
    # host
    if [ "$host" == "" ]
    then
        host="127.0.0.1"
    fi
    # port: set default value
    if [ "$port" == "" ]
    then
        port="27017"
    fi    
    # db name
    if [ "$dbName" == "" ]
    then
        dbName="wiperdog"
    fi
}

# CHECK PORT MUST BE NUMBER
function checkPort() {
    export isNumber=1
    if ! [[ $1 =~ ^[0-9]+$ ]]
    then
        if [ "$1" == "" ]
        then
            isNumber=1
        else
	    isNumber=0
        fi
    fi
}

# SEND DATA INPUT TO GROOVY FILE
function sendToGroovy() {
    "./groovy" "./gendbmongoinfo.groovy" "$status" "$host" "$port" "$dbName" "$user" "$pass"
}

# CALL MAIN FUNCTION
main
