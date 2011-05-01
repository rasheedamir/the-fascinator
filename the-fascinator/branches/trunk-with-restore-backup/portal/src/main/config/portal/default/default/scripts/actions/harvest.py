from au.edu.usq.fascinator.api import PluginManager

class HarvestData:
    def __init__(self):
        self.__harvestManager = Services.getHarvestManager()
        result = "{}"
        func = formData.get("func")
        if func == "get-harvester-config":
            result = self.getHarvester(formData.get("type")).getConfig()
            writer = response.getPrintWriter("text/html; charset=UTF-8")
            writer.println(result)
            writer.close()
        else:
            writer = response.getPrintWriter("text/plain; charset=UTF-8")
            writer.println(result)
            writer.close()
    
    def getContents(self):
        return self.__harvestManager.getContents()
    
    def getHarvesters(self):
        return PluginManager.getHarvesterPlugins()
    
    def getHarvester(self, type):
        return PluginManager.getHarvester(type)

scriptObject = HarvestData()
