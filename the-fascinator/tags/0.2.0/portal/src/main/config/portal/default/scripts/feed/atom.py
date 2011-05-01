from au.edu.usq.fascinator.common import JsonConfigHelper
from au.edu.usq.fascinator.api.indexer import SearchRequest
from java.io import ByteArrayInputStream, ByteArrayOutputStream
import os

class FeedData:
    def __init__(self):
        self.__feed()
        
    def __feed(self):
        portal = Services.getPortalManager().get(portalId)
        recordsPerPage = portal.recordsPerPage
        pageNum = sessionState.get("pageNum", 1)
        
        query = "*:*"
        
        req = SearchRequest(query)
        req.setParam("facet", "true")
        req.setParam("rows", str(recordsPerPage))
        req.setParam("facet.field", portal.facetFieldList)
        req.setParam("facet.sort", "true")
        req.setParam("facet.limit", str(portal.facetCount))
        req.setParam("sort", "f_dc_title asc")
        
        portalQuery = portal.query
        if portalQuery:
            req.addParam("fq", portalQuery)
        else:
            fq = sessionState.get("fq")
            req.setParam("fq", fq)
        
        req.setParam("start", str((pageNum - 1) * recordsPerPage))
        
        print " * query: ", query
        print " * portalQuery='%s'" % portalQuery
        print " * feed.py:", req.toString()
        
        out = ByteArrayOutputStream()
        Services.indexer.search(req, out)
        self.__result = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
        
    def cleanUp(self, value):
        return value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;")

    def hasResults(self): 
        return self.__result is not None
        
    def getResult(self):
        return self.__result 
    
    def getFileName(self, path):
        return os.path.split(path)[1]
        
        
        
        

scriptObject = FeedData()
