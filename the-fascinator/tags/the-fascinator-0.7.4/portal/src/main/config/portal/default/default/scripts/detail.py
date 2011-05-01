import os, re

from download import DownloadData
from userAgreement import AgreementData

from java.io import ByteArrayInputStream, ByteArrayOutputStream
from java.lang import Boolean
from java.net import URLDecoder, URLEncoder
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
        useDownload = Boolean.parseBoolean(self.formData.get("download", "true"))
        self.__isPreview = Boolean.parseBoolean(self.formData.get("preview", "false"))
        self.__previewPid = None
        self.__hasPid = False

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
                self.__hasPid = True
                if useDownload:
                    # Download the payload to support relative links
                    download = DownloadData()
                    download.__activate__(context)
                else:
                    # Render the detail screen with the alternative preview
                    self.__readMetadata(oid)
                    self.__previewPid = pid
            # Otherwise, render the detail screen
            else:
                self.__readMetadata(oid)
                self.__previewPid = self.getPreview()

            if self.__previewPid:
                self.__previewPid = URLEncoder.encode(self.__previewPid, "UTF-8")
        else:
            # require trailing slash for relative paths
            q = ""
            if self.__isPreview:
                q = "?preview=true"
            self.response.sendRedirect("%s/%s/%s" % (self.contextPath, uri, q))

    def getAllowedRoles(self):
        metadata = self.getMetadata()
        if metadata is not None:
            return metadata.getList("security_filter")
        else:
            return []

    def getAllPreviews(self):
        list = self.getAltPreviews()
        preview = self.getPreview()
        if not list.contains(preview):
            list.add(preview)
        return list

    def getAltPreviews(self):
        return self.__metadata.getList("altpreview")

    def getFileName(self):
        return self.getObject().getSourceId()

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

    def getPreview(self):
        return self.__metadata.get("preview")

    def getPreviewPid(self):
        return self.__previewPid

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

    def hasPid(self):
        return self.__hasPid

    def isAccessDenied(self):
        myRoles = self.page.authentication.get_roles_list()
        allowedRoles = self.getAllowedRoles()
        for role in myRoles:
            if role in allowedRoles:
                return False
        return True

    def isDetail(self):
        return not (self.request.isXHR() or self.__isPreview)

    def isIndexed(self):
        return self.__getNumFound() == 1

    def isPending(self):
        meta = self.getObject().getMetadata()
        status = meta.get("render-pending")
        return Boolean.parseBoolean(status)

    def setStatus(self, status):
        self.response.setStatus(status)

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

    def __readMetadata(self, oid):
        self.__loadSolrData(oid)
        if self.isIndexed():
            self.__metadata = self.__solrData.getJsonList("response/docs").get(0)
            if self.__object is None:
                # Try again, indexed records might have a special storage_id
                self.__object = self.__getObject(oid)
            # Just a more usable instance of metadata
            self.__json = JsonConfigHelper(self.__solrData.getList("response/docs").get(0))
            self.__metadataMap = TreeMap(self.__json.getMap("/"))
        else:
            self.__metadata.set("id", oid)

