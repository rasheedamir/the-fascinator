from au.edu.usq.fascinator.common import JsonConfig, JsonConfigHelper
from au.edu.usq.fascinator.portal import Portal

from java.io import File, FileWriter
from java.util import HashMap

from org.apache.commons.io.output import NullWriter
from org.apache.commons.lang import StringUtils

class SettingsActions:
    def __init__(self):
        print " * settings.py: formData=%s" % formData
        result = "{}"
        portalManager = Services.getPortalManager()
        portal = portalManager.get(portalId)
        func = formData.get("func")
        if func == "view-update":
            portal.setDescription(formData.get("view-description"))
            portal.setQuery(formData.get("view-query"))
            portal.setSearchQuery(formData.get("view-search-query"))
            print " *** ", formData.get("view-records-per-page")
            portal.setRecordsPerPage(int(formData.get("view-records-per-page")))
            portal.setFacetCount(int(formData.get("view-facet-count")))
            portal.setFacetSort(formData.get("view-facet-sort") is not None)
            portalManager.save(portal)
        elif func == "general-update":
            config = JsonConfig()
            email = StringUtils.trimToEmpty(formData.get("general-email"))
            systemEmail = StringUtils.trimToEmpty(config.get("email"))
            print email, systemEmail
            if systemEmail != email:
                config.set("email", formData.get("general-email"), True)
                config.set("configured", "true", True)
                config.store(NullWriter(), True)
                # mark restart
                sessionState.set("need-restart", "true")
            else:
                print " * settings.py: email not updated: did not change"
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
        elif func == "watcher-update":
            configFile = self.getWatcherFile()
            if configFile is not None:
                pathIds = formData.get("pathIds").split(",")
                actives = formData.getValues("watcher-active")
                if actives is None:
                    actives = []
                deletes = formData.getValues("watcher-delete")
                if deletes is None:
                    deletes = []
                watchDirs = HashMap()
                for pathId in pathIds:
                    if pathId not in deletes:
                        path = formData.get("%s-path" % pathId)
                        stopped = str(pathId not in actives).lower()
                        watchDir = HashMap()
                        watchDir.put("ignoreFileFilter", formData.get("%s-file" % pathId))
                        watchDir.put("ignoreDirectories", formData.get("%s-dir" % pathId))
                        watchDir.put("cxtTags", [])
                        watchDir.put("stopped", stopped)
                        watchDirs.put(path, watchDir)
                json = JsonConfigHelper(self.getWatcherFile())
                json.setMap("watcher/watchDirs", watchDirs)
                json.store(FileWriter(configFile), True)
            else:
                result = "The Watcher is not installed properly."
        elif func == "restore-default-config":
            # delete the file
            JsonConfig.getSystemFile().delete()
            # restore default
            JsonConfig.getSystemFile()
            # mark restart
            sessionState.set("need-restart", "true")
        writer = response.getPrintWriter("text/plain; charset=UTF-8")
        writer.println(result)
        writer.close()
    
    def getWatcherFile(self):
        configFile = FascinatorHome.getPathFile("watcher/config.json")
        if configFile.exists():
            return JsonConfigHelper(configFile)
        return None

scriptObject = SettingsActions()
