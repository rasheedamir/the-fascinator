import random, time

from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common import JsonSimpleConfig
from au.edu.usq.fascinator.common.solr import SolrResult

from java.io import BufferedReader
from java.io import ByteArrayInputStream
from java.io import ByteArrayOutputStream
from java.io import InputStreamReader
from java.lang import Exception
from java.lang import StringBuilder
from java.lang import System

from org.apache.commons.lang import StringEscapeUtils

class OaiPmhError:
    def __init__(self, code, message):
        self.__code = code
        self.__message = message

    def getCode(self):
        return self.__code

    def getMessage(self):
        return self.__message

class OaiPmhVerb:
    def __init__(self, formData, currentToken, sessionState, context):
        self.log = context["log"]
        self.config = JsonSimpleConfig()

        self.__error = None
        self.__verb = formData.get("verb")
        self.__metadataFormats = self.__metadataFormatList()
        self.log.debug(" * OAI Verb = '{}'", self.__verb)

        if self.__verb is None:
            self.__error = OaiPmhError("badVerb", "No verb was specified")

        elif self.__verb in ["GetRecord", "ListIdentifiers", "ListRecords"]:
            self.__metadataPrefix = formData.get("metadataPrefix")
            if self.__metadataPrefix is None:
                if currentToken:
                    #check expiry
                    if currentToken.getExpiry() > System.currentTimeMillis():
                        self.__metadataPrefix = currentToken.getMetadataPrefix()
                    else:
                        self.__error = OaiPmhError("badResumptionToken", "Token has expired")
                        tokenList = sessionState.get("resumptionTokenList")
                        tokenList.pop(currentToken.getToken())
                        sessionState.set("resumptionTokenList", tokenList)
                else:
                    self.__error = OaiPmhError("badResumptionToken", "Invalid token")
            elif self.__metadataPrefix not in self.__metadataFormats:
                self.__error = OaiPmhError("cannotDisseminateFormat",
                                           "Record not available as metadata type: %s" % self.__metadataPrefix)
        elif self.__verb in ["Identify", "ListMetadataFormats", "ListSets"]:
            pass
        else:
            self.__error = OaiPmhError("badVerb", "Unknown verb: '%s'" % self.__verb)

    def __metadataFormatList(self):
        metadataFormats = self.config.getObject(["portal", "oai-pmh", "metadataFormats"])
        metadataList = []
        for format in metadataFormats.keySet():
            metadataList.append(str(format))
        return metadataList

    def getError(self):
        return self.__error

    def getVerb(self):
        return self.__verb

    def getMetadataPrefix(self):
        return self.__metadataPrefix

    def getIdentifier(self):
        return self.__identifier

class ResumptionToken:
    def __init__(self, token=None, start=0, metadataPrefix="", sessionExpiry=300000):
        if token is None:
            random.seed()
            token = "%016x" % random.getrandbits(128)
        self.__token = token
        self.__start = start
        self.__metadataPrefix = metadataPrefix
        self.__totalFound = 0
        self.__expiry = System.currentTimeMillis() + sessionExpiry

    def getToken(self):
        return self.__token

    def getExpiry(self):
        return self.__expiry

    def setTotalFound(self, totalFound):
        self.__totalFound = totalFound

    def getTotalFound(self):
        return self.__totalFound

    def getMetadataPrefix(self):
        return self.__metadataPrefix

    def getStart(self):
        return self.__start

