from au.edu.usq.fascinator.api.storage import Payload
from java.lang import String, Boolean, System
from java.io import ByteArrayInputStream, File, FileInputStream, FileOutputStream, ByteArrayOutputStream, OutputStream
from java.util.zip import ZipOutputStream, ZipEntry
from org.apache.commons.io import IOUtils

from org.dom4j import DocumentHelper, QName
from org.dom4j.io import XMLWriter, OutputFormat, SAXReader

from xml.etree import ElementTree as ElementTree

import traceback, os, hashlib

class Epub:
    def __init__(self):
        print "--- Creating ePub for: %s ---" % portalId
        self.__portal = Services.getPortalManager().get(portalId)
        self.__portalManifest = self.__portal.getJsonMap("manifest")
        
        self.__epubMimetypeStream = None
        self.__epubContainerStream = None
        self.__epubcss = None
        self.__orderedItem = []
        self.__itemRefDict = {}
        
        if self.__portalManifest:
            try:
                self.__getEpubMimeTypeFiles()
                self.__getDigitalItems(self.__portalManifest)
                self.__createEpub()
            except:
                traceback.print_exc()
        
    def __createEpub(self):
        response.setHeader("Content-Disposition", "attachment; filename=%s.epub" % self.__portal.getName())
        out = response.getOutputStream("application/zip")
        zipOutputStream = ZipOutputStream(out)
        
        #save mimetype... and the rest of standard files in epub
        zipOutputStream.putNextEntry(ZipEntry("mimetype"))
        IOUtils.copy(self.__epubMimetypeStream, zipOutputStream)
        zipOutputStream.closeEntry()
        
        zipOutputStream.putNextEntry(ZipEntry("META-INF/container.xml"))
        IOUtils.copy(self.__epubContainerStream, zipOutputStream)
        zipOutputStream.closeEntry()

        zipOutputStream.putNextEntry(ZipEntry("OEBPS/epub.css"))
        IOUtils.copy(self.__epubcss, zipOutputStream)
        zipOutputStream.closeEntry()
        
        #### Creating toc.ncx ####
        tocXml = ElementTree.Element("ncx", {"version": "2005-1", "xml:lang":"en", "xmlns":"http://www.daisy.org/z3986/2005/ncx/"})
        headNode = ElementTree.Element("head")
        tocXml.append(headNode)
        
        headNode.append(ElementTree.Element("meta", {"name": "dtb:uid", "content": "1"}))
        headNode.append(ElementTree.Element("meta", {"name": "dtb:depth", "content": "1"}))
        headNode.append(ElementTree.Element("meta", {"name": "dtb:totalPageCount", "content": "1"}))
        headNode.append(ElementTree.Element("meta", {"name": "dtb:maxPageNumber", "content": "1"}))
        headNode.append(ElementTree.Element("meta", {"name": "dtb:generator", "content": "ICE v2"}))
        
        #docTitle
        docTitle = ElementTree.Element("docTitle")
        textNode = ElementTree.Element("text")
        textNode.text = self.__getPortal().name
        docTitle.append(textNode)
        tocXml.append(docTitle)
        
        #docAuthor
        docAuthor = ElementTree.Element("docAuthor")
        textNode = ElementTree.Element("text")
        textNode.text = "ICE v2"
        docAuthor.append(textNode)
        tocXml.append(docAuthor)
        
        #navMap
        navMap = ElementTree.Element("navMap")
        tocXml.append(navMap)
        
        #### Creating content.opf ####
        contentXml = ElementTree.Element("package", {"version": "2.0", "xmlns":"http://www.idpf.org/2007/opf",
                                                     "unique-identifier":"BookId"})
        
        metadataNode = ElementTree.Element("metadata", {"xmlns:dc": "http://purl.org/dc/elements/1.1/", 
                                                        "xmlns:opf": "http://www.idpf.org/2007/opf"})
        contentXml.append(metadataNode)
        
        #metadata information
        metadata = ElementTree.Element("dc:title")
        metadata.text = self.__getPortal().name
        metadataNode.append(metadata)
        
        metadata = ElementTree.Element("dc:language")
        metadata.text = "en-AU"
        metadataNode.append(metadata)
        
        metadata = ElementTree.Element("dc:creator", {"opf:role":"aut"})
        metadata.text = "ICE"
        metadataNode.append(metadata)
        
        metadata = ElementTree.Element("dc:publisher")
        metadata.text = "University of Southern Queensland"
        metadataNode.append(metadata)
        
        metadata = ElementTree.Element("dc:identifier", {"id":"BookId"})
        metadata.text = self.__getPortal().name
        metadataNode.append(metadata)
        
        #manifest
        manifest = ElementTree.Element("manifest")
        contentXml.append(manifest)
        
        spine = ElementTree.Element("spine", {"toc":"ncx"})
        contentXml.append(spine)
        
        item = ElementTree.Element("item", {"id":"ncx", "href":"toc.ncx", "media-type":"text/xml"})
        manifest.append(item)
        css = ElementTree.Element("item", {"id":"style", "href":"epub.css", "media-type":"text/css"})
        manifest.append(css)
        
        count = 1
        for itemHash in self.__orderedItem:
            id, title, htmlFileName, payloadDict, isImage = self.__itemRefDict[itemHash]
            
            for payloadId in payloadDict:
                payload, payloadType = payloadDict[payloadId]
                if isinstance(payload, Payload):
                    payloadId = payloadId.lower()
                    if payloadType == "application/xhtml+xml":
                        zipOutputStream.putNextEntry(ZipEntry("OEBPS/%s" % payloadId.replace(" ", "_")))
                        ##process the html....
                        saxReader = SAXReader(Boolean.parseBoolean("false"))
                        try:
                            saxDoc = saxReader.read(payload.getInputStream())
