"""*** NOTE: this is a draft version of export to fedora script. 
You might need to modify the script to fit your needs.

Based on payloadToExport values, this script will create the datastream 
if not exist in fedora or replace the existing datastream if it's found.

Based on payloadToDelete values, this script will also remove the payload 
from fascinator and datastream from fedora.
"""

""" CHANGE THE FEDORA url, username and password """
fedoraUrl = "http://localhost:8001/fedora/"
username = "fedoraAdmin"
password = "fedoraAdmin"

from fedora.client import FedoraClient
from org.apache.commons.io import IOUtils
from au.edu.usq.fascinator.harvester.fedora.restclient import FedoraRestClient
from java.io import File, FileOutputStream
from java.lang import String
import time

def checkIfDsExist(apim, fedoraId, value, dsState):
    dataStreams = apim.getDatastreams(fedoraId, value, dsState)
    found = False 
    foundDs = None
    for ds in dataStreams:
        if ds.getID()==value:
            found = True
            foundDs = ds
            break 
    return found, foundDs

print "** Export to Fedora2 **"

client = FedoraClient(fedoraUrl, username, password) 
apim = client.getAPIM()
restClient = FedoraRestClient(fedoraUrl)

#Delete the payload that is supposed to be deleted
props = object.getMetadata()
e = props.propertyNames()
fedoraId = props.getProperty("fedoraPid")
fedoraId = "uon:20"
while (e.hasMoreElements()):
    key = str(e.nextElement())
    value = props.getProperty(key)
    if key.startswith("payloadToDelete."):
        #Notify fedora to delete this datastream
        print " * Deleting datastream: '%s'" % value
        #check if datastream exist
        found, foundDs = checkIfDsExist(apim, fedoraId, value, dsState)
        if found:
            apim.purgeDatastream(fedoraId, foundDs.getID(), None, None, "Deleting datastream %s" % foundDs.getID(), False)
            print " * Deleted '%s'" % value 
        else:
            print " * '%s' datastream not exist in '%s' object" % (value, fedoraId) 
        
        #Notify fascinator to delete the payload
        try:
            object.removePayload(props.getProperty(key))
        except:
            print "Fail to delete payload from fascinator: '%s'" % value
        props.remove(key)
    if key.startswith("payloadToExport."):
        #Notify fedora to add this datastream
        print " ** Exporting modified datastream: ", value
        payload = object.getPayload(value)
        #Check if object exist
        objectResult = restClient.findObjects(fedoraId, 1)
        if not objectResult.getObjectFields().isEmpty():
            try:
                mimeType = payload.getContentType()
                formatURI = None
                dsLocation = None
                dsState = "A"
                #Check if datastream exist
                print "Getting datastream for: ", value
                found, foundDs = checkIfDsExist(apim, fedoraId, value, dsState)
                if found:
                    print " * Modifying: '%s'" % value
                    f = File.createTempFile("fedora-", ".xml")
                    fOut = FileOutputStream(f)
                    IOUtils.copy(payload.open(), fOut)
                    payload.close()
                    fOut.close()
                    dsLocation = client.uploadFile(f)
                    dsId = apim.modifyDatastreamByReference(fedoraId, foundDs.getID(), foundDs.getAltIDs(), foundDs.getLabel(), \
                                                     mimeType, foundDs.getFormatURI(), dsLocation, foundDs.getChecksumType(), foundDs.getChecksum(), \
                                                     "Updating datastream from Fascinator", True)
                    print " * Modified: '%s'" % dsId
                else:
                    print " * Adding: '%s'" % value
                    f = File.createTempFile("fedora-", ".xml")
                    fOut = FileOutputStream(f)
                    IOUtils.copy(payload.open(), fOut)
                    payload.close()
                    fOut.close()
                    dsLocation = client.uploadFile(f)
                    controlGroup = "M"  
                    dsId = apim.addDatastream(fedoraId, value, None, value, True, mimeType, formatURI, dsLocation, controlGroup, dsState, None, None, "Creating new datastream")
                    print " * Added datastream: '%s'" % dsId
                    
                #set the value of last exported
                props.setProperty("lastExported", time.strftime("%Y-%m-%dT%H:%M:%SZ"))
            except Exception, e:
                #Remote exception
                print "Error: ", str(e)
        else:
            print "Object id: '%s' is not exist in Fedora" % fedoraPid
        props.remove(key)

#reset modified property after export
resetModifiedProperty = props.getProperty("resetModifiedProperty", "false")
if resetModifiedProperty == "true":
    props.setProperty("modified", "false")

object.close()
