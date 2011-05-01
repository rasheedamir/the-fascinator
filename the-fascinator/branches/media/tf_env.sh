#!/bin/bash
#
export FASCINATOR_HOME=/u01/tf-home
export SOLR_BASE_DIR=$FASCINATOR_HOME
export MAVEN_OPTS="-XX:MaxPermSize=512m -Xmx1024m -Dfascinator.home=$FASCINATOR_HOME -Dsolr.base.dir=$SOLR_BASE_DIR"
