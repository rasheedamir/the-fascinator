from au.edu.usq.fascinator.portal import Portal

class ManifestActions:
    def __init__(self):
        print " * manifest.py: formData=%s" % formData
        result = "{}"
        func = formData.get("func")
        id = formData.get("id")
        nodeId = formData.get("nodeId")
        nodePath = self.__getNodePath(formData.get("parents"), nodeId)
        originalPath = "manifest//%s" % nodeId
        print "nodePath: %s" % nodePath
        portalManager = Services.getPortalManager()
        portal = portalManager.get(id)
        if func == "rename":
            title = formData.get("title")
            portal.set("%s/title" % nodePath, title)
            portalManager.save(portal)
        elif func == "move":
            refNodeId = formData.get("refNodeId")
            refNodePath = self.__getNodePath(formData.get("refParents"),
                                             formData.get("refNodeId"));
            print "refNodePath: %s" % refNodePath
            moveType = formData.get("type")
            if moveType == "before":
                portal.moveBefore(originalPath, refNodePath)
            elif moveType == "after":
                portal.moveAfter(originalPath, refNodePath)
            elif moveType == "inside":
                print "originalPath: %s" % originalPath
                portal.move(originalPath, nodePath)
            portalManager.save(portal)
        elif func == "set-hidden":
            portal.set("%s/hidden" % originalPath, formData.get("hidden"))
            portalManager.save(portal)
        elif func == "set-blog":
            portal.set("%s/blog" % originalPath, formData.get("blog"))
            portalManager.save(portal)
        writer = response.getPrintWriter("text/plain")
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

scriptObject = ManifestActions()
