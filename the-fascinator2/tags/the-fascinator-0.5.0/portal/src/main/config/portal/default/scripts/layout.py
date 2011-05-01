import md5

from authentication import Authentication

from au.edu.usq.fascinator.common import JsonConfig
from java.net import URLEncoder
from org.apache.commons.lang import StringEscapeUtils

class LayoutData:

    def __init__(self):
        self.authentication = Authentication();
        self.authentication.session_init();
        self.config = JsonConfig()

    def getPortal(self):
        return Services.getPortalManager().get(portalId)

    def getPortals(self):
        return Services.getPortalManager().portals

    def getPortalName(self):
        return self.getPortal().getDescription()

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
        portalName = portalId
        if not Services.pageService.resourceExists(portalId, templateName, False):
            portalName = Services.portalManager.DEFAULT_PORTAL_NAME
        return "%s/%s" % (portalName, templateName)

    def isConfigured(self):
        return self.config.isConfigured()

    def isNotConfigured(self):
        return not self.config.isConfigured()

    def isOutdated(self):
        return self.config.isOutdated()

    def needRestart(self):
        return "true" == sessionState.get("need-restart", "false")

scriptObject = LayoutData()
