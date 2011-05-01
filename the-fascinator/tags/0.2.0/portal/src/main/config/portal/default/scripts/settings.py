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
            if formData.get("backupAction") == "Update":    
                self.__updateBackupPaths()
        
    def __updateEmail(self):
        self.__portal.email = formData.get("emailAddress")
        Services.portalManager.save(self.__portal)
        
    def __updateBackupPaths(self):
        backupPaths = self.__portal.backupPaths
        backupPaths.clear()
        size = int(formData.get("backupUrlSize"))
        for i in range (1, size+2):  
            keyName = "backupPaths_%s_name" % i
            #valueName = "backupPaths_%s_label" % i
            valueName = formData.get("default")
            name = formData.get(keyName)
            #value = formData.get(valueName)
            print " * setting.py Updatebackup Path: name='%s', valueName='%s', count='%s'" % (name, valueName, i) 
            if name==valueName:
                backupPaths.put(name, "default")
            elif valueName=="on" and i==size+1: #this will be the newest added path
                backupPaths.put(name, "default")
            elif name is not None:
                backupPaths.put(name, "")
#            if name is not None and value is not None:
#                backupPaths.put(name, value)
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

scriptObject = SettingsData()
