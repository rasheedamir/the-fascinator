from au.edu.usq.fascinator.api.storage import PayloadType, StorageException
from java.io import ByteArrayOutputStream
from org.apache.commons.io import IOUtils
from org.w3c.tidy import Tidy

class PreviewData:
    def __init__(self):
        oid = formData.get("oid")
        self.__content = self.__load(oid)
    
    def getContent(self):
        return self.__content;
    
    def __load(self, oid):
        template = """<div class="title" /><div class="page-toc" /><div class="body"><div>%s</div></div>"""
        print "Loading HTML preview for %s..." % oid
        if oid == "blank":
            return template % "<p>This page intentionally left blank.</p>"
        else:
            object = Services.getStorage().getObject(oid)
            
            # get preview payload or source if no preview
            pid = self.__getPreviewPid(object)
            payload = object.getPayload(pid)
            mimeType = payload.getContentType()
            
            print "pid=%s mimeType=%s" % (pid, mimeType)
            isHtml = mimeType in ["text/html", "application/xhtml+xml"]
            if isHtml or mimeType.startswith("text/"):
                out = ByteArrayOutputStream()
                IOUtils.copy(payload.open(), out)
                content = out.toString("UTF-8")
                if content.find('class="body"'):  ## assumes ICE content
                    return content
                elif isHtml:
                    return template % content
                elif mimeType == "text/plain":
                    return template % ('<pre>%s</pre>' % content)
                else:
                    return content
            elif mimeType.startswith("image/"):
                return template % ('<div rel="%s"><img src="%s" /></div>' % (oid, pid))
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
    
    def __tidy(self, content):
        tidy = Tidy()
        tidy.setIndentAttributes(False)
        tidy.setIndentContent(False)
        tidy.setPrintBodyOnly(True)
        tidy.setSmartIndent(False)
        tidy.setWraplen(0)
        tidy.setXHTML(True)
        tidy.setNumEntities(True)
        tidy.setShowWarnings(False)
        tidy.setQuiet(True)
        out = ByteArrayOutputStream()
        tidy.parse(IOUtils.toInputStream(content, "UTF-8"), out)
        return out.toString("UTF-8")

scriptObject = PreviewData()
