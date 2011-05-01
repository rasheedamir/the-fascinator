import os
from org.apache.commons.io import IOUtils
from java.net import URLDecoder

class DownloadData:
    def __init__(self):
        basePath = portalId + "/" + pageName
        uri = URLDecoder.decode(request.getAttribute("RequestURI"))
        uri = uri[len(basePath)+1:]
        oid, pid, payload = self.__resolve(uri)
        print """ * download.py: uri='%s'
                oid='%s'
                pid='%s'
                payload='%s'""" % (uri, oid, pid, payload)
        if payload is not None:
            filename = os.path.split(pid)[1]
            mimeType = payload.contentType
            if mimeType == "application/octet-stream":
                response.setHeader("Content-Disposition", "attachment; filename=%s" % filename)
            out = response.getOutputStream(payload.contentType)
            IOUtils.copy(payload.inputStream, out)
            out.close()
        else:
            response.setStatus(404)
            writer = response.getPrintWriter("text/plain")
            writer.println("Not found: uri='%s'" % uri)
            writer.close()
    
    def __resolve(self, uri):
        slash = uri.rfind("/")
        oid = uri
        pid = uri[slash+1:]
        payload = Services.storage.getPayload(oid, pid)
        #print " ******* oid=%s\n ******* pid=%s\n ******* payload=%s" % (oid, pid, payload)
        if payload is None:
            uri2 = uri
            while payload is None and uri2.find("/") > -1:
                slash = uri2.rfind("/")
                oid = uri[:slash]
                pid = uri[slash+1:]
                payload = Services.storage.getPayload(oid, pid)
                uri2 = uri2[:slash]
                #print " ******* oid=%s\n ******* pid=%s\n ******* payload=%s" % (oid,pid,payload)
        return oid, pid, payload

scriptObject = DownloadData()