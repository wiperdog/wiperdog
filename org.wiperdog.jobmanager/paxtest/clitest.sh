#!/bin/sh
mvn exec:java -Dexec.mainClass=com.insight_tec.pi.jobmanager.internal.AppMain -Dexec.args="$*"

