#Work out proxy info
TMP=${http_proxy#*//}
HOST=${TMP%:*}
TMP=${http_proxy##*:}
PORT=${TMP%/}

#Set environment vars
OS=`uname`
if [ "$OS" == "Darwin" ] ; then
	JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home
else
	#Assume Ubuntu with sun-java6-jdk installed
	JAVA_HOME=/usr/lib/jvm/java-6-sun
fi
export JAVA_HOME
export FASCINATOR_HOME=/opt/the-fascinator2
export SOLR_HOME=/opt/the-fascinator2/solr/solr
export JAVA_OPTS="-Xmx512m -Dsolr.solr.home=$SOLR_HOME -Dsolr.data.dir=$SOLR_HOME/data -Dhttp.proxyHost=$HOST -Dhttp.proxyPort=$PORT -Dhttp.nonProxyHosts=localhost"
export MAVEN_OPTS=$JAVA_OPTS

#Check system-config.json and copy if necessary
if [ ! -f ~/.fascinator/system-config.json ] ; then
	cp $FASCINATOR_HOME/code/common/src/main/resources/system-config.json ~/.fascinator
fi

