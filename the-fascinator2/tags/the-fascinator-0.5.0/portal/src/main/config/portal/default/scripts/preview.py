from au.edu.usq.fascinator.api.storage import PayloadType, StorageException
from java.io import ByteArrayOutputStream
from org.apache.commons.io import IOUtils

class PreviewData:
    def __init__(self):
        print "formData=%s" % (formData)
        self.__content = self.__load(formData.get("oid"))
    
    def getContent(self):
        return self.__content;
    
    def __load(self, oid):
        print "Loading HTML preview for %s..." % oid
        object = Services.getStorage().getObject(oid)
        
        # get preview payload or source if no preview
        pid = self.__getPreviewPid(object)
        payload = object.getPayload(pid)
        mimeType = payload.getContentType()
        
        print "pid=%s mimeType=%s" % (pid, mimeType)
        template = '<div class="title" /><div class="page-toc" /><div class="body"><div>%s</div></div>'
        isHtml = mimeType in ["text/html", "application/xhtml+xml"]
        if isHtml or mimeType.startswith("text/"):
            out = ByteArrayOutputStream()
            IOUtils.copy(payload.open(), out)
            content = out.toString("UTF-8")
            if isHtml:
                return template % content
            elif mimeType == "text/plain":
                return template % ('<pre>%s</pre>' % content)
            else:
                return content
        elif mimeType.startswith("image/"):
            return template % ('<img src="%s" />' % pid)
        else:
            return '<a href="%s" rel="%s">%s</a>' % (oid, mimeType, pid)
        payload.close()
        object.close()
    
    def __getPreviewPid(self, object):
        pidList = object.getPayloadIdList()
        for pid in pidList:
            try:
                payload = object.getPayload(pid)
                if PayloadType.Preview.equals(payload.getType()):
                    return payload.getId()
            except StorageException, e:
                pass
        return object.getSourceId()

scriptObject = PreviewData()
