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

# get platform
OS=`uname`
if [ "$OS" == "Darwin" ]; then
	NUM_PROCS=`ps a | grep [j]etty | wc -l`
else
	NUM_PROCS=`pgrep -l -f jetty | wc -l`
fi

if [ "$1" == "status" ]; then
	if [ $NUM_PROCS == 1 ]; then
		echo "The Fascinator is RUNNING."
	else
		echo "The Fascinator is STOPPED."
	fi
elif [ "$1" == "stop" -o "$1" == "restart" ]; then
	if [ $NUM_PROCS == 1 ]; then
		echo "Stopping The Fascinator..."
		pushd $TF_HOME/portal
		mvn -P dev jetty:stop
		popd
	else
		echo "The Fascinator is already STOPPED."
	fi
fi

if [ "$1" == "start" -o "$1" == "restart" ]; then
	if [ $NUM_PROCS == 1 ]; then
		echo "The Fascinator is already RUNNING."
	else
		echo "Starting The Fascinator..."
		pushd $TF_HOME/portal
		mkdir -p $FASCINATOR_HOME/logs/
		nohup mvn -P dev jetty:run &> $FASCINATOR_HOME/logs/stdout.out &
		popd
	fi
	echo "Application logs: $FASCINATOR_HOME/logs"
	echo "Standard out log: $FASCINATOR_HOME/log/stdout.out"
fi

if [ "$1" == "build" -o "$1" == "rebuild" ]; then
	if [ $NUM_PROCS == 1 ]; then
		echo "The Fascinator is currently RUNNING."
		echo "Please stop it before attempting a $1."
	else
		echo "Building The Fascinator..."
		if [ "$1" == "build" ]; then
			mvn install
		else
			mvn clean install
		fi
	fi
fi

