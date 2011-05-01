from au.edu.usq.fascinator.api import PluginManager

class ContentData:
    
    def __init__(self):
        self.__contentManager = Services.contentManager
    
    def getContents(self):
        return self.__contentManager.getContents()
    
    def getHarvesters(self):
        return PluginManager.getHarvesterPlugins()
    
    def getHarvester(self, type):
        return PluginManager.getHarvester(type)

scriptObject = ContentData()
