from __main__ import Services, contextPath, pageName, portalId, request, response

import os

from au.edu.usq.fascinator.api.storage import StorageException

from java.io import ByteArrayOutputStream
from java.net import URLDecoder

from org.apache.commons.io import IOUtils
from org.apache.commons.lang import StringEscapeUtils

class DownloadData:
    def __init__(self):
        basePath = portalId + "/" + pageName
        fullUri = URLDecoder.decode(request.getAttribute("RequestURI"))
        uri = fullUri[len(basePath)+1:]
        try:
            object, payload = self.__resolve(uri)
            if object == None:
                response.sendRedirect(contextPath + "/" + fullUri + "/")
                return
            print "URI='%s' OID='%s' PID='%s'" % (uri, object.getId(), payload.getId())
        except StorageException, e:
            payload = None
            print "Failed to get object: %s" % (str(e))

        if payload is not None:
            filename = os.path.split(payload.getId())[1]
            mimeType = payload.getContentType()
            if mimeType == "application/octet-stream":
                response.setHeader("Content-Disposition", "attachment; filename=%s" % filename)

            type = payload.getContentType()
            # Enocode textual responses before sending
            if type.startswith("text/"):
                out = ByteArrayOutputStream()
                IOUtils.copy(payload.open(), out)
                payload.close()
                writer = response.getPrintWriter(type + "; charset=UTF-8")
                writer.println(out.toString("UTF-8"))
                writer.close()
            # Other data can just be streamed out
            else:
                out = response.getOutputStream(payload.getContentType())
                IOUtils.copy(payload.open(), out)
                payload.close()
                object.close()
                out.close()
        else:
            response.setStatus(404)
            writer = response.getPrintWriter("text/plain; charset=UTF-8")
            writer.println("Not found: uri='%s'" % uri)
            writer.close()
    
    def __resolve(self, uri):
        slash = uri.find("/")

        if slash == -1:
            return None, None

        oid = uri[:slash]
        object = Services.storage.getObject(oid)

        pid = uri[slash+1:]
        if pid == "":
            pid = object.getSourceId()

        payload = object.getPayload(pid)
        return object, payload

if __name__ == "__main__":
    scriptObject = DownloadData()
