import random, time
from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.common import JsonSimpleConfig
from au.edu.usq.fascinator.common.solr import SolrResult
from java.io import ByteArrayInputStream, ByteArrayOutputStream
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
    def __init__(self, formData, currentToken, sessionState):
        self.__error = None
        self.__verb = formData.get("verb")
        self.__metadataFormats = self.__metadataFormatList()
        print " * verb=%s" % self.__verb

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
            elif self.__metadataPrefix not in self.__metadataFormatList():
                self.__error = OaiPmhError("cannotDisseminateFormat",
                                           "Record not available as metadata type: %s" % self.__metadataPrefix)
        elif self.__verb in ["Identify", "ListMetadataFormats", "ListSets"]:
            pass
        else:
            self.__error = OaiPmhError("badVerb", "Unknown verb: '%s'" % self.__verb)

    def __metadataFormatList(self):
        systemConfig = JsonSimpleConfig()
        metadataFormats = systemConfig.getObject(["portal", "oai-pmh", "metadataFormats"])
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
        self.systemConfig = JsonSimpleConfig()

        self.velocityContext = context
        self.services = context["Services"]
        self.log = context["log"]
        self.sessionState = context["sessionState"]
        self.portalDir = context["portalDir"]
        self.__result = None
        self.__token = None
        self.__portalName = context["page"].getPortal().getName()
        self.__enabledInAllViews = False
        self.__enabledInViews = []
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

        print " * oai.py: formData=%s" % self.vc("formData")
        self.vc("request").setAttribute("Content-Type", "text/xml")
        self.__request = OaiPmhVerb(self.vc("formData"), self.__currentToken, self.sessionState)

        if self.getError() is None and \
                self.getVerb() in ["GetRecord", "ListIdentifiers", "ListRecords"]:

            ## Only list those data if the metadata format is enabled
            self.__metadataPrefix = self.vc("formData").get("metadataPrefix")
            if self.__metadataPrefix is None:
                self.__metadataPrefix = self.__currentToken.getMetadataPrefix()

            self.__enabledInAllViews = self.systemConfig.getBoolean(False, ["portal", "oai-pmh", "metadataFormats", self.__metadataPrefix, "enabledInAllViews"])
            if self.__enabledInAllViews:
                self.__search()
            else:
                self.__enabledInViews = self.systemConfig.getStringList(["portal", "oai-pmh", "metadataFormats", self.__metadataPrefix, "enabledViews"])
                if self.__portalName in self.__enabledInViews:
                    self.__search()

    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            self.log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None

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
        self.__result = SolrResult(None)

        portal = self.services.getPortalManager().get(self.vc("portalId"))
        recordsPerPage = portal.recordsPerPage

        query = self.vc("formData").get("query")
        if query is None or query == "":
            query = "*:*"
        req = SearchRequest(query)
        req.setParam("facet", "true")
        req.setParam("rows", str(recordsPerPage))
        req.setParam("facet.field", portal.facetFieldList)
        req.setParam("facet.limit", str(portal.facetCount))
        req.setParam("sort", "f_dc_title asc")

        portalQuery = portal.query
        print " * portalQuery=%s" % portalQuery
        if portalQuery:
            req.addParam("fq", portalQuery)
        req.addParam("fq", "item_type:object")
        
        #Check if there's resumption token exist in the formData
        if self.__currentToken:
            start = self.__currentToken.getStart()
            totalFound = self.__currentToken.getTotalFound()
            nextTokenStart = start+recordsPerPage
            if nextTokenStart < totalFound:
                self.__token = ResumptionToken(start=nextTokenStart, metadataPrefix=self.__metadataPrefix, sessionExpiry=self.__sessionExpiry)
        else:
            start = 0
            metadataPrefix = self.vc("formData").get("metadataPrefix")
            self.__token = ResumptionToken(start=recordsPerPage, metadataPrefix=self.__metadataPrefix, sessionExpiry=self.__sessionExpiry)

        req.setParam("start", str(start))

        print " * oai.py:", req.toString()

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
        if self.__enabledInAllViews or self.__portalName in self.__enabledInViews:
            return self.__token
        return None

    def getMetadataFormats(self):
        return self.systemConfig.getJsonSimpleMap(["portal", "oai-pmh", "metadataFormats"])

    def encodeXml(self, string):
        return StringEscapeUtils.escapeXml(string);
