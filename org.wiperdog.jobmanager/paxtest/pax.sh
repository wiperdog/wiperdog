#!/bin/sh
# 止めるときは ^C で。
#   デフォルトで使用されるfelixのshellが gogo なので、shutdownコマンドが無い
# 実行には maven2のインストールが必要
#
TESTDIR=$(cd $(dirname $0);pwd)
# WORKINGDIR=/tmp/runner
WORKINGDIR=$TESTDIR
MAVEN=/usr/bin/mvn
JETTYPORT=18080
JETTYHOST=0.0.0.0

VMOPTIONS="$VMOPTIONS -Dfelix.home=$TESTDIR"
VMOPTIONS="$VMOPTIONS -Dfelix.fileinstall.dir=$TESTDIR/etc"
VMOPTIONS="$VMOPTIONS -Dfelix.fileinstall.filter=.*\\.cfg"
VMOPTIONS="$VMOPTIONS -Dfelix.fileinstall.noInitialDelay=true"
VMOPTIONS="$VMOPTIONS -Dfelix.fileinstall.poll=2000"
# VMOPTIONS="$VMOPTIONS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=23110"

# generate runner.args file
cat <<EOF_RUNNER_ARGS > runner.args
--bundleStartLevel=3
--systemPackages=javax.ejb,javax.jms,javax.mail,javax.mail.internet
--workingDirectory=$WORKINGDIR
--vmOptions=${VMOPTIONS}
--platform=felix
--version=2.0.5

EOF_RUNNER_ARGS

# start pax runner with using maven
$MAVEN -f paxpom.xml test

