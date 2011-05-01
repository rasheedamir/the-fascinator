from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common import JsonConfigHelper

from java.io import InputStreamReader
from java.lang import Exception
from java.util import ArrayList, HashMap

from org.apache.commons.lang import StringEscapeUtils

class OrganiserData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context

        print "formData: %s" % self.vc("formData")
        self.__oid = self.vc("formData").get("oid")
        result = None
        try:
            # get the package manifest
            self.__manifest = self.__readManifest(self.__oid)
            # check if we need to do processing
            func = self.vc("formData").get("func")
            if func == "get-rvt-manifest":
                result = self.__getRvtManifest(self.getManifest())
        except Exception, e:
            log.error("Failed to load manifest", e);
            result = '{ status: "error", message: "%s" }' % str(e)
        if result is not None:
            writer = self.vc("response").getPrintWriter("application/json; charset=UTF-8")
            writer.println(result)
            writer.close()
    
    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def getManifest(self):
        return self.__manifest.getJsonMap("manifest")
    
    def getFormData(self, field):
        return StringEscapeUtils.escapeHtml(self.vc("formData").get(field, ""))
    
    def getPackageTitle(self):
        return StringEscapeUtils.escapeHtml(self.vc("formData").get("title", self.__manifest.get("title")))
    
    def getMeta(self, metaName):
        return StringEscapeUtils.escapeHtml(self.vc("formData").get(metaName, self.__manifest.get(metaName)))
    
    def getManifestViewId(self):
        searchPortal = self.__manifest.get("viewId", defaultPortal)
        if Services.portalManager.exists(searchPortal):
            return searchPortal
        else:
            return defaultPortal
    
    def getMimeType(self, oid):
        return self.__getContentType(oid) or ""
    
    def getMimeTypeIcon(self, path, oid, altText = None):
        format = self.__getContentType(oid)
        return self.__getMimeTypeIcon(path, format, altText)

    def __getMimeTypeIcon(self, path, format, altText = None):
        if format[-1:] == ".":
            format = format[0:-1]
        if altText is None:
            altText = format
        # check for specific icon
        iconPath = "images/icons/mimetype/%s/icon.png" % format
        resource = Services.getPageService().resourceExists(self.vc("portalId"), iconPath)
        if resource is not None:
            return "<img class=\"mime-type\" src=\"%s/%s\" title=\"%s\" />" % (path, iconPath, altText)
        elif format.find("/") != -1:
            # check for major type
            return self.__getMimeTypeIcon(path, format.split("/")[0], altText)
        # use default icon
        iconPath = "images/icons/mimetype/icon.png"
        return "<img class=\"mime-type\" src=\"%s/%s\" title=\"%s\" />" % (path, iconPath, altText)

    def __getContentType(self, oid):
        #print " *** __getContentType(%s)" % oid
        contentType = ""
        if oid == "blank":
            contentType = "application/x-fascinator-blank-node"
        else:
            try:
                object = Services.getStorage().getObject(oid)
                sourceId = object.getSourceId()
                payload = object.getPayload(sourceId)
                contentType = payload.getContentType()
                payload.close()
                object.close()
            except StorageException, e:
                log.error("Error during getObject()", e)
        return contentType
    
    def __readManifest(self, oid):
        object = Services.getStorage().getObject(oid)
        sourceId = object.getSourceId()
        payload = object.getPayload(sourceId)
        payloadReader = InputStreamReader(payload.open(), "UTF-8")
        manifest = JsonConfigHelper(payloadReader)
        payloadReader.close()
        payload.close()
        object.close()
        return manifest
    
    def __getRvtManifest(self, manifest):
        rvtMap = HashMap()
        rvtMap.put("title", self.__manifest.get("title"))
        rvtMap.put("toc", self.__getRvtNodes(manifest))
        rvtManifest = JsonConfigHelper(rvtMap)
        return rvtManifest.toString()
    
    def __getRvtNodes(self, manifest):
        rvtNodes = ArrayList()
        #print "manifest=%s" % manifest
        for key in manifest.keySet():
            package = False
            node = manifest.get(key)
            try:
                # add the node
                rvtNode = HashMap()
                if node.get("hidden") != "True":
                    relPath = node.get("id")
                    # check if node is a package
                    if relPath:
                        package = (self.__getContentType(relPath) == "application/x-fascinator-package")
                    else:
                        relPath = key.replace("node", "blank")
                    rvtNode.put("visible", True)
                    rvtNode.put("title", node.get("title"))
                    if package:
                        subManifest = self.__readManifest(relPath)
                        if subManifest:
                            subManifest = subManifest.getJsonMap("manifest")
                            rvtNode.put("children", self.__getRvtNodes(subManifest))
                        relPath = key.replace("node", "package")
                    else:
                        rvtNode.put("children", self.__getRvtNodes(node.getJsonMap("children")))
                    rvtNode.put("relPath", relPath)
                    rvtNodes.add(rvtNode)
            except Exception, e:
                log.error("Failed to process node '%s': '%s'" % (node.toString(), str(e)))
        return rvtNodes
