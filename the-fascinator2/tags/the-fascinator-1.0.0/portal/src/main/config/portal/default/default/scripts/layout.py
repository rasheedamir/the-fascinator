import md5
from authentication import AuthenticationData
from java.net import URLDecoder
from org.apache.commons.lang import StringEscapeUtils

class LayoutData:
    def __init__(self):
        pass
    
    def __activate__(self, context):
        self.sessionState = context["sessionState"]
        self.services = context["Services"]
        self.security = context["security"]
        self.request = context["request"]
        self.portalId = context["portalId"]
        uri = URLDecoder.decode(self.request.getAttribute("RequestURI"))
        self.__relPath = "/".join(uri.split("/")[1:])
        self.authentication = AuthenticationData()
        self.authentication.__activate__(context)

        #self.formData = context["formData"]
        #if self.formData is not None:
        #    for field in self.formData.getFormFields():
        #        log.debug("Form Data: '{}' => '{}'", field, self.formData.get(field))
        #if self.sessionState is not None:
        #    for field in self.sessionState.keySet():
        #        log.debug("Session Data: '{}' => '{}'", field, self.sessionState.get(field))
        #log.debug("PATH: '{}'", self.request.getPath())
        #for param in self.request.getParameterNames():
        #    log.debug("PARAM: '{}' : '{}'", param, self.request.getParameter(param));

    def getRelativePath(self):
        return self.__relPath
    
    def getPortal(self):
        return self.services.getPortalManager().get(self.portalId)
    
    def getPortals(self):
        return self.services.getPortalManager().portals
    
    def getPortalName(self):
        return self.getPortal().getDescription()

    def getQuery(self):
        query = self.sessionState.get("query")
        if query is None:
            return ""
        else:
            return self.escapeHtml(query)

    def escapeXml(self, text):
        return StringEscapeUtils.escapeXml(text)
    
    def escapeHtml(self, text):
        return StringEscapeUtils.escapeHtml(text)
    
    def unescapeHtml(self, text):
        return StringEscapeUtils.unescapeHtml(text)
    
    def md5Hash(self, data):
        return md5.new(data).hexdigest()
    
    def capitalise(self, text):
        return text[0].upper() + text[1:]
    
    def getTemplate(self, templateName):
        return self.services.velocityService.resourceExists(self.portalId, templateName)
    
    def getQueueStats(self):
        return self.services.getHouseKeepingManager().getQueueStats()
    
    def getSsoProviders(self):
        return self.security.ssoBuildLogonInterface(self.sessionState)
