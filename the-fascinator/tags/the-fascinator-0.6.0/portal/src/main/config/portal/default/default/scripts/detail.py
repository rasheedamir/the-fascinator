import os, urllib

from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common import JsonConfigHelper

from java.awt import Desktop
from java.io import ByteArrayInputStream, ByteArrayOutputStream, File, StringWriter
from java.net import URLDecoder, URLEncoder
from java.util import TreeMap

from org.apache.commons.io import FileUtils, IOUtils
from org.apache.commons.lang import StringEscapeUtils

class SolrDoc:
    def __init__(self, json):
        self.json = json

    def getField(self, name):
        field = self.json.getList("response/docs/%s" % name)
        if field.isEmpty():
            return None
        return field.get(0)

    def getFieldText(self, name):
        return self.json.get("response/docs/%s" % name)

    def getFieldList(self, name):
        return self.json.getList("response/docs/%s" % name)

    def getDublinCore(self):
        dc = self.json.getList("response/docs").get(0)
        remove = []
        for entry in dc:
            if not entry.startswith("dc_") and not entry.startswith("meta_"):
                remove.append(entry)
        for key in remove:
            dc.remove(key)
        return TreeMap(JsonConfigHelper(dc).getMap("/"))

    def getPreview(self):
        dc = self.json.getList("response/docs").get(0)
        try:
            preview = dc.get("preview")
            return preview
        except Exception, e:
            return None

    def getAltPreviews(self):
        dc = self.json.getList("response/docs").get(0)
        return dc.get("altpreview") or []

    def isPackage(self):
        return self.getField("dc_format") == "application/x-fascinator-package"

    def toString(self):
        return self.json.toString()


