import md5, os, random, time
from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.common import JsonConfigHelper
from au.edu.usq.fascinator.portal import Pagination
from java.io import ByteArrayInputStream, ByteArrayOutputStream
from java.net import URLDecoder
from java.util import HashMap

class SolrDoc:
    def __init__(self, json):
        self.json = json
    
    def getField(self, name):
        return self.json.getList("response/docs/%s" % name).get(0)
    
    def getFieldText(self, name):
        return self.json.get("response/docs/%s" % name)
    
    def getFieldList(self, name):
        return self.json.getList("response/docs/%s" % name)
    
    def getDublinCore(self):
        dc = self.json.getList("response/docs").get(0)
        remove = ["dc_title", "dc_description"]
        for entry in dc:
            if not entry.startswith("dc_"):
                remove.append(entry)
        for key in remove:
            dc.remove(key)
        return JsonConfigHelper(dc).getMap("/")
    
    def toString(self):
        return self.json.toString()

class OaiPmhError:
    
    def __init__(self, code, message):
        self.__code = code
        self.__message = message
    
    def getCode(self):
        return self.__code
    
    def getMessage(self):
        return self.__message

class OaiPmhVerb:
    def __init__(self, formData):
        self.__error = None
        self.__verb = formData.get("verb")
        print " * verb=%s" % self.__verb
        if self.__verb is None:
            self.__error = OaiPmhError("badVerb", "No verb was specified")
        elif self.__verb in ["GetRecord", "ListIdentifiers", "ListRecords"]:
            self.__metadataPrefix = formData.get("metadataPrefix")
            if self.__metadataPrefix is None:
                self.__error = OaiPmhError("badArgument", "Missing required argument: metadataPrefix")
            elif self.__metadataPrefix != "oai_dc":
                self.__error = OaiPmhError("cannotDisseminateFormat",
                                           "Record not available as metadata type: %s" % self.__metadataPrefix)
        elif self.__verb in ["Identify", "ListMetadataFormats", "ListSets"]:
            pass
        else:
            self.__error = OaiPmhError("badVerb", "Unknown verb: '%s'" % self.__verb)
    
    def getError(self):
        return self.__error
    
    def getVerb(self):
        return self.__verb
    
    def getMetadataPrefix(self):
        return self.__metadataPrefix
    
    def getIdentifier(self):
        return self.__identifier

class ResumptionToken:
    def __init__(self, start):
        random.seed()
        self.__token = "%016x" % random.getrandbits(128)
        self.__start = start;
    
    def getToken(self):
        return self.__token
    
    def getStart(self):
        return self.__start

class SearchData:
    def __init__(self):
        print " * oai.py: formData=%s" % formData
        request.setAttribute("Content-Type", "text/xml")
        self.__request = OaiPmhVerb(formData)
        if self.getError() is None and \
                self.getVerb() in ["GetRecord", "ListIdentifiers", "ListRecords"]:
            self.__search()
    
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
    
    def __search(self):
        self.__result = JsonConfigHelper()
        
        portal = Services.getPortalManager().get(portalId)
        recordsPerPage = portal.recordsPerPage
        
        query = formData.get("query")
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
        
        # TODO resumptionToken
        #req.setParam("start", str((self.__pageNum - 1) * recordsPerPage))
        
        print " * oai.py:", req.toString()
        
        out = ByteArrayOutputStream()
        Services.indexer.search(req, out)
        self.__result = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))

scriptObject = SearchData()
