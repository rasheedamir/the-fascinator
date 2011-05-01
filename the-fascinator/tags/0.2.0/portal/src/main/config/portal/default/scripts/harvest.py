from au.edu.usq.fascinator.api import PluginManager

class ContentData:
    
    def __init__(self):
        self.__harvestManager = Services.harvestManager
    
    def getContents(self):
        return self.__harvestManager.contents
    
    def getHarvesters(self):
        return PluginManager.harvesterPlugins
    
    def getHarvester(self, type):
        return PluginManager.getHarvester(type)

scriptObject = ContentData()
