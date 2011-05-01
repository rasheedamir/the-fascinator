from au.edu.usq.fascinator.api import PluginManager
from au.edu.usq.fascinator.portal import Portal

from java.io import ByteArrayInputStream, ByteArrayOutputStream

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
            newPortal = Portal(name)
            newPortal.setFacetFields(Services.portalManager.default.facetFields)
            newPortal.setQuery(query)
            self.__portal = newPortal
        else:
            portalName = formData.get("portalName")
            print " * settings.py: portalName=%s, portalId=%s" % (portalName, portalId)
            if portalName is None or (formData.get("portalAction") == "Cancel"):
                self.__portal = Services.portalManager.get(portalId)
            else:
                self.__portal = Portal(portalName)
                self.__portal.name = portalName
                Services.portalManager.add(self.__portal)
            if formData.get("portalAction") == "Update":
                self.__updatePortal()
            if formData.get("emailAction") == "Update":
                self.__updateEmail()
            if formData.get("backupAction") == "Update":    
                self.__updateBackupPaths()
        
    def __updateEmail(self):
        self.__portal.email = formData.get("emailAddress")
        Services.portalManager.save(self.__portal)
        
    def __updateBackupPaths(self):
        backupPaths = self.__portal.backupPaths
        size = int(formData.get("backupUrlSize"))
        for i in range (1, size+2):  
            keyName = "backupPaths_%s_name" % i
            activeName = "backupPaths_%s_active" % i
            includeName = "backupPaths_%s_include-rendition-meta" % i
            includePortal = "backupPaths_%s_include-portal-view" % i
            filterName = "backupPaths_%s_filter" % i
            
            name = formData.get(keyName)
            activeValue = formData.get(activeName)
            includeRenditionValue = formData.get(includeName)
            filterValue = formData.get(filterName)
            portalViewValue = formData.get(includePortal)
            #print " * setting.py Updatebackup Path: name='%s', active='%s', includeRenditionValue='%s', filter='%s', count='%s'" % \
            #    (name, activeValue, includeRenditionValue, filterValue, i) 
            if name is not None:
                newInfo = {}
                newInfo["active"] = activeValue is not None
                newInfo["include-rendition-meta"] = includeRenditionValue is not None
                newInfo["ignoreFilter"] = filterValue
                newInfo["include-portal-view"] = portalViewValue is not None
                backupPaths.put(name, newInfo)
            self.__portal.setBackupPaths(backupPaths)
        Services.portalManager.save(self.__portal)
    
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
            #print "key: %s, label: %s" % (name, label)
            if name is not None and label is not None:
                facetFields.put(name, label)
        self.__portal.setFacetFields(facetFields)
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

scriptObject = SettingsData()
