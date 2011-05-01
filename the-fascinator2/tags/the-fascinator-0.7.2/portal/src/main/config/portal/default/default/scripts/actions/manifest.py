from au.edu.usq.fascinator.common import JsonConfigHelper

from java.io import ByteArrayInputStream
from java.lang import String

from org.apache.commons.lang import StringEscapeUtils

class ManifestData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        auth = context["page"].authentication
        if auth.is_logged_in():
            self.fd = self.vc("formData").get

            print "formData=%s" % self.vc("formData")
            result = "{}"
            func = self.fd("func")
            oid = self.fd("oid")
    
            if func != "update-package-meta":
                nodeId = self.fd("nodeId")
                nodePath = self.__getNodePath(self.fd("parents"), nodeId)
                originalPath = "manifest//%s" % nodeId
    
            self.__object = Services.getStorage().getObject(oid)
            sourceId = self.__object.getSourceId()
            payload = self.__object.getPayload(sourceId)
            self.__manifest = JsonConfigHelper(payload.open())
            payload.close()
    
            if func == "update-package-meta":
                print "*********  update-package-meta ***************"
                metaList = list(self.vc("formData").getValues("metaList"))
                for metaName in metaList:
                    value = self.fd(metaName)
                    self.__manifest.set(metaName, value)
                #title = formData.get("title")
                #self.__manifest.set("title", StringEscapeUtils.escapeHtml(title))
                self.__saveManifest()
            if func == "rename":
                title = self.fd("title")
                self.__manifest.set("%s/title" % nodePath, title)
                self.__saveManifest()
            elif func == "move":
                refNodeId = self.fd("refNodeId")
                refNodePath = self.__getNodePath(self.fd("refParents"),
                                                 self.fd("refNodeId"));
                moveType = self.fd("type")
                if moveType == "before":
                    self.__manifest.moveBefore(originalPath, refNodePath)
                elif moveType == "after":
                    self.__manifest.moveAfter(originalPath, refNodePath)
                elif moveType == "inside":
                    self.__manifest.move(originalPath, nodePath)
                self.__saveManifest()
            elif func == "update":
                title = StringEscapeUtils.escapeHtml(self.fd("title"))
                hidden = self.fd("hidden")
                hidden = hidden == "true"
                self.__manifest.set("%s/title" % nodePath, title)
                self.__manifest.set("%s/hidden" % nodePath, str(hidden))
                #if self.__manifest.get("%s/id" % nodePath) is None:
                #    print "blank node!"
                self.__saveManifest()
                result = '{ title: "%s", hidden: "%s" }' % (title, hidden)
            elif func == "delete":
                title = self.__manifest.get("%s/title" % nodePath)
                if title:
                    self.__manifest.removePath(nodePath)
                    self.__saveManifest()
                else:
                    title = "Untitled"
                result = '{ title: "%s" }' % title
            
            self.__object.close()
        else:
            result = '{ "status": "error", "message": "Only registered users can access this API" }'
        
        writer = self.vc("response").getPrintWriter("text/plain; charset=UTF-8")
        writer.println(result)
        writer.close()
    
    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def __getNodePath(self, parents, nodeId):
        parents = [p for p in parents.split(",") if p != ""]
        nodePath = "manifest/%s" % nodeId
        if len(parents) > 0:
            nodePath = ""
            for parent in parents:
                if nodePath == "":
                    nodePath = "manifest/%s"  % parent
                else:
                    nodePath += "/children/%s" % parent
            nodePath += "/children/%s" % nodeId
        return nodePath
    
    def __saveManifest(self):
        manifestStr = String(self.__manifest.toString())
        self.__object.updatePayload(self.__object.getSourceId(),
                                    ByteArrayInputStream(manifestStr.getBytes("UTF-8")))
