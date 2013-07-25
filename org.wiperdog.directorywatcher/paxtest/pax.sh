#!/bin/sh
#  felixのバージョンは2.0.5を指定しているので、shutdownコマンドで停止できる。
# 
# 依存ファイル: 
#    paxpom.xml
#    bundlelist.txt
#
#
POMNAME=$1
if [ "$POMNAME" = "" ]; then
	POMNAME="paxpom.xml"
fi
#
TESTDIR=$(cd $(dirname $0);pwd)
# TESTDIR=`cd \`dirname $0\`;pwd`
# WORKINGDIR=/tmp/runner
WORKINGDIR=$TESTDIR
MAVEN=/usr/bin/mvn
# JETTYPORT=18080
# JETTYHOST=0.0.0.0

VMOPTIONS="$VMOPTIONS -Dfelix.home=$TESTDIR"
VMOPTIONS="$VMOPTIONS -Dfelix.fileinstall.dir=$TESTDIR/etc"
VMOPTIONS="$VMOPTIONS -Dfelix.fileinstall.filter=.*\\.cfg"
VMOPTIONS="$VMOPTIONS -Dfelix.fileinstall.noInitialDelay=true"
VMOPTIONS="$VMOPTIONS -Dfelix.fileinstall.poll=2000"

# generate runner.args file
cat <<EOF_RUNNER_ARGS > runner.args
--bundleStartLevel=4
--systemPackages=javax.xml.ws.wsaddressing
--workingDirectory=$WORKINGDIR
--vmOptions=${VMOPTIONS}
--platform=felix
--version=2.0.5

# -Dorg.ops4j.pax.url.mvn.repositories=http://192.168.10.50:8081/artifactory/libs-release-local/

EOF_RUNNER_ARGS

# start pax runner with using maven
$MAVEN -f ${POMNAME} test