class DetailData:
    def __init__(self):
        self.__flvFlag = None
        self.__object = None
        self.__render = True
        if formData.get("func") == "open-file":
            self.__openFile()
            writer = response.getPrintWriter("text/plain; charset=UTF-8")
            writer.println("{}")
            writer.close()
        else:
            self.__storage = Services.storage
            uri = URLDecoder.decode(request.getAttribute("RequestURI"))
            basePath = portalId + "/" + pageName
            baseOid = uri[len(basePath)+1:]
            slash = baseOid.find("/")
            if slash != -1:
                self.__oid = baseOid[:slash]
                self.__pid = baseOid[slash+1:]

                if self.__pid != "":
                    from download import DownloadData
                    DownloadData()
                    self.__render = False
                    bindings.put("SkipTemplateRender", True)
                    return
            else:
                # Fix missing trailing slashes
                response.sendRedirect(contextPath + "/" + uri + "/")
                self.__render = False
                bindings.put("SkipTemplateRender", True)
                return

            self.__render = True
            try:
                try:
                    self.__object = self.__storage.getObject(self.__oid)
                    self.__sid = self.__oid
                except StorageException, e:
                    self.__sid = self.__getStorageId(self.__oid)
                    self.__object = self.__storage.getObject(self.__sid)
                self.__pid = self.__object.getSourceId()
                self.__payload = self.__object.getPayload(self.__pid)
                self.__mimeType = self.__payload.getContentType()
            except StorageException, e2:
                self.__sid = self.__oid
                self.__mimeType = "application/octet-stream"

            print "URI='%s' OID='%s' SID='%s' PID='%s' MIME='%s'" % (uri, self.__oid, self.__sid, self.__pid, self.__mimeType)
            self.__metadata = JsonConfigHelper()
            self.__search()

            if int(self.__json.get("response/numFound")) == 0:
                return

            self.__previewPayload = self.__metadata.getPreview()

            # get the package manifest
            self.__manifest = JsonConfigHelper()
            if self.__metadata.isPackage():
                try:
                    sourceId = self.__object.getSourceId()
                    payload = self.__object.getPayload(sourceId)
                    self.__manifest = JsonConfigHelper(payload.open())
                    payload.close()
                except StorageException, e:
                    pass

    def __getStorageId(self, oid):
        req = SearchRequest('id:"%s"' % oid)
        req.addParam("fl", "storage_id")
        out = ByteArrayOutputStream()
        Services.indexer.search(req, out)
        json = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
        if int(json.get("response/numFound")) == 0:
            raise StorageException("No object matching id: '%s'" % oid)
        return json.getList("response/docs").get(0).get("storage_id")

    def isRendered(self):
        return self.__render

    def __openFile(self):
        file = formData.get("file")
        print "opening file %s..." % file
        Desktop.getDesktop().open(File(file))

    def __search(self):
        req = SearchRequest('id:"%s"' % self.__oid)
        out = ByteArrayOutputStream()
        Services.indexer.search(req, out)
        self.__json = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
        self.__metadata = SolrDoc(self.__json)

    def canOpenFile(self):
        #f = File(self.getObject().getId())
        #return f.exists();
        # get original file.path from properties
        filePath = self.__object.getMetadata().getProperty("file.path")
        return filePath is not None and File(filePath).exists()

    def canOrganise(self):
        userRoles = page.authentication.get_roles_list()
        workflowSecurity = self.__metadata.getFieldList("workflow_security")
        for userRole in userRoles:
            if userRole in workflowSecurity:
                return True
        return False

    def closeObject(self):
        object = self.getObject()
        if object is not None:
            object.close()

    def containsPid(self, pid):
        return self.getObject().getPayloadIdList().contains(pid);

    def doRender():
        return self.__render

    def encode(self, url):
        return URLEncoder.encode(url, "UTF-8")

    def formatMetaName(self, name):
        return name.replace("dc_", "DC.")

    def formatName(self, name):
        if name.startswith("dc_"):
            name = name[3:]
        if name.startswith("meta_"):
            name = name[5:]
        return name.replace("_", " ").capitalize()

    def getError(self):
        payloadIdList = self.getObject().getPayloadIdList()
        for payloadId in payloadIdList:
            try:
                payload = self.getObject().getPayload(payloadId)
                if str(payload.getType()) == "Error":
                    return payload
            except StorageException, e:
                pass
        return None

    def getFileName(self, path):
        return os.path.split(path)[1]

    def getFilePathWithoutExt(self, path):
        return os.path.splitext(self.getFileName(path))[0]

    def getFileSize(self, path):
        return FileUtils.byteCountToDisplaySize(os.path.getsize(path))

    def getMetadata(self):
        return self.__metadata

    def getMimeType(self):
        return self.__mimeType

    def getObject(self):
        return self.__object

    def getPackageManifest(self):
        return self.__manifest.getJsonMap("manifest")

    def getPayloadContent(self):
        mimeType = self.__mimeType
        print "payload content mimeType=%s" % mimeType
        contentStr = ""
        if mimeType == None:  #e.g. 7z file
            return '<h4 class="error">No preview available</h4><p>Please download the file instead...</p></pre>'
        if mimeType == "application/octet-stream":
            dcFormat = self.__json.get("response/docs/dc_format")
            if dcFormat is not None:
                dcFormat = dcFormat[1:-1]
            print dcFormat, mimeType
            if dcFormat != mimeType:
                return "<div><em>(File not found)</em></div>"
            else:
                return "<div><em>(Binary file)</em></div>"
        elif mimeType.startswith("text/"):
            if mimeType == "text/html":
                objectPath = "http://%s:%s%s/%s/download/%s/" % \
                (request.serverName, serverPort, contextPath, portalId, self.__oid)
                objectLink = '<a class="iframe-link-alt" href="%s">View outside the frame</a>' % objectPath
                objectFrame = '<iframe class="iframe-preview" src="%s"></iframe>' % objectPath
                contentStr = objectLink + "<br/>" + objectFrame

            else:
                #print "pid=%s payload=%s" % (pid, payload)
                if self.__payload is not None:
                    sw = StringWriter()
                    IOUtils.copy(self.__payload.open(), sw)
                    self.__payload.close()
                    sw.flush()
                    contentStr = "<pre>" + StringEscapeUtils.escapeHtml(sw.toString()) + "</pre>"
        elif mimeType == "application/pdf" \
            or mimeType.find("vnd.ms")>-1 \
            or mimeType.find("vnd.openxmlformats")>-1 \
            or mimeType.find("vnd.oasis.opendocument.")>-1:
            # get the html version if exist...
            pid = self.getPreview()
            #print "pid=%s" % pid
            #contentStr = '<iframe class="iframe-preview" src="%s/%s/download/%s/%s"></iframe>' % \
            #    (contextPath, portalId, self.__oid, pid)
            try:
                payload = self.getObject().getPayload(pid)
                out = ByteArrayOutputStream()
                IOUtils.copy(payload.open(), out)
                payload.close()
                contentStr = out.toString("UTF-8")

            except StorageException, e:
                errOut = ""
                payload = self.getError()
                if payload is not None:
                    out = ByteArrayOutputStream()
                    IOUtils.copy(payload.open(), out)
                    payload.close()
                    errOut = '<pre>' + out.toString("UTF-8") + '</pre>'
                contentStr = '<h4 class="error">No preview available</h4><p>You can always <a class="open-this" href="#">access the original source file</a>.</p><p>Administrators can attempt re-rendering by using the Re-Harvest action.</p><h5>Details</h5>' + errOut
        elif self.hasError():
            payload = self.getError()
            out = ByteArrayOutputStream()
            IOUtils.copy(payload.open(), out)
            payload.close()
            contentStr = '<h4 class="error">No preview available</h4><p>Please try re-rendering by using the Re-Harvest action.</p><h5>Details</h5><pre>' + out.toString("UTF-8") + '</pre>'
        return contentStr

    def getPayLoadUrl(self, pid):
        return "%s/%s" % (self.__oid, pid)

    def getPdfUrl(self):
        pid = os.path.splitext(self.__pid)[0] + ".pdf"
        return "%s/%s" % (self.__oid, pid)

    def getPreview(self):
        return self.__previewPayload

    def getAltPreviews(self):
        return self.__metadata.getAltPreviews()

    def getSolrResponse(self):
        return self.__json

    def getStorageId(self):
        obj = self.getObject()
        return obj.getId()

    def getWorkflow(self):
        return self.__workflowStep

    def hasError(self):
        payloadIdList = self.getObject().getPayloadIdList()
        for payloadId in payloadIdList:
            try:
                payload = self.getObject().getPayload(payloadId)
                if str(payload.getType()) == "Error":
                    return True
            except StorageException, e:
                pass
        return False

    def hasFlv(self):
        if self.__flvFlag is not None:
            return self.__flvFlag
        payloadIdList = self.getObject().getPayloadIdList()
        for payloadId in payloadIdList:
            try:
                payload = self.getObject().getPayload(payloadId)
                mimeType = payload.getContentType()
                if mimeType == "video/x-flv":
                    self.__flvFlag = urllib.quote(payloadId)
                    return self.__flvFlag
            except StorageException, e:
                pass
        self.__flvFlag = False
        return False

    def hasHtml(self):
        payloadIdList = self.getObject().getPayloadIdList()
        for payloadId in payloadIdList:
            try:
                payload = self.getObject().getPayload(payloadId)
                mimeType = payload.getContentType()
                if mimeType == "text/html" or mimeType == "application/xhtml+xml":
                    return True
            except StorageException, e:
                pass
        return False

    def hasImsManifest(self):
        try:
            payload = self.getObject().getPayload("imsmanifest.xml")
            return True
        except StorageException, e:
            pass
        return False

    def hasSlideShow(self):
        pid = self.__pid
        pid = pid[:pid.find(".")] + ".slide.htm"
        if self.containsPid(pid):
            return pid
        else:
            return False

    def hasWorkflow(self):
        self.__workflowStep = self.__metadata.getFieldList("workflow_step_label")
        if self.__workflowStep.size() == 0:
            return False
        else:
            self.__workflowStep = self.__workflowStep.get(0)
            return True

    def isHidden(self, pid):
        if pid.find("_files%2F")>-1:
            return True
        return False

    def isMetadataOnly(self):
        previewPid = self.getPreview()
        if previewPid is None:
            if self.__metadata.isPackage() or self.hasFlv() or self.hasHtml():
                return False
            elif self.hasError():
                #check if have error...
                return False
            return True
        else:
            return False

    def isPending(self):
        object = self.getObject()
        metaProps = object.getMetadata()
        status = metaProps.get("render-pending")
        if status is None or status == "false":
            return False
        else:
            return True

scriptObject = DetailData()
