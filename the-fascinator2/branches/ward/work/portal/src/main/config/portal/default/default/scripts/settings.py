from au.edu.usq.fascinator.api import PluginManager
from au.edu.usq.fascinator.common import FascinatorHome, JsonConfig, JsonConfigHelper
from au.edu.usq.fascinator.portal import Portal

from java.io import ByteArrayInputStream, ByteArrayOutputStream, File
from java.util import HashMap

class SettingsData:

    def __init__(self):
        self.__portal = None
        action = formData.get("verb")
        if action == "create_view":
            fq = [q for q in sessionState.get("fq") if q != 'item_type:"object"']
            if fq == []:
                name = "new"
                desc = "New View"
                query = ""
            else:
                name = ""
                desc = ""
                query = str(" ".join(fq))
            newPortal = Portal(name, desc, query)
            newPortal.setFacetFields(Services.portalManager.default.facetFields)
            newPortal.setQuery(query)
            self.__portal = newPortal
        else:
            portalName = formData.get("portalName")
            print " * settings.py: portalName=%s, portalId=%s" % (portalName, portalId)
            if portalName is None or (formData.get("portalAction") == "Cancel"):
                self.__portal = Services.portalManager.get(portalId)
            else:
                self.__portal = Portal()
                self.__portal.name = portalName
                Services.portalManager.add(self.__portal)
            if formData.get("portalAction") == "Update":
                self.__updatePortal()
            if formData.get("emailAction") == "Update":
                self.__updateEmail()
    
    def isSelected(self, category):
        selected = sessionState.get("settingsCategory")
        if category == selected:
            return "selected"
        return ""
    
    def __updatePortal(self):
        self.__portal.name = formData.get("portalName")
        self.__portal.description = formData.get("portalDescription")
        self.__portal.query = formData.get("portalQuery")
        self.__portal.recordsPerPage = int(formData.get("portalRecordsPerPage"))
        self.__portal.facetCount = int(formData.get("portalFacetLimit"))
        self.__portal.facetSort = formData.get("portalFacetSort") is not None
        facetFields = self.__portal.facetFields
        facetFields.clear()
        size = int(formData.get("portalFacetSize"))
        for i in range(1,size+2):
            nameKey = "portalFacet_%s_name" % i
            labelKey = "portalFacet_%s_label" % i
            name = formData.get(nameKey)
            label = formData.get(labelKey)
            print "key: %s, label: %s" % (name, label)
            if name is not None and label is not None:
                facetFields.put(name, label)
        Services.portalManager.save(self.__portal)
    
    def getPortal(self):
        return self.__portal
    
    def getIndexerPlugins(self):
        return PluginManager.getIndexerPlugins()
    
    def getStoragePlugins(self):
        return PluginManager.getStoragePlugins()
    
    def getHarvesterPlugins(self):
        return PluginManager.getHarvesterPlugins()
    
    def getTransformerPlugins(self):
        return PluginManager.getTransformerPlugins()
    
    def getWatcherConfig(self):
        json = JsonConfig()
        watcherPath = json.get("watcher/path", "${fascinator.home}/watcher)")
        configFile = File("%s/app/config.json" % watcherPath)
        if configFile.exists():
            return JsonConfigHelper(configFile)
        return None
    
    def getEmail(self):
        return JsonConfig().get("email")
    
    def getTimeout(self):
        return JsonConfig().get("portal/houseKeeping/config/frequency")
    
    def getFacetDisplays(self):
        facetDisplays = self.__portal.getMap("portal/facet-displays")
        if facetDisplays is None or facetDisplays.isEmpty():
            facetDisplays = HashMap()
            facetDisplays.put("list", "List menu")
            facetDisplays.put("tree", "Dynamic tree")
        return facetDisplays

scriptObject = SettingsData()
