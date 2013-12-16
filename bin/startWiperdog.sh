#!/bin/sh
# Wiperdog Service Startup Script for Unix

# determine the prefix

self="$0"
while [ -h "$self" ]; do
	res=`ls -ld "$self"`
	ref=`expr "$res" : '.*-> \(.*\)$'`
	if expr "$ref" : '/.*' > /dev/null; then
		self="$ref"
	else
		self="`dirname \"$self\"`/$ref"
	fi
done
dir=`dirname "$self"`
PREFIX=`cd "$dir/.." && pwd`


# move to the Wiperdog home/bin

cd "$PREFIX/bin"

export FORK_PREFIX=$PREFIX
export FELIX_PORT=$port
export BUNDLE_LIST=$bundle
# Override felix port and bundle if user specify -f and -b flag for port and bundle parameters 
while getopts "f:b:" opt
do
   case "$opt" in
      f) export FELIX_PORT=$OPTARG;;
      b) export BUNDLE_LIST=$OPTARG ;;
      \?) echo "Example:./startWiperdog.sh -f 23111 -b BundleList0.csv " ;;
   esac
done

if [ ! $FELIX_PORT ]; then
  echo "Start wiperdog without specified an instance"
  echo "You can start with specific instance by syntax:"
  echo "port=xxxyyy bundle=ListBundle0.csv ./startWiperdog.sh"
  echo "Or syntax:"
  echo "./startWiperdog.sh -f xxxyyy -b ListBundleX.csv"
else
  export FORK_PREFIX=$PREFIX/fork/"$FELIX_PORT"
  if [ ! -d "$FORK_PREFIX" ]; then
    echo "Fork folder does not exists for port $FELIX_PORT, please try to create fork now"
   [ -x $PREFIX/bin/createFork.sh ] || exit 5
    /bin/sh "$PREFIX"/bin/createFork.sh -f $FELIX_PORT 
    if [ ! -d "$FORK_PREFIX" ]; then
       exit 1
    fi
  fi
fi

# temporary memory size setting, change this if this is too small(or too big).
export JAVA_OPTS=-Xmx256m

"./groovy" "./startWiperdog.groovy" "$@" -b $BUNDLE_LIST
