import os.path
import array, md5, os

from au.edu.usq.fascinator.api import PluginManager
from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.api.storage import Payload, PayloadType
from au.edu.usq.fascinator.common import JsonConfig, JsonConfigHelper
from au.edu.usq.fascinator.common.storage.impl import GenericPayload
from au.edu.usq.fascinator.portal import Pagination, Portal

from java.io import ByteArrayInputStream, ByteArrayOutputStream, File
from java.net import URLDecoder, URLEncoder
from java.util import ArrayList, HashMap, LinkedHashMap, HashSet
from java.lang import Exception

class SearchData:
    def __init__(self):
        self.__portal = Services.portalManager.get(portalId)
        self.__result = JsonConfigHelper()
        self.__pageNum = sessionState.get("pageNum", 1)
        self.__selected = []
        self.__search()
        
    def getPortalName(self):
        return self.__portal.getDescription()
        
    def encode(self, url):
        return URLEncoder.encode(url, "UTF-8")
        
    def __search(self):
        recordsPerPage = self.__portal.recordsPerPage
        
        uri = URLDecoder.decode(request.getAttribute("RequestURI"))
        query = None
        pagePath = portalId + "/" + pageName
        if uri != pagePath:
            query = uri[len(pagePath)+1:]
        if query is None or query == "":
            query = formData.get("query")
        if query is None or query == "":
            query = "*:*"
        
        if query == "*:*":
            self.__query = ""
        else:
            self.__query = query
        sessionState.set("query", self.__query)
        
        # find objects with annotations matching the query
        if query != "*:*":
            anotarQuery = self.__query
            annoReq = SearchRequest(anotarQuery)
            annoReq.setParam("facet", "false")
            annoReq.setParam("rows", str(99999))
            annoReq.setParam("sort", "dateCreated asc")
            annoReq.setParam("start", str(0))        
            anotarOut = ByteArrayOutputStream()
            Services.indexer.annotateSearch(annoReq, anotarOut)
            resultForAnotar = JsonConfigHelper(ByteArrayInputStream(anotarOut.toByteArray()))
            resultForAnotar = resultForAnotar.getJsonList("response/docs")
            ids = HashSet()
            for annoDoc in resultForAnotar:
                annotatesUri = annoDoc.get("annotatesUri")
                ids.add(annotatesUri)
                print "Found annotation for %s" % annotatesUri
            # add annotation ids to query
            query += ' OR id:("' + '" OR "'.join(ids) + '")'

        portalSearchQuery = self.__portal.searchQuery
        if portalSearchQuery == "":
            portalSearchQuery = query
        else:
            if query != "*:*":
                query += " AND " + portalSearchQuery
            else:
                query = portalSearchQuery

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
        
        self.__selected = list(req.getParams("fq"))

        sessionState.set("fq", self.__selected)
        sessionState.set("searchQuery", portalSearchQuery)
        sessionState.set("pageNum", self.__pageNum)
        
        # Make sure 'fq' has already been set in the session
        if not page.authentication.is_admin():
            security_roles = page.authentication.get_roles_list()
            security_query = 'security_filter:("' + '" OR "'.join(security_roles) + '")'
            current_user = page.authentication.get_username()
            owner_query = 'owner:"' + current_user + '"'
            req.addParam("fq", "(" + security_query + ") OR (" + owner_query + ")")

        req.setParam("start", str((self.__pageNum - 1) * recordsPerPage))
        
        print " * search.py:", req.toString(), self.__pageNum
        
        out = ByteArrayOutputStream()
        Services.indexer.search(req, out)
        self.__result = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
        if self.__result is not None:
            self.__paging = Pagination(self.__pageNum,
                                       int(self.__result.get("response/numFound")),
                                       self.__portal.recordsPerPage)


    def canManage(self, wfSecurity):
        user_roles = page.authentication.get_roles_list()
        for role in user_roles:
            if role in wfSecurity:
                return True
        return False

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
        return os.path.splitext(os.path.basename(path))[0]
    
    def getFacetQuery(self, name, value):
        return '%s:"%s"' % (name, value)
    
    def isImage(self, format):
        return format.startswith("image/")

    def getMimeTypeIcon(self, format):
        # check for specific icon
        iconPath = "images/icons/mimetype/%s/icon.png" % format
        resource = Services.getPageService().resourceExists(portalId, iconPath)
        if resource is not None:
            return iconPath
        elif format.find("/") != -1:
            # check for major type
            return self.getMimeTypeIcon(format[:format.find("/")])
        # use default icon
        return "images/icons/mimetype/icon.png"
    
    def getActiveManifestTitle(self):
        return self.__getActiveManifest().get("title")
    
    def getActiveManifestId(self):
        return sessionState.get("package/active/id")
    
    def getSelectedItemsCount(self):
        return self.__getActiveManifest().getList("manifest//id").size()
    
    def isSelectedForPackage(self, oid):
        return self.__getActiveManifest().get("manifest//node-%s" % oid) is not None
    
    def getManifestItemTitle(self, oid, defaultValue):
        return self.__getActiveManifest().get("manifest//node-%s/title" % oid, defaultValue)
    
    def __getActiveManifest(self):
        activeManifest = sessionState.get("package/active")
        if not activeManifest:
            activeManifest = JsonConfigHelper()
            activeManifest.set("title", "New package")
            activeManifest.set("viewId", portalId)
            sessionState.set("package/active", activeManifest)
        return activeManifest

if __name__ == "__main__":
    scriptObject = SearchData()
