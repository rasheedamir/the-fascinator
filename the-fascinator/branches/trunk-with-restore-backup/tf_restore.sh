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
    echo "Usage: `basename $0` [path-to-be-restored]"
    echo "Where [path-to-be-restored] is wbere the directory will be restored."
    echo "If [path-to-be-restored] is an absolute path"
}

# get fascinator home dir
pushd `dirname $0`
TF_HOME=`pwd`
popd

# setup environment
. $TF_HOME/tf_env.sh

# get platform
OS=`uname`
if [ "$OS" == "Darwin" ]; then
    NUM_PROCS=`ps a | grep [j]etty | wc -l`
else
    NUM_PROCS=`pgrep -l -f jetty | wc -l`
fi

# only harvest if TF is running
if [ $NUM_PROCS == 1 ]; then
    if [ -f $2 ]; then
        RESTORE_PATH=$1
        mvn -f $TF_HOME/core/pom.xml -P dev -Dexec.args=$RESTORE_PATH -Dexec.mainClass="au.edu.usq.fascinator.RestoreClient" exec:java &> $FASCINATOR_HOME/logs/restore.out
    else
        usage
    fi
else
    echo "Please start The Fascinator before harvesting."
fi
