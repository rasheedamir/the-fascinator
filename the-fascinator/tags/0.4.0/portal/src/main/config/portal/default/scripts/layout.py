import md5

from authentication import Authentication

from java.net import URLEncoder
from org.apache.commons.lang import StringEscapeUtils

class LayoutData:

    def __init__(self):
        self.authentication = Authentication();
        self.authentication.session_init();

    def getPortal(self):
        return Services.getPortalManager().get(portalId)

    def getPortals(self):
        return Services.getPortalManager().portals

    def getPortalName(self):
        return self.getPortal().getDescription()

    def escapeText(self, text):
        return StringEscapeUtils.escapeXml(text)

    def md5Hash(self, data):
        return md5.new(data).hexdigest()

    def capitalise(self, text):
        return text[0].upper() + text[1:]

    def getTemplate(self, templateName):
        portalName = portalId
        if not Services.pageService.resourceExists(portalId, templateName, False):
            portalName = Services.portalManager.DEFAULT_PORTAL_NAME
        return "%s/%s" % (portalName, templateName)

scriptObject = LayoutData()
