#!/bin/bash
cd /var/lib/hudson/jobs/The\ Fascinator\ Full\ Build/workspace/trunk/
mvn site:stage-deploy -DstagingDirectory=/tmp/fascinator-site -DstagingSiteURL=file:///var/www/hudson/the-fascinator-full
rm -rf /tmp/fascinator-site
