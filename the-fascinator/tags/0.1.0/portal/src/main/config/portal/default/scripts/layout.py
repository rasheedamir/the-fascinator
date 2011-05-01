# 
# Script for Template layout
# 

from java.net import URLEncoder

class TemplateData:
    
    def __init__(self):
        self.__checkLogin()
        if formData.get("action") == "clear-session":
            sessionState.clear()

    def __checkLogin(self):
        action = formData.get("action")
        if (action == "logout"):
            sessionState.set("username", None)
        else:
            username = formData.get("username")
            if username is not None:
                #TODO actual login procedure
                sessionState.set("username", username)

    def getPortals(self):
        return Services.getPortalManager().getPortals()

    def encodeURL(self, url):
        return URLEncoder.encode(url, "UTF-8")
    
    def getTemplate(self, templateName):
        portalName = portalId
        if not Services.pageService.resourceExists(portalId, templateName, False):
            portalName = Services.portalManager.DEFAULT_PORTAL_NAME
        return "%s/%s" % (portalName, templateName)

scriptObject = TemplateData()
