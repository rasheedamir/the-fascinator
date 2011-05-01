from au.edu.usq.fascinator.common import JsonConfigHelper

from java.io import ByteArrayInputStream
from java.lang import Boolean, String

from org.apache.commons.lang import StringEscapeUtils

class ManifestActions:
    def __init__(self):
        print "formData=%s" % formData
        
        result = "{}"
        func = formData.get("func")
        oid = formData.get("oid")
        
        if func != "set-package-title":
            nodeId = formData.get("nodeId")
            nodePath = self.__getNodePath(formData.get("parents"), nodeId)
            originalPath = "manifest//%s" % nodeId
        
        self.__object = Services.getStorage().getObject(oid)
        sourceId = self.__object.getSourceId()
        payload = self.__object.getPayload(sourceId)
        self.__manifest = JsonConfigHelper(payload.open())
        payload.close()
        
        if func == "set-package-title":
            title = formData.get("title")
            self.__manifest.set("title", StringEscapeUtils.escapeHtml(title))
            self.__saveManifest()
        if func == "rename":
            title = formData.get("title")
            self.__manifest.set("%s/title" % nodePath, title)
            self.__saveManifest()
        elif func == "move":
            refNodeId = formData.get("refNodeId")
            refNodePath = self.__getNodePath(formData.get("refParents"),
                                             formData.get("refNodeId"));
            moveType = formData.get("type")
            if moveType == "before":
                self.__manifest.moveBefore(originalPath, refNodePath)
            elif moveType == "after":
                self.__manifest.moveAfter(originalPath, refNodePath)
            elif moveType == "inside":
                self.__manifest.move(originalPath, nodePath)
            self.__saveManifest()
        elif func == "update":
            title = StringEscapeUtils.escapeHtml(formData.get("title"))
            hidden = formData.get("hidden")
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
        writer = response.getPrintWriter("text/plain; charset=UTF-8")
        writer.println(result)
        writer.close()
    
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

scriptObject = ManifestActions()
