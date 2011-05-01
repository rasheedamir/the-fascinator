#!/bin/bash
#
# this script controls the fascinator using maven and jetty
# only usable when installed in development mode
#

# suppress console output from pushd/popd
pushd() {
	builtin pushd "$@" > /dev/null
}
popd() {
	builtin popd "$@" > /dev/null
}

# get fascinator home dir
pushd `dirname $0`
TF_HOME=`pwd`
popd

if [ "$1" == "" ]; then
	echo "Usage: `basename $0` start|stop|restart|status|build|rebuild"
	exit 0
fi

# setup environment
. $TF_HOME/tf_env.sh

if [ "$1" == "stop" -o "$1" == "restart" ]; then
	echo "Stopping The Fascinator..."
	pushd $TF_HOME/portal
	mvn -P dev jetty:stop
	popd
fi

if [ "$1" == "start" -o "$1" == "restart" ]; then
	echo "Starting The Fascinator..."
	pushd $TF_HOME/portal
	nohup mvn -P dev jetty:run &> $TF_HOME/portal.out &
	popd
	echo "Application logs: $FASCINATOR_HOME/logs"
	echo "Standard out log: $TF_HOME/portal.out"
fi

if [ "$1" == "build" -o "$1" == "rebuild" ]; then
	echo "Building The Fascinator..."
	if [ "$1" == "build" ]; then
		mvn install
	else
		mvn clean install
	fi
fi

