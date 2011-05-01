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
        if action == "backup-portal":
            print "Backing up: ", portalName
            backupPath = ""
            email = ""
            portal = Services.portalManager.get(portalName)
            if portal:
                email = portal.email
                if portal.backupPaths:
                    for key in portal.backupPaths:
                        if portal.backupPaths[key]=="default":
                            backupPath = key
                portalQuery = portal.getQuery()
                #print " ***** portalQuery: ", portalQuery
                #print " ***** backupPath: ", backupPath
                #print " ***** email: ", email
                #print " ***** description: ", description
            if backupPath is None:
                " ** Default backup path configured in system-config.json will be used "
            Services.portalManager.backup(email, backupPath, portalQuery)
        self.__latest = JsonConfigHelper()
        self.__result = JsonConfigHelper()
        self.__search()
    
    def __search(self):
        indexer = Services.getIndexer()
        portalQuery = Services.getPortalManager().get(portalId).getQuery()
        
        req = SearchRequest("last_modified:[NOW-1MONTH TO *]")
        req.setParam("fq", 'item_type:"object"')
        if portalQuery:
            req.addParam("fq", portalQuery)
        req.setParam("rows", "10")
        req.setParam("sort", "last_modified desc, f_dc_title asc");
        out = ByteArrayOutputStream()
        indexer.search(req, out)
        self.__latest = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
        
        req = SearchRequest("*:*")
        req.setParam("fq", 'item_type:"object"')
        if portalQuery:
            req.addParam("fq", portalQuery)
        req.addParam("fq", "")
        req.setParam("rows", "0")
        out = ByteArrayOutputStream()
        indexer.search(req, out)
        sessionState.set("fq", 'item_type:"object"')
        sessionState.set("query", portalQuery.replace("\"", "'"))
        
        self.__result = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
    
    def getLatest(self):
        return self.__latest.getList("response/docs")
    
    def getItemCount(self):
        return self.__result.get("response/numFound")

scriptObject = HomeData()