#                            ## remove class or style nodes
#                            classOrStyleNodes = saxDoc.selectNodes("//@class | //@style ")
#                            for classOrStyleNode in classOrStyleNodes:
#                                node = classOrStyleNode
#                                if classOrStyleNode.getParent():
#                                    node = classOrStyleNode.getParent()
#                                if node.getQualifiedName() == "img":
#                                    attr = node.attribute(QName("class"))
#                                    if attr:
#                                        node.remove(attr)
#                                    attr = node.attribute(QName("style"))
#                                    if attr:
#                                        node.remove(attr)
                                    
                            ## remove name in a tags
                            ahrefs = saxDoc.selectNodes("//*[local-name()='a' and @name!='']")
                            for a in ahrefs:
                                attr = a.attribute(QName("name"))
                                if attr:
                                    a.remove(attr)
                                    
                            ## fix images src name.... replace space with underscore and all lower case
                            imgs = saxDoc.selectNodes("//*[local-name()='img' and contains(@src, '_files')]")
                            for img in imgs:
                                srcAttr = img.attribute(QName("src"))
                                if srcAttr:
                                    src = srcAttr.getValue()
                                    #hash the sourcename
                                    filepath, filename = os.path.split(src)
                                    filename, ext = os.path.splitext(filename)
                                    filename = hashlib.md5(filename).hexdigest()
                                    src = os.path.join(filepath.lower().replace(" ", "_"), "node-%s%s" % (filename, ext))
                                    img.addAttribute(QName("src"), src.replace(" ", "_"))
                                
                            bodyNode = saxDoc.selectSingleNode("//*[local-name()='div' and @class='body']")
                            bodyNode.setName("div")
                            
                            out = ByteArrayOutputStream()
                            format = OutputFormat.createPrettyPrint()
                            format.setSuppressDeclaration(True)
                            writer = XMLWriter(out, format)
                            writer.write(bodyNode)
                            writer.flush()
                            contentStr = out.toString("UTF-8")
                            
                            htmlString = """<html xmlns="http://www.w3.org/1999/xhtml"><head><title>%s</title>
                                            <link rel="stylesheet" href="epub.css"/>
                                            </head><body>%s</body></html>"""
                            htmlString = htmlString % (title, contentStr)
                            
                            IOUtils.copy(ByteArrayInputStream(String(htmlString).getBytes("UTF-8")), zipOutputStream)
                            includeFile = False
                        except:
                            traceback.print_exc()
                    else:
                        #images....
                        zipOutputStream.putNextEntry(ZipEntry("OEBPS/%s" % payloadId))
                        IOUtils.copy(payload.getInputStream(), zipOutputStream)
                        zipOutputStream.closeEntry()
                else:
                    zipOutputStream.putNextEntry(ZipEntry("OEBPS/%s" % payloadId.replace(" ", "_")))
                    IOUtils.copy(payload, zipOutputStream)
                    zipOutputStream.closeEntry()
                
                itemNode = ElementTree.Element("item", {"media-type":payloadType, "href": payloadId.replace(" ", "_")})  
                if payloadId == htmlFileName.lower():
                    itemNode.set("id", itemHash)
                else:
                    itemNode.set("id", payloadId.replace("/", "_"))                  
                manifest.append(itemNode)
                
            if not isImage: 
                navPoint = ElementTree.Element("navPoint", {"class":"chapter", "id":"%s" % itemHash, 
                                                                "playOrder":"%s" % count})
            else:
                navPoint = ElementTree.Element("navPoint", {"class":"chapter", "id":"%s" % htmlFileName, 
                                                                "playOrder":"%s" % count})
            navMap.append(navPoint)
            navLabel = ElementTree.Element("navLabel")
            navPoint.append(navLabel)
            textNode = ElementTree.Element("text")
            textNode.text = title
            navLabel.append(textNode)
            content = ElementTree.Element("content")
            navPoint.append(content)
            content.set("src", htmlFileName)
            count +=1
                
            itemRefNode = ElementTree.Element("itemref")
            spine.append(itemRefNode)
            itemRefNode.set("idref", itemHash)
                
        #saving content.opf...
        zipOutputStream.putNextEntry(ZipEntry("OEBPS/content.opf"))
        IOUtils.copy(ByteArrayInputStream(String(ElementTree.tostring(contentXml)).getBytes("UTF-8")), zipOutputStream)
        zipOutputStream.closeEntry()
        
        #saving toc.ncx
        zipOutputStream.putNextEntry(ZipEntry("OEBPS/toc.ncx"))
        IOUtils.copy(ByteArrayInputStream(String(ElementTree.tostring(tocXml)).getBytes("UTF-8")), zipOutputStream)
        zipOutputStream.closeEntry()
        
        zipOutputStream.close()
        
        
    def __getDigitalItems(self, manifest):
        for itemHash in manifest.keySet():
            payloadDict = {}
            item = manifest[itemHash]
            id = item.get("id")
            title = item.get("title")
            hidden = item.get("hidden")
            children = item.getJsonMap("children")
            pid = id[id.rfind("/")+1:]
            htmlFileName = pid[:pid.rfind(".")] + ".htm"
            nodeHtm = "%s.htm" % itemHash #.replace("-", "_")
            
            isImage=False
            sourcePayload = Services.storage.getPayload(id, pid)
            if sourcePayload and hidden != 'true':
                payloadType = sourcePayload.contentType
                htmlPayload = Services.storage.getPayload(id, htmlFileName)
                process = True
                if htmlPayload:
                    #gather all the related payload
                    payloadDict[nodeHtm] = htmlPayload, "application/xhtml+xml"
                    payloadList = Services.storage.getObject(id).getPayloadList()
                    for payload in payloadList:
                        payloadid = payload.id
                        if payloadid.find("_files") > -1:
                            if payload.contentType.startswith("image"):
                                #hash the name here....
                                filepath, filename = os.path.split(payload.id)
                                filename, ext = os.path.splitext(filename)
                                filename = hashlib.md5(filename).hexdigest()
                                payloadid = os.path.join("%s" % filepath.lower().replace(" ", "_"), "node-%s%s" % (filename, ext))
                            payloadDict[payloadid] = payload, payload.contentType
                #elif sourcePayload:
                    #for now only works for images
                elif payloadType.startswith("image") and sourcePayload:
                        #hash the file name to avoid invalid id in epub...
                        isImage=True
                        #use thumbnail if exist 
                        ext = os.path.splitext(id)[1]
                        filename = id[id.rfind("/")+1:-len(ext)] #+ ".thumb.jpg"
                        hashedFileName = hashlib.md5(filename).hexdigest()
                        thumbNailPayload = Services.storage.getPayload(id, "%s.thumb.jpg" % filename)
                        htmlString = """<html xmlns="http://www.w3.org/1999/xhtml"><head><title>%s</title>
                                        <link rel="stylesheet" href="epub.css"/>
                                        </head><body><div><span style="display: block"><img src="%s" alt="%s"/></span></div></body></html>"""
                        if thumbNailPayload:
                            htmlString = htmlString % (pid, "node-%s.thumb.jpg" % hashedFileName, pid)
                            payloadDict["node-%s.thumb.jpg" % hashedFileName] = thumbNailPayload, "image/jpeg"
                        else:
                            htmlString = htmlString % (pid, pid.lower().replace(" ", "_"), pid)
                            payloadDict[pid] = sourcePayload, payloadType
                        payloadDict[nodeHtm] = ByteArrayInputStream(String(htmlString).getBytes("UTF-8")), "application/xhtml+xml"
                else:
                    process = False
            
                if process:
                    self.__itemRefDict[itemHash] = id, title, nodeHtm, payloadDict, isImage
                    self.__orderedItem.append(itemHash)
                    if children:
                        self.__getDigitalItems(children)
        
    def __getPortal(self):
        return Services.portalManager.get(portalId)
    
    def __getEpubMimeTypeFiles(self):
        try:
            mimeTypeFile = File(self.__getPortal().getClass().getResource("/epub/mimetype").toURI())
            self.__epubMimetypeStream = FileInputStream(mimeTypeFile)
            
            containerFile = File(self.__getPortal().getClass().getResource("/epub/container.xml").toURI())
            self.__epubContainerStream = FileInputStream(containerFile)
            
            cssFile = File(self.__getPortal().getClass().getResource("/epub/epub.css").toURI())
            self.__epubcss = FileInputStream(cssFile)
        except:
            print "Mimetype file/container.xml file for epub are not found or in wrong format"
        
scriptObject = Epub()
