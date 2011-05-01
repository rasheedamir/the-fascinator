import array, md5, os

from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.api.storage import Payload, PayloadType
from au.edu.usq.fascinator.api import PluginManager
from au.edu.usq.fascinator.common import JsonConfig, JsonConfigHelper
from au.edu.usq.fascinator.common.storage.impl import GenericPayload
from au.edu.usq.fascinator.portal import Pagination, Portal

from java.io import ByteArrayInputStream, ByteArrayOutputStream
from java.net import URLDecoder, URLEncoder
from java.util import LinkedHashMap

class SearchData:
    def __init__(self):
        self.__portal = Services.portalManager.get(portalId)
        self.__result = JsonConfigHelper()
        self.__pageNum = sessionState.get("pageNum", 1)
        self.__selected = []
        self.__search()
        
    def getPortalName(self):
        return Services.portalManager.get(portalId).description
    
    def __search(self):
        recordsPerPage = self.__portal.recordsPerPage
        
        uri = URLDecoder.decode(request.getAttribute("RequestURI"))
        if uri != portalPath:
            query = uri[len(portalPath):]
        if query is None or query == "":
            query = formData.get("query")
        if query is None or query == "":
            query = "*:*"
        
        if query == "*:*":
            self.__query = ""
        else:
            self.__query = query
        sessionState.set("query", self.__query)
        
        req = SearchRequest(query)
        req.setParam("facet", "true")
        req.setParam("rows", str(recordsPerPage))
        req.setParam("facet.field", self.__portal.facetFieldList)
        req.setParam("facet.sort", "true")
        req.setParam("facet.limit", str(self.__portal.facetCount))
        req.setParam("sort", "f_dc_title asc")
        
        # setup facets
        action = formData.get("verb")
        value = formData.get("value")
        fq = sessionState.get("fq")
        if fq is not None:
            self.__pageNum = 1
            req.setParam("fq", fq)
        if action == "add_fq":
            self.__pageNum = 1
            name = formData.get("name")
            print " * add_fq: %s" % value
            req.addParam("fq", URLDecoder.decode(value, "UTF-8"))
        elif action == "remove_fq":
            self.__pageNum = 1
            req.removeParam("fq", URLDecoder.decode(value, "UTF-8"))
        elif action == "clear_fq":
            self.__pageNum = 1
            req.removeParam("fq")
        elif action == "select-page":
            self.__pageNum = int(value)
        req.addParam("fq", 'item_type:"object"')
        
        portalQuery = self.__portal.query
        print " * portalQuery=%s" % portalQuery
        if portalQuery:
            req.addParam("fq", portalQuery)
        
        self.__selected = req.getParams("fq")
        
        sessionState.set("fq", self.__selected)
        sessionState.set("pageNum", self.__pageNum)
        
        req.setParam("start", str((self.__pageNum - 1) * recordsPerPage))
        
        print " * search.py:", req.toString(), self.__pageNum
        
        out = ByteArrayOutputStream()
        Services.indexer.search(req, out)
        self.__result = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
        if self.__result is not None:
            self.__paging = Pagination(self.__pageNum,
                                       int(self.__result.get("response/numFound")),
                                       self.__portal.recordsPerPage)
    
    def getQueryTime(self):
        return int(self.__result.get("responseHeader/QTime")) / 1000.0;
    
    def getPaging(self):
        return self.__paging
    
    def getResult(self):
        return self.__result
    
    def getFacetField(self, key):
        return self.__portal.facetFields.get(key)
    
    def getFacetName(self, key):
        return self.__portal.facetFields.get(key).get("label")
    
    def getFacetCounts(self, key):
        values = LinkedHashMap()
        valueList = self.__result.getList("facet_counts/facet_fields/%s" % key)
        for i in range(0,len(valueList),2):
            name = valueList[i]
            count = valueList[i+1]
            if count > 0:
                values.put(name, count)
        return values
    
    def hasSelectedFacets(self):
        return (self.__selected is not None and len(self.__selected) > 1) and \
            not (self.__portal.query in self.__selected and len(self.__selected) == 2)
    
    def getSelectedFacets(self):
        return self.__selected
    
    def isPortalQueryFacet(self, fq):
        return fq == self.__portal.query
    
    def isSelected(self, fq):
        return fq in self.__selected
    
    def getSelectedFacetIds(self):
        return [md5.new(fq).hexdigest() for fq in self.__selected]
    
    def getFileName(self, path):
        return os.path.split(path)[1]
    
    def getFacetQuery(self, name, value):
        return '%s:"%s"' % (name, value)
    
    def isImage(self, format):
        return format.startswith("image/")
    
    def getThumbnail(self, oid):
        ext = os.path.splitext(oid)[1]
        url = oid[oid.rfind("/")+1:-len(ext)] + ".thumb.jpg"
        if Services.getStorage().getPayload(oid, url):
            return url
        return None

scriptObject = SearchData()
