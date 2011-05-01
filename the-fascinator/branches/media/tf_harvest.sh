#!/bin/bash
#
# this script starts a fascinator harvest using maven
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

SAMPLE_DIR=$TF_HOME/core/src/test/resources
if [ "$1" == "" ]; then
	echo "Usage: `basename $0` <jsonFile>"
	echo "Where jsonFile is a JSON configuration file"
	echo "If jsonFile is not an absolute path, the file is assumed to be in:"
	echo "    $SAMPLE_DIR"
	echo "Available files:"
	for SAMPLE_FILE in `ls $SAMPLE_DIR/*.json.sample`; do
		TMP=${SAMPLE_FILE##*/resources/}
		echo -n "    "
		echo $TMP | cut -d . -f 1
	done
	exit 0
fi

function copy_sample {
	if [ ! -f $1.json ]; then
		cp "$1.json.sample" $1.json
	fi
	if [ ! -f $1.py ]; then
		cp "$1.py.sample" $1.py
	fi
}

# setup environment
. $TF_HOME/tf_env.sh

# get platform
NUM_PROCS=`pgrep -l -f fascinator | wc -l`
if [ $NUM_PROCS > 1 ]; then
	if [ -f $1.json ]; then
		BASE_FILE=$1
	else
		BASE_FILE=$SAMPLE_DIR/$1
	fi
	pushd $TF_HOME/core
	copy_sample $BASE_FILE
	mvn -P dev -DjsonFile=$BASE_FILE.json exec:java > harvest.out
	popd
else
	echo "Please start The Fascinator before harvesting."
fi
