import os

from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common import JsonConfigHelper

from java.io import ByteArrayInputStream, ByteArrayOutputStream
from java.lang import Boolean
from java.net import URLDecoder
from java.util import TreeMap

from org.apache.commons.io import IOUtils

class DownloadData:
    def __init__(self):
        pass
    
    def __activate__(self, context):
        self.services = context["Services"]
        self.contextPath = context["contextPath"]
        self.pageName = context["pageName"]
        self.portalId = context["portalId"]
        self.request = context["request"]
        self.response = context["response"]
        self.formData = context["formData"]
        self.page = context["page"]

        self.__metadata = JsonConfigHelper()
        object = None
        payload = None

        basePath = self.portalId + "/" + self.pageName
        fullUri = URLDecoder.decode(self.request.getAttribute("RequestURI"))
        uri = fullUri[len(basePath)+1:]
        try:
            object, payload = self.__resolve(uri)
            if object is None:
                print " *** redirecting because object not found: '%s'" % uri
                self.response.sendRedirect(self.contextPath + "/" + fullUri + "/")
                return
            else:
                oid = object.getId()
                self.__loadSolrData(oid)
                if self.isIndexed():
                    self.__metadata = self.__solrData.getJsonList("response/docs").get(0)
                    self.__json = JsonConfigHelper(self.__solrData.getList("response/docs").get(0))
                    self.__metadataMap = TreeMap(self.__json.getMap("/"))
                else:
                    self.__metadata.set("id", oid)
            #print "URI='%s' OID='%s' PID='%s'" % (uri, object.getId(), payload.getId())
        except StorageException, e:
            payload = None
            print "Failed to get object: %s" % (str(e))

        if self.isAccessDenied():
            # Redirect to the object page for standard access denied error
            self.response.sendRedirect(self.contextPath + "/" + self.portalId + "/detail/" + object.getId())
            return

        ## The byte range cache will check for byte range requests first
        self.cache = self.services.getByteRangeCache()
        processed = self.cache.processRequest(self.request, self.response, payload)
        if processed:
            # We don't need to return data, the cache took care of it.
            return

        if payload is not None:
            filename = os.path.split(payload.getId())[1]
            mimeType = payload.getContentType()
            if mimeType == "application/octet-stream":
                self.response.setHeader("Content-Disposition", "attachment; filename=%s" % filename)

            type = payload.getContentType()
            # Enocode textual responses before sending
            if type is not None and type.startswith("text/"):
                out = ByteArrayOutputStream()
                IOUtils.copy(payload.open(), out)
                payload.close()
                writer = self.response.getPrintWriter(type + "; charset=UTF-8")
                writer.println(out.toString("UTF-8"))
                writer.close()
            # Other data can just be streamed out
            else:
                if type is None:
                    # Send as raw data
                    out = self.response.getOutputStream("application/octet-stream")
                else:
                    out = self.response.getOutputStream(type)
                IOUtils.copy(payload.open(), out)
                payload.close()
                object.close()
                out.close()
        else:
            self.response.setStatus(404)
            writer = self.response.getPrintWriter("text/plain; charset=UTF-8")
            writer.println("Resource not found: uri='%s'" % uri)
            writer.close()

    def getAllowedRoles(self):
        metadata = self.getMetadata()
        if metadata is not None:
            return metadata.getList("security_filter")
        else:
            return []

    def getMetadata(self):
        return self.__metadata

    def isAccessDenied(self):
        # Admins always have access
        if self.page.authentication.is_admin():
            return False

        # Check for normal access
        myRoles = self.page.authentication.get_roles_list()
        allowedRoles = self.getAllowedRoles()
        for role in myRoles:
            if role in allowedRoles:
                return  False
        return True

    def isDetail(self):
        preview = Boolean.parseBoolean(self.formData.get("preview", "false"))
        return not (self.request.isXHR() or preview)

    def isIndexed(self):
        return self.__getNumFound() == 1

    def __resolve(self, uri):
        slash = uri.find("/")
        if slash == -1:
            return None, None
        oid = uri[:slash]
        try:
            object = self.services.getStorage().getObject(oid)
        except StorageException:
            # not found check if oid's are mapped differently, use storage_id
            sid = self.__getStorageId(oid)
            object = self.services.getStorage().getObject(sid)
        pid = uri[slash+1:]
        if pid == "":
            pid = object.getSourceId()
        payload = object.getPayload(pid)
        return object, payload

    def __getNumFound(self):
        return int(self.__solrData.get("response/numFound"))

    def __getStorageId(self, oid):
        req = SearchRequest('id:"%s"' % oid)
        req.addParam("fl", "storage_id")
        out = ByteArrayOutputStream()
        self.services.getIndexer().search(req, out)
        json = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
        return json.getList("response/docs").get(0).get("storage_id")

    def __loadSolrData(self, oid):
        portal = self.page.getPortal()
        query = 'id:"%s"' % oid
        if self.isDetail() and portal.getSearchQuery():
            query += " AND " + portal.getSearchQuery()
        req = SearchRequest(query)
        req.addParam("fq", 'item_type:"object"')
        if self.isDetail():
            req.addParam("fq", portal.getQuery())
        out = ByteArrayOutputStream()
        self.services.getIndexer().search(req, out)
        self.__solrData = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
