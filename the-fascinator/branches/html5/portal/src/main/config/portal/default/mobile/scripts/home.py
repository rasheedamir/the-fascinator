from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.common import JsonConfigHelper
from java.io import ByteArrayInputStream, ByteArrayOutputStream
from java.util import HashMap

class HomeData:
    def __init__(self):
        action = formData.get("verb")
        portalName = formData.get("value")
        sessionState.remove("fq")
        if action == "delete-portal":
            print " * home.py: delete portal %s" % portalName
            Services.portalManager.remove(portalName)
        self.__latest = JsonConfigHelper()
        self.__mine = JsonConfigHelper()
        self.__workflows = JsonConfigHelper()
        self.__result = JsonConfigHelper()
        self.__search()
    
    def __search(self):
        indexer = Services.getIndexer()
        portalQuery = Services.getPortalManager().get(portalId).getQuery()
        portalSearchQuery = Services.getPortalManager().get(portalId).getSearchQuery()
        
        # Security prep work
        current_user = page.authentication.get_username()
        security_roles = page.authentication.get_roles_list()
        security_query = 'security_filter:("' + '" OR "'.join(security_roles) + '")'
        owner_query = 'owner:"' + current_user + '"'

        req = SearchRequest("last_modified:[NOW-1MONTH TO *]")
        req.setParam("fq", 'item_type:"object"')
        if portalQuery:
            req.addParam("fq", portalQuery)
        if portalSearchQuery:
            req.addParam("fq", portalSearchQuery)
        req.setParam("rows", "5")
        req.setParam("sort", "last_modified desc, f_dc_title asc");
        if not page.authentication.is_admin():
            req.addParam("fq", "(" + security_query + ") OR (" + owner_query + ")")
        out = ByteArrayOutputStream()
        indexer.search(req, out)
        self.__latest = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
        
        req = SearchRequest(owner_query)
        req.setParam("fq", 'item_type:"object"')
        if portalQuery:
            req.addParam("fq", portalQuery)
        if portalSearchQuery:
            req.addParam("fq", portalSearchQuery)
        req.setParam("rows", "10")
        req.setParam("sort", "last_modified desc, f_dc_title asc");
        if not page.authentication.is_admin():
            req.addParam("fq", "(" + security_query + ") OR (" + owner_query + ")")
        out = ByteArrayOutputStream()
        indexer.search(req, out)
        self.__mine = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))

        req = SearchRequest('workflow_security:"' + current_user + '"')
        req.setParam("fq", 'item_type:"object"')
        if portalQuery:
            req.addParam("fq", portalQuery)
        if portalSearchQuery:
            req.addParam("fq", portalSearchQuery)
        req.setParam("rows", "10")
        req.setParam("sort", "last_modified desc, f_dc_title asc");
        if not page.authentication.is_admin():
            req.addParam("fq", "(" + security_query + ") OR (" + owner_query + ")")
        out = ByteArrayOutputStream()
        indexer.search(req, out)
        self.__workflows = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))

        req = SearchRequest("*:*")
        req.setParam("fq", 'item_type:"object"')
        if portalQuery:
            req.addParam("fq", portalQuery)
        if portalSearchQuery:
            req.addParam("fq", portalSearchQuery)
        req.addParam("fq", "")
        req.setParam("rows", "5")
        if not page.authentication.is_admin():
            req.addParam("fq", "(" + security_query + ") OR (" + owner_query + ")")
        out = ByteArrayOutputStream()
        indexer.search(req, out)
        
        sessionState.set("fq", 'item_type:"object"')
        #sessionState.set("query", portalQuery.replace("\"", "'"))
        
        self.__result = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
    
    def getLatest(self):
        return self.__latest.getList("response/docs")
    
    def getMine(self):
        return self.__mine.getList("response/docs")

    def getWorkflows(self):
        return self.__workflows.getList("response/docs")

    def getItemCount(self):
        return self.__result.get("response/numFound")
    
    def getResult(self):
        return self.__result.getList("response/docs")

scriptObject = HomeData()
