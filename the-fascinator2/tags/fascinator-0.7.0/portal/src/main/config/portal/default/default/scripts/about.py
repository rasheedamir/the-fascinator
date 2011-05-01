from au.edu.usq.fascinator.api import PluginManager
from au.edu.usq.fascinator.common import JsonConfigHelper

from org.apache.commons.io import IOUtils

from java.io import StringWriter

class AboutData:
    def __activate__(self, context):
        self.pageService = context["Services"].pageService
    
    def hasMetadata(self, plugin):
        return plugin.pluginDetails.metadata is not None
    
    def getAccessControlPlugins(self):
        return PluginManager.getAccessControlPlugins()
    
    def getAuthenticationPlugins(self):
        return PluginManager.getAuthenticationPlugins()
    
    def getHarvesterPlugins(self):
        return PluginManager.getHarvesterPlugins()
    
    def getIndexerPlugins(self):
        return PluginManager.getIndexerPlugins()
    
    def getStoragePlugins(self):
        return PluginManager.getStoragePlugins()
    
    def getSubscriberPlugins(self):
        return PluginManager.getSubscriberPlugins()
    
    def getRolesPlugins(self):
        return PluginManager.getRolesPlugins()
    
    def getTransformerPlugins(self):
        return PluginManager.getTransformerPlugins()
    
    def getMetadata(self, plugin, field):
        metadata = plugin.pluginDetails.metadata
        if metadata:
            json = JsonConfigHelper(plugin.pluginDetails.metadata)
            return json.get(field)
        return None
    
    def getAbout(self):
        return self.getResourceContent()
    
    def getResourcePath(self, plugin, field):
        return self.getMetadata(plugin, field)
    
    def getResourceContent(self, plugin, field):
        resource = self.getMetadata(plugin, field)
        stream = self.pageService.getResource(resource)
        if stream:
            writer = StringWriter()
            IOUtils.copy(stream, writer, "UTF-8")
            html = writer.toString()
            print " *** html:", html
            return html
        return "<em>'%s' not found!</em>" % (field)
    

