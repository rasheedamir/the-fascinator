from au.edu.usq.fascinator.common import JsonConfigHelper
from au.edu.usq.fascinator.portal import Portal
from java.util import HashMap

class FacetActions:
    def __init__(self):
        print " * facets.py: formData=%s" % formData
        result = "{}"
        portalManager = Services.getPortalManager()
        portal = portalManager.get(portalId)
        func = formData.get("func")
        if func == "view-update":
            portal.setDescription(formData.get("view-description"))
            portal.setQuery(formData.get("view-query"))
            portal.setRecordsPerPage(int(formData.get("view-records-per-page")))
            portal.setFacetCount(int(formData.get("view-facet-count")))
            portal.setFacetSort(formData.get("view-facet-sort") is not None)
            portalManager.save(portal)
        elif func == "facets-update":
            portal.removePath("portal/facet-fields")
            fields = formData.getValues("field")
            labels = formData.getValues("label")
            displays = formData.getValues("display")
            deletes = formData.getValues("delete")
            for i in range(0, len(fields)):
                field = fields[i]
                if deletes[i] == "false":
                    portal.set("portal/facet-fields/%s/label" % field, labels[i])
                    portal.set("portal/facet-fields/%s/display" % field, displays[i])
            portalManager.save(portal)
        elif func == "backup-update":
            pathIds = formData.get("pathIds").split(",")
            actives = formData.getValues("backup-active")
            deletes = formData.getValues("backup-delete")
            if actives is None:
                actives = []
            #renditions = formData.getValues("backup-rendition")
            #if renditions is None:
            #    renditions = []
            views = formData.getValues("backup-view")
            if views is None:
                views = []
            paths = HashMap()
            for pathId in pathIds: 
                if deletes is None or pathId not in deletes:
                    path = formData.get("%s-path" % pathId)
                    pathName = path.replace("/", "_").replace("${user.home}", "")
                    active = str(pathId in actives).lower()
                    #rendition = str(pathId in renditions).lower()
                    view = str(pathId in views).lower()
                    ignoreFilter = formData.get("%s-ignore" % pathId)
                    
                    json = HashMap()
                    json.put("path", path)
                    json.put("active", active)
                    json.put("include-portal-view", view)
                    json.put("ignoreFilter", ignoreFilter)
                    
                    storage = HashMap()
                    storage.put("type", "file-system")
                    
                    filesystem = HashMap()
                    filesystem.put("home", path)
                    filesystem.put("use-link", "false")
                    storage.put("file-system", filesystem)
                    
                    json.put("storage", storage)
                    paths.put(pathName, json)
            # reset the path first
            portal.setMap("portal/backup/paths", HashMap())
            portal.setMultiMap("portal/backup/paths", paths)
            portalManager.save(portal)
        writer = response.getPrintWriter("text/plain")
        writer.println(result)
        writer.close()

scriptObject = FacetActions()