class OaiData:
    def __init__(self):
        pass

    def __activate__(self, context):
        # Set up configuration
        self.systemConfig = JsonSimpleConfig()
        self.oaiConfig = None
        self.getMetadataFormats()

        self.velocityContext = context
        self.services = context["Services"]
        self.log = context["log"]
        self.sessionState = context["sessionState"]
        self.portalDir = context["portalDir"]

        self.__result = None
        self.__token = None

        # Check if the OAI request has an overriding portal ('set') to the URL
        paramSet = self.vc("formData").get("set")
        self.__portalName = context["page"].getPortal().getName()
        if paramSet is not None:
            portals = self.vc("page").getPortals().keySet()
            if portals.contains(paramSet):
                self.__portalName = paramSet
                self.log.debug("=== PORTAL override! : {}", self.__portalName);

        self.__metadataPrefix = ""
        self.__sessionExpiry = self.systemConfig.getInteger(None, ["portal", "oai-pmh", "sessionExpiry"])

        self.__resumptionTokenList = self.sessionState.get("resumptionTokenList")
        if self.__resumptionTokenList == None:
            self.__resumptionTokenList = {}
        #Check if there's resumption token exist in the formData
        self.__currentToken = None

        resumptionToken = self.vc("formData").get("resumptionToken")
        if resumptionToken:
            if self.__resumptionTokenList.has_key(resumptionToken):
                self.__currentToken = self.__resumptionTokenList[resumptionToken]

        self.vc("request").setAttribute("Content-Type", "text/xml")
        self.__request = OaiPmhVerb(self.vc("formData"), self.__currentToken, self.sessionState, context)

        if self.getError() is None and \
                self.getVerb() in ["GetRecord", "ListIdentifiers", "ListRecords"]:

            ## Only list those data if the metadata format is enabled
            self.__metadataPrefix = self.vc("formData").get("metadataPrefix")
            if self.__metadataPrefix is None:
                self.__metadataPrefix = self.__currentToken.getMetadataPrefix()

            if self.isInView(self.__metadataPrefix):
                self.__search()

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            self.log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

    def isInView(self, format, view = None):
        # Sanity check
        if format is None or format == "":
            return False
        # Default to current poral
        if view is None:
            view = self.__portalName

        # Make sure there is some config for this format
        formatConfig = self.getMetadataFormats().get(format)
        if formatConfig is None:
            return False
        # Is it visible everywhere?
        allViews = formatConfig.getBoolean(False, ["enabledInAllViews"])
        if allViews:
            self.log.debug("=== Format '{}' is in all views", format)
            return True
        # Check if it is visible in this view
        else:
            allowedViews = formatConfig.getStringList(["enabledViews"])
            if view in allowedViews:
                self.log.debug("=== Format '{}' is in view '{}'", format, view)
                return True
        # Rejection
        self.log.debug("=== Format '{}' is NOT in view '{}'", format, view)
        return False

    def getID(self, item):
        identifier = item.getFirst("oai_identifier")
        # Fallback to the default
        if identifier is None or identifier == "":
            return "oai:fascinator:" + item.getFirst("id")
        # Use the indexed value
        return identifier

    def getSet(self, item):
        set = item.getFirst("oai_set")
        # Fallback to the portal name
        if set is None or set == "":
            return self.__portalName
        # Use the required set
        return set

    def getVerb(self):
        return self.getRequest().getVerb()

    def getError(self):
        return self.getRequest().getError()

    def getResponseDate(self):
        return time.strftime("%Y-%m-%dT%H:%M:%SZ")

    def getRequest(self):
        return self.__request

    def getResult(self):
        return self.__result

    def getElement(self, elementName, values):
        elementStr = ""
        if values:
            for value in values:
                elementStr += "<%s>%s</%s>" % (elementName, value, elementName)
        return elementStr

    def __search(self):
        self.log.debug(" === __search()")
        self.__result = SolrResult(None)

        portal = self.services.getPortalManager().get(self.__portalName)
        recordsPerPage = portal.recordsPerPage

        # Resolve our identifier
        id = self.vc("formData").get("identifier")
        self.log.debug(" === ID: '{}'", id)
        query = "*:*"
        if id is not None and id != "":
            # A default TF2 OID
            if id.startswith("oai:fascinator:"):
                query = "id:" + id.replace("oai:fascinator:", "")
            # Or a custom OAI ID
            else:
                query = "oai_identifier:" + id.replace(":", "\\:")

        self.log.debug(" === QUERY: '{}'", query)
        req = SearchRequest(query)
        req.setParam("facet", "true")
        req.setParam("rows", str(recordsPerPage))
        req.setParam("facet.field", portal.facetFieldList)
        req.setParam("facet.limit", str(portal.facetCount))
        req.setParam("sort", "f_dc_title asc")

        portalQuery = portal.query
        self.log.debug(" * portalQuery={}", portalQuery)
        if portalQuery:
            req.addParam("fq", portalQuery)
        req.addParam("fq", "item_type:object")

        # Check if there's resumption token exist in the formData
        if self.__currentToken:
            start = self.__currentToken.getStart()
            totalFound = self.__currentToken.getTotalFound()
            nextTokenStart = start + recordsPerPage
            if nextTokenStart < totalFound:
                self.__token = ResumptionToken(start = nextTokenStart, \
                    metadataPrefix = self.__metadataPrefix, \
                    sessionExpiry = self.__sessionExpiry)
        else:
            start = 0
            metadataPrefix = self.vc("formData").get("metadataPrefix")
            self.__token = ResumptionToken(start = recordsPerPage, \
                metadataPrefix = self.__metadataPrefix, \
                sessionExpiry = self.__sessionExpiry)

        req.setParam("start", str(start))

        self.log.debug(" * oai.py:", req.toString())

        out = ByteArrayOutputStream()
        self.services.indexer.search(req, out)
        self.__result = SolrResult(ByteArrayInputStream(out.toByteArray()))

        totalFound = self.__result.getNumFound()
        if totalFound == 0:
            self.__token = None
        elif self.__token:
            if self.__token.getStart() < totalFound:
                self.__token.setTotalFound(totalFound)
            else:
                self.__token = None

        #Storing the resumptionToken to session
        if self.__token:
            self.__resumptionTokenList[self.__token.getToken()] = self.__token #(totalFound, self.__token.getConstructedToken())
            #Need to know how long the server need to store this token
            self.sessionState.set("resumptionTokenList", self.__resumptionTokenList)

    def getToken(self):
        if self.isInView(self.__metadataPrefix):
            return self.__token
        return None

    def getMetadataFormats(self):
        if self.oaiConfig is None:
            self.oaiConfig = self.systemConfig.getJsonSimpleMap(["portal", "oai-pmh", "metadataFormats"])
        return self.oaiConfig

    def encodeXml(self, string):
        return StringEscapeUtils.escapeXml(string);

    def getPayload(self, oid, metadataFileName):
        # First get the Object from storage
        object = None
        try:
            object = self.services.getStorage().getObject(oid)
        except StorageException, e:
            return None

        # Check whether the payload exists
        try:
            return object.getPayload(metadataFileName)
        except StorageException, e:
            return None

    def getPayloadContent(self, payload):
        if payload is None:
            return ""

        try:
            sb = StringBuilder()
            reader = BufferedReader(InputStreamReader(payload.open(), "UTF-8"))
            line = reader.readLine()

            while line is not None:
                sb.append(line).append("\n")
                line = reader.readLine()
            payload.close()

            if sb:
                return sb
            return ""

        except Exception, e:
            return ""
