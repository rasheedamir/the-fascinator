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

# show usage
function usage {
    echo "Usage: `basename $0` [jsonFile]"
    echo "Where [jsonFile] is a JSON configuration file."
    echo "If [jsonFile] is not an absolute path, the file is assumed to be in:"
    echo "    $HARVEST_DIR"
    echo "Available sample files:"
    for HARVEST_FILE in `ls $HARVEST_DIR/*.json`; do
        TMP=${HARVEST_FILE##*/harvest/}
        echo -n "    "
        echo $TMP | cut -d . -f 1-1
    done
}

# get fascinator home dir
pushd `dirname $0`
TF_HOME=`pwd`
popd

# setup environment
. $TF_HOME/tf_env.sh

HARVEST_DIR=$FASCINATOR_HOME/harvest
if [ "$1" == "" ]; then
    usage
    exit 0
fi

# get platform
OS=`uname`
if [ "$OS" == "Darwin" ]; then
    NUM_PROCS=`ps a | grep [j]etty | wc -l`
else
    NUM_PROCS=`pgrep -l -f jetty | wc -l`
fi

# only harvest if TF is running
if [ $NUM_PROCS == 1 ]; then
    if [ -f $1 ]; then
        JSON_FILE=$1
    else
        JSON_FILE=$HARVEST_DIR/$1.json
    fi
    if [ -f $JSON_FILE ]; then
        mvn -f $TF_HOME/core/pom.xml -P dev -Dexec.args=$JSON_FILE -Dexec.mainClass=au.edu.usq.fascinator.HarvestClient exec:java &> $FASCINATOR_HOME/logs/harvest.out
    else
        echo "ERROR: '$JSON_FILE' not found!"
        usage
    fi
else
    echo "Please start The Fascinator before harvesting."
fi
