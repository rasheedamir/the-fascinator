from au.edu.usq.fascinator.api import PluginManager

class SettingsData:

    def __init__(self):
        pass

    def getIndexerPlugins(self):
        return PluginManager.getIndexerPlugins()

    def getStoragePlugins(self):
        return PluginManager.getStoragePlugins()

    def getHarvesterPlugins(self):
        return PluginManager.getHarvesterPlugins()

    def getTransformerPlugins(self):
        return PluginManager.getTransformerPlugins()

scriptObject = SettingsData()
