import os, re

from download import DownloadData
from userAgreement import AgreementData

from java.io import ByteArrayInputStream, ByteArrayOutputStream
from java.lang import Boolean
from java.net import URLDecoder
from java.util import TreeMap

from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common import JsonConfigHelper

class DetailData:
    def __init__(self):
        self.userAgreement = AgreementData(bindings)

    def __activate__(self, context):
        self.velocityContext = context
        self.services = context["Services"]
        self.request = context["request"]
        self.response = context["response"]
        self.contextPath = context["contextPath"]
        self.formData = context["formData"]
        self.page = context["page"]

        self.uaActivated = False

        uri = URLDecoder.decode(self.request.getAttribute("RequestURI"))
        matches = re.match("^(.*?)/(.*?)/(?:(.*?)/)?(.*)$", uri)
        if matches and matches.group(3):
            oid = matches.group(3)
            pid = matches.group(4)

            self.__metadata = JsonConfigHelper()
            self.__object = self.__getObject(oid)
            self.__oid = oid

            # If we have a PID
            if pid:
                # Download the payload instead... supports relative links
                download = DownloadData()
                download.__activate__(context)

            # Otherwise, render the detail screen
            else:
                self.__loadSolrData(oid)
                if self.isIndexed():
                    self.__metadata = self.__solrData.getJsonList("response/docs").get(0)
                    if self.__object is None:
                        # Try again, indexed records might have a special storage_id
                        self.__object = self.__getObject(oid)
                    self.__json = JsonConfigHelper(self.__solrData.getList("response/docs").get(0))
                    self.__metadataMap = TreeMap(self.__json.getMap("/"))
                else:
                    self.__metadata.set("id", oid)
        else:
            # require trailing slash for relative paths
            self.response.sendRedirect("%s/%s/" % (self.contextPath, uri))

    def getFileName(self):
        filePath = self.getProperty("file.path")
        return os.path.split(filePath)[1]

    def getFileNameSplit(self, index):
        return os.path.splitext(self.getFileName())[index]

    def getFriendlyName(self, name):
        if name.startswith("dc_"):
            name = name[3:]
        if name.startswith("meta_"):
            name = name[5:]
        return name.replace("_", " ").capitalize()

    def getMetadata(self):
        return self.__metadata

    def getMetadataMap(self):
        return self.__metadataMap

    def getObject(self):
        return self.__object

    def getOid(self):
        return self.__oid

    def getProperty(self, field):
        return self.getObject().getMetadata().getProperty(field)

    def getUserAgreement(self):
        if not self.uaActivated:
            self.userAgreement.__activate__(self.velocityContext, self.getMetadata())
            self.uaActivated = True
        return self.userAgreement

    def hasLocalFile(self):
        # get original file.path from object properties
        filePath = self.getProperty("file.path")
        return filePath and os.path.exists(filePath)

    def isDetail(self):
        preview = Boolean.parseBoolean(self.formData.get("preview", "false"))
        return not (self.request.isXHR() or preview)

    def isIndexed(self):
        return self.__getNumFound() == 1

    def isPending(self):
        meta = self.getObject().getMetadata()
        status = meta.get("render-pending")
        return Boolean.parseBoolean(status)

    def __getNumFound(self):
        return int(self.__solrData.get("response/numFound"))

    def __getObject(self, oid):
        obj = None
        try:
            storage = self.services.getStorage()
            try:
                obj = storage.getObject(oid)
            except StorageException:
                sid = self.__getStorageId(oid)
                if sid is not None:
                    obj = storage.getObject(sid)
                    print "Object not found: oid='%s', trying sid='%s'" % (oid, sid)
        except StorageException:
            print "Object not found: oid='%s'" % oid
        return obj

    def __getStorageId(self, oid):
        return self.__metadata.get("storage_id")

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
