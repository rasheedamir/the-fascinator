#!/bin/bash
#
# this script is used for deleting fascinator data
# data will be deleted:
# 1. $FASCINATOR_HOME/storage
# 2. $FASCINATOR_HOME/activemq-data
# 3. $FASCINATOR_HOME/logs
# 4. $FASCINATOR_HOME/cache
# 5. $SOLR_BASE_DIR/solr/indexes/anotar/index
# 6. $SOLR_BASE_DIR/solr/indexes/fascinator/index
# 7. $SOLR_BASE_DIR/solr/indexes/security/index

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
	echo "Usage: ./`basename $0` all|solr"
	exit 0
fi

# setup environment
. $TF_HOME/tf_env.sh

if [ "$1" == "all" ]; then
    echo Deleting all data
    echo Deleting: $FASCINATOR_HOME/test/storage
    rm -rf $FASCINATOR_HOME/test/storage

    echo Deleting: $FASCINATOR_HOME/test/activemq-data
    rm -rf $FASCINATOR_HOME/test/activemq-data

    echo Deleting: $FASCINATOR_HOME/test/logs
    rm -rf $FASCINATOR_HOME/test/logs

    echo Deleting: $FASCINATOR_HOME/test/cache
    rm -rf $FASCINATOR_HOME/test/cache
fi

if [ "$1" == "all" -o "$1" == "solr" ]; then
    echo Deleting solr data
    
    echo Deleting: $SOLR_BASE_DIR/test/solr/indexes/anotar/index
    rm -rf $SOLR_BASE_DIR/test/solr/indexes/anotar/index

    echo Deleting: $SOLR_BASE_DIR/test/solr/indexes/fascinator/index
    rm -rf $SOLR_BASE_DIR/test/solr/indexes/fascinator/index

    echo Deleting: $SOLR_BASE_DIR/test/solr/indexes/security/index
    rm -rf $SOLR_BASE_DIR/test/solr/indexes/security/index
fi

