#!/bin/sh
#
# Undertakes a Maven release of the Archetypes
# Also creates a maven site for the version
#
# Usage: archetype.sh <archetype-name> <release-version> <new-snapshot-version>
#
# Where version is the SVN tag name that will be used.
#   For example: archetype.sh plugin-harvester-archetype 0.7.3 0.7.4-SNAPSHOT 

ARCHETYPE_NAME=$1
VERSION=$2
SNAPSHOT=$3
BASE_URL=https://fascinator.usq.edu.au/svn-auth/code/the-fascinator2/contrib/maven/archetypes/$ARCHETYPE_NAME
TRUNK=$BASE_URL/trunk/
DS=`date +%Y%m%d%H%M%S`
TMP=/tmp/release/$VERSION/$DS
echo "TMP Folder: $TMP"

if [ "$1" = "" ];
then
  echo "Please provide a release name"
  exit
fi

if [ "$2" = "" ];
then
  echo "Please provide a release version"
  exit
fi

if [ "$3" = ""];
then
  echo "Please provide a snapshot version"
  exit
fi

echo Creating tag for Version $1

mkdir -p $TMP
cd $TMP

#Checkout trunk pom.xml
if ! svn co $TRUNK . ; then        
  echo "Unable to checkout pom.xml from $TRUNK";
  exit 1;
fi

#Prepare for release
if ! mvn --batch-mode -Dtag=$ARCHETYPE_NAME-$VERSION release:prepare -DreleaseVersion=$VERSION -DdevelopmentVersion=$SNAPSHOT; then
  echo "Failed to prepare the release - please check the codebase."
  exit 1;
fi

mkdir /var/www/hudson/$ARCHETYPE_NAME-$VERSION

#Deploy and create maven site for the release
#To disable the maven site creation put -Dgoals=deploy
if ! mvn release:perform; then
  echo "Fail to deploy the release and the maven site"
  exit 1;
fi

#Now, tidy up
#svn delete $RELEASE -m "Tidying up after release $VERSION"
rm -rf $TMP

