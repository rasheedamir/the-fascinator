import os

from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.api.storage import PayloadType
from au.edu.usq.fascinator.common import JsonConfigHelper

from java.awt import Desktop
from java.io import ByteArrayInputStream, ByteArrayOutputStream, File, StringWriter
from java.net import URLDecoder
from java.lang import Boolean

from org.apache.commons.io import IOUtils
from org.dom4j.io import OutputFormat, XMLWriter, SAXReader

import traceback

import traceback

class SolrDoc:
    def __init__(self, json):
        self.json = json
    
    def getField(self, name):
        field = self.json.getList("response/docs/%s" % name)
        print " ***** field: %s" % field
        if field.isEmpty():
            return None
        return field.get(0)
    
    def getFieldText(self, name):
        return self.json.get("response/docs/%s" % name)
    
    def getFieldList(self, name):
        return self.json.getList("response/docs/%s" % name)
    
    def getDublinCore(self):
        dc = self.json.getList("response/docs").get(0)
        remove = ["dc_title", "dc_description"]
        for entry in dc:
            if not entry.startswith("dc_"):
                remove.append(entry)
        for key in remove:
            dc.remove(key)
        return JsonConfigHelper(dc).getMap("/")
    
    def toString(self):
        return self.json.toString()

class DetailData:
    def __init__(self):
        self.__storage = Services.storage
        uri = URLDecoder.decode(request.getAttribute("RequestURI"))
        basePath = portalId + "/" + pageName
        self.__oid = uri[len(basePath)+1:]
        slash = self.__oid.rfind("/")
        self.__pid = self.__oid[slash+1:]
        print " * detail.py: uri='%s' oid='%s' pid='%s'" % (uri, self.__oid, self.__pid)
        if formData.get("verb") == "open":
            self.__openFile()
        else:
            payload = self.__storage.getPayload(self.__oid, self.__pid)
            if payload is not None:
                self.__mimeType = payload.contentType
            else:
                self.__mimeType = "application/octet-stream"
            self.__metadata = JsonConfigHelper()
            self.__search()
    
    def __search(self):
        req = SearchRequest('id:"%s"' % self.__oid)
        out = ByteArrayOutputStream()
        Services.indexer.search(req, out)
        self.__json = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
        self.__metadata = SolrDoc(self.__json)
    
    def getFileName(self, path):
        return os.path.split(path)[1]
    
    def getMimeType(self):
        return self.__mimeType
    
    def getSolrResponse(self):
        return self.__json
    
    def formatName(self, name):
        return name[3:4].upper() + name[4:]
    
    def formatValue(self, value):
        return value[1:-1]
    
    def isHidden(self, pid):
        if pid.find("_files%2F")>-1:
            return True
        return False
    
    def getMetadata(self):
        return self.__metadata
    
    def getObject(self):
        return self.__storage.getObject(self.__oid)
    
    def getStorageId(self):
        obj = self.getObject()
        if hasattr(obj, "getPath"):
            return obj.path.absolutePath
        return obj.id
    
    def hasSlideShow(self):
        pid = self.__pid
        pid = pid[:pid.find(".")] + ".slide.htm"
        payload = self.__storage.getPayload(self.__oid, pid)
        if payload is None:
            return ""
        return pid
    
    def hasSpeechConversion(self):
        #This is only work for .txt
        if self.__mimeType=="text/plain":
            pid = self.__pid
            pid = pid[:pid.find(".")] + ".mp3"
            payload = self.__storage.getPayload(self.__oid, pid)
            if payload is None:
                return ""
            return pid
        return ""
    
    def getPayloadContent(self):
        mimeType = self.__mimeType
        print " * detail.py: payload content mimeType=%s" % mimeType
        contentStr = ""
        if mimeType.startswith("text/"):
            if mimeType == "text/html":
                contentStr = '<iframe class="iframe-preview" src="%s/%s/download/%s"></iframe>' % \
                    (contextPath, portalId, self.__oid)
            else:
                pid = self.__oid[self.__oid.rfind("/")+1:]
                payload = self.__storage.getPayload(self.__oid, pid)
                print " * detail.py: pid=%s payload=%s" % (pid, payload)
                if payload is not None:
                    sw = StringWriter()
                    sw.write("<pre>")
                    IOUtils.copy(payload.getInputStream(), sw)
                    sw.write("</pre>")
                    sw.flush()
                    contentStr = sw.toString()
        elif mimeType == "application/pdf" or mimeType.find("vnd")>-1 or mimeType.find("vnd.oasis.opendocument.")>-1:
            # get the html version if exist...
            pid = os.path.splitext(self.__pid)[0] + ".htm"
            print " * detail.py: pid=%s" % pid
            #contentStr = '<iframe class="iframe-preview" src="%s/%s/download/%s/%s"></iframe>' % \
            #    (contextPath, portalId, self.__oid, pid)
            payload = self.__storage.getPayload(self.__oid, pid)
            saxReader = SAXReader(Boolean.parseBoolean("false"))
            try:
                document = saxReader.read(payload.getInputStream())
            except:
                traceback.print_exc()
            #slideNode = document.selectSingleNode("//div[@class='body']")
            slideNode = document.selectSingleNode("//*[local-name()='body']")
            #linkNodes = slideNode.selectNodes("//img")
            #contentStr = slideNode.asXML();
            # encode character entities correctly
            out = ByteArrayOutputStream()
            format = OutputFormat.createPrettyPrint()
            format.setSuppressDeclaration(True)
            writer = XMLWriter(out, format)
            writer.write(slideNode)
            writer.close()
            contentStr = out.toString("UTF-8")
        return contentStr
    
    def __openFile(self):
        value = formData.get("value")
        print " * detail.py: opening file %s..." % value
        Desktop.getDesktop().open(File(value))

scriptObject = DetailData()
