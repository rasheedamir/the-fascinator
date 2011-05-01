#!/bin/bash

. tf_env.sh

OS=`uname`

if [ "$1" == "" ]; then
	echo "Usage: ./tf.sh start|stop|restart|check"
elif [ "$1" == "check" ]; then
	echo "Are these the droids you're looking for?"
	if [ "$OS" == "Darwin" ]; then
		ps a | grep [j]etty
		ps a | grep "[j]ava -jar start.jar"
	else
		pgrep -l -f jetty
		pgrep -l -f "java -jar start.jar"
	fi
elif [ "$1" == "stop" -o "$1" == "restart" ]; then
	echo "Stopping..."
	if [ "$OS" == "Darwin" ]; then
		killall java
	else
		pkill -f "java -jar start.jar"
		pkill -f jetty
	fi
fi

if [ "$1" == "start" -o "$1" == "restart" ]; then
	#echo "Updating..."
	#cd $FASCINATOR_HOME/code
	#mvn install &>portal.out
	#cd $OLDPWD
	if [ "$?" == "0" ]; then
		echo "Starting Solr..."
		cd $FASCINATOR_HOME/solr
		java -jar start.jar &>$OLDPWD/solr.out &
		cd $OLDPWD
		echo "Starting Portal..."
		cd $FASCINATOR_HOME/code/portal
		mvn -P test -Djetty.port=9997 jetty:run &>$OLDPWD/portal.out &
		cd $OLDPWD
	else
		echo "ERROR: Build failed. Please see $OLDPWD/portal.out for more details."
	fi
fi
