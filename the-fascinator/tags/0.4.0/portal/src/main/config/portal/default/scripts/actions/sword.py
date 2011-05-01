from au.edu.usq.fascinator.portal import SwordSimpleServer
from java.io import File
from java.lang import Exception
from org.apache.commons.io import FileUtils
from org.purl.sword.client import Client, PostMessage

class SwordHelper(object):
    def __init__(self):
        self.__processRequest()

    def __processRequest(self):
        func = formData.get("func")
        url = formData.get("url")
        client = Client()
        client.setCredentials(formData.get("username"), formData.get("password"))
        if func == "collections":
            responseType = "application/json"
            try:
                serviceDoc = client.getServiceDocument(url)
                data = '{"collections":['
                for w in serviceDoc.service.workspaces:
                    for c in w.collections:
                        data += '{"title":"%s","location":"%s"}' % (c.title, c.location)
                data += ']}'
                responseData = data
            except Exception, e:
                print str(e)
                responseData = '{"error":"%s"}' % str(e)
        elif func == "post":
            tmpFile = File.createTempFile("ims-", ".zip")
            from actions.imscp import ImsPackage
            imscp = ImsPackage(tmpFile)
            postMsg = PostMessage()
            postMsg.setFiletype("application/zip")
            postMsg.setFilepath(tmpFile.getAbsolutePath())
            postMsg.setDestination(url)
            depositResponse = client.postFile(postMsg)
            FileUtils.deleteQuietly(tmpFile)
            responseType = "text/xml"
            responseData = str(depositResponse)
        else:
            responseType = "text/html"
            responseData = ""
        out = response.getPrintWriter(responseType)
        out.println(responseData)
        out.close()

scriptObject = SwordHelper()
