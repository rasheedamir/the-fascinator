import os
from org.apache.commons.io import IOUtils
from java.net import URLDecoder

class DownloadData:
    def __init__(self):
        basePath = portalId + "/" + pageName
        uri = request.getAttribute("RequestURI")
        print " * download.py: basePath=%s uri=%s" % (basePath, uri)
        uri = uri[len(basePath)+1:]
        if uri.find("%2F") == -1:
            slash = uri.rfind("/")
        else:
            slash = uri.find("/")
        oid = URLDecoder.decode(uri[:slash])
        pid = URLDecoder.decode(uri[slash+1:])
        print " * download.py: oid=%s pid=%s" % (oid, pid)
        payload = Services.storage.getPayload(oid, pid)
        filename = os.path.split(pid)[1]
        mimeType = payload.contentType
        if mimeType == "application/octet-stream":
            response.setHeader("Content-Disposition", "attachment; filename=%s" % filename)
        out = response.getOutputStream(payload.contentType)
        IOUtils.copy(payload.inputStream, out)
        out.close()

scriptObject = DownloadData()
