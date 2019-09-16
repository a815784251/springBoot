#!/bin/bash

export JAVA_HOME=/usr/local/jdk1.8.0_45
export PATH=${JAVA_HOME}/bin:${PATH}

export SERVICE_NAME=LDC-Console
export JAVA_MAIN_CLASS=com.web.console.ServerLaunch
export DEBUG_PORT=2222
export JCONSOLE_PORT=3333
export JAVA_OPTS="-Xmx256m -Xms128m -XX:+PrintGCDetails -Xloggc:logs/gc.log -Dcom.sun.management.jmxremote.port=9977 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
cd `dirname $0`
export SCRIPT_NAME=$0

./_server.sh $@
