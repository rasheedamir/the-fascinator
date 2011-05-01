
class Reindex:
    def __init__(self):
        if formData.get("func") == "reindex":
            file = formData.get("file")
            portalId = formData.get("portalId")
            portalManager = Services.getPortalManager()
            if file:
                print " * Reindexing: formData=%s" % file
                portalManager.indexObject(file)
            elif portalId:
                portal = portalManager.get(portalId)
                print " * Reindexing: Portal=%s" % portal.name
                portalManager.indexPortal(portal)
                
scriptObject = Reindex()
