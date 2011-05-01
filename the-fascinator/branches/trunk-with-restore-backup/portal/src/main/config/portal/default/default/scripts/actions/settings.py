from au.edu.usq.fascinator.common import JsonConfig, JsonConfigHelper

from authentication import Authentication

from java.io import FileWriter
from java.util import HashMap

from org.apache.commons.io.output import NullWriter
from org.apache.commons.lang import StringUtils

class SettingsActions:
    def __init__(self):
        self.authentication = Authentication()
        self.authentication.session_init()

        self.writer = response.getPrintWriter("text/html; charset=UTF-8")

        if self.authentication.is_logged_in() and self.authentication.is_admin():
            self.process()
        else:
            print " * settings.py : AJAX : Unauthorised access"
            self.throw_error("Only administrative users can access this feature")

    def getWatcherFile(self):
        configFile = FascinatorHome.getPathFile("watcher/config.json")
        if configFile.exists():
            return JsonConfigHelper(configFile)
        return None

    def process(self):
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
                Services.getHouseKeepingManager().requestUrgentRestart()
            else:
                print " * settings.py: email not updated: did not change"
                self.throw_error("Email address is the same! No change saved.")

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
            queries = formData.getValues("backup-queries")
            if queries is None:
                queries = []
            paths = HashMap()
            for pathId in pathIds:
                if deletes is None or pathId not in deletes:
                    path = formData.get("%s-path" % pathId)
                    pathName = path.replace("/", "_").replace("${user.home}", "")
                    active = str(pathId in actives).lower()
                    #rendition = str(pathId in renditions).lower()
                    query = str(pathId in queries).lower()
                    ignoreFilter = formData.get("%s-ignore" % pathId)

                    json = HashMap()
                    json.put("path", path)
                    json.put("active", active)
                    json.put("include-portal-query", query)
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
            # backup the file
            JsonConfig.backupSystemFile()
            # delete the file
            JsonConfig.getSystemFile().delete()
            # restore default
            JsonConfig.getSystemFile()
            # mark restart
            Services.getHouseKeepingManager().requestUrgentRestart()

        elif func == "housekeeping-update":
            config = JsonConfig()
            freq = StringUtils.trimToEmpty(formData.get("housekeeping-timeout"))
            systemFreq = StringUtils.trimToEmpty(config.get("portal/houseKeeping/config/frequency"))
            result = "House Keeper refreshed"
            if systemFreq != freq:
                config.set("portal/houseKeeping/config/frequency", freq, True)
                config.store(NullWriter(), True)
                result = "Frequency updated, refreshing House Keeper"
            # Refresh the HouseKeeper
            message = JsonConfigHelper()
            message.set("type", "refresh")
            Services.getHouseKeepingManager().sendMessage(message.toString())

        self.writer.println(result)
        self.writer.close()

    def throw_error(self, message):
        response.setStatus(500)
        self.writer.println("Error: " + message)
        self.writer.close()

scriptObject = SettingsActions()
