import htmlentitydefs

from au.edu.usq.fascinator.api.storage import PayloadType
from au.edu.usq.fascinator.common import FascinatorHome, JsonConfigHelper
from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.api.storage import StorageException

from com.sun.syndication.feed.atom import Content
from com.sun.syndication.propono.atom.client import AtomClientFactory, BasicAuthStrategy

from java.io import ByteArrayInputStream, ByteArrayOutputStream
from java.net import Proxy, ProxySelector, URL, URLDecoder
from java.lang import Exception
from java.util import TreeMap

from org.apache.commons.io import FileUtils, IOUtils
from org.w3c.tidy import Tidy

from org.apache.velocity import VelocityContext


class ProxyBasicAuthStrategy(BasicAuthStrategy):
    def __init__(self, username, password, baseUrl):
        BasicAuthStrategy.__init__(self, username, password)
        self.__baseUrl = baseUrl
    
    def addAuthentication(self, httpClient, method):
        BasicAuthStrategy.addAuthentication(self, httpClient, method)
        url = URL(self.__baseUrl)
        proxy = ProxySelector.getDefault().select(url.toURI()).get(0)
        httpClient.getParams().setAuthenticationPreemptive(False);
        if not proxy.type().equals(Proxy.Type.DIRECT):
            address = proxy.address()
            proxyHost = address.getHostName()
            proxyPort = address.getPort()
            httpClient.getHostConfiguration().setProxy(proxyHost, proxyPort)
            print "Using proxy '%s:%s'" % (proxyHost, proxyPort)

class BlogData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        self.page = context["page"]
        self.services = context["Services"]
        self.log = context["log"]

        responseType = "text/html; charset=UTF-8"
        responseMsg = ""
        func = self.vc("formData").get("func")
        if func == "url-history":
            responseType = "text/plain; charset=UTF-8"
            responseMsg = "\n".join(self.getUrls())
        else:
            try:
                url = self.vc("formData").get("url")
                title = self.vc("formData").get("title")
                username = self.vc("formData").get("username")
                password = self.vc("formData").get("password")
                
                auth = ProxyBasicAuthStrategy(username, password, url)
                self.__service = AtomClientFactory.getAtomService(url, auth)
                
                try:
                    oid = self.vc("formData").get("oid")
                    self.__object = self.__getObject(oid)
                    self.__readMetadata(oid)
                    sourceId = self.__object.getSourceId()
                    sourcePayload = self.__object.getPayload(sourceId)
                    if sourcePayload and sourcePayload.getContentType() == "application/x-fascinator-package":
                        jsonManifest = JsonConfigHelper(sourcePayload.open())
                        content = self.__getManifestContent(jsonManifest)
                        sourcePayload.close()
                    else:
                        content = self.services.pageService.renderObject(self.__convertToVelocityContext(), "detail", self.__metadata)
                    content, doc = self.__tidy(content)
                    content = self.__processMedia(oid, doc, content)
                    success, value = self.__post(title, content)
                except Exception, e:
                    e.printStackTrace()
                    success = False
                    value = e.getMessage()
                
                if success:
                    altLinks = value.getAlternateLinks()
                    if altLinks is not None:
                        self.saveUrl(url)
                        responseMsg = "<p>Success! Visit the <a href='%s' target='_blank'>blog post</a>.</p>" % altLinks[0].href
                    else:
                        responseMsg = "<p class='warning'>The server did not return a valid link!</p>"
                else:
                    responseMsg = "<p class='error'>%s</p>" % value
            except Exception, e:
                print "Failed to post: %s" % e.getMessage()
                responseMsg = "<p class='error'>%s</p>"  % e.getMessage()
        writer = self.vc("response").getPrintWriter(responseType)
        writer.println(responseMsg)
        writer.close()
    
    # Get from velocity context
    def vc(self, index):
        if self.velocityContext[index] is not None:
            return self.velocityContext[index]
        else:
            self.log.error("ERROR: Requested context entry '" + index + "' doesn't exist")
            return None
        
    def __convertToVelocityContext(self):
        vc = VelocityContext()
        for key in self.velocityContext.keySet():
            vc.put(key, bindings.get(key));
        vc.put("velocityContext", vc);
        return vc
    
    
    def __loadSolrData(self, oid):
        portal = self.page.getPortal()
        query = 'id:"%s"' % oid
        if portal.getSearchQuery():
            query += " AND " + portal.getSearchQuery()
        req = SearchRequest(query)
        req.addParam("fq", 'item_type:"object"')
        req.addParam("fq", portal.getQuery())
        out = ByteArrayOutputStream()
        self.services.getIndexer().search(req, out)
        self.__solrData = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))

    def __readMetadata(self, oid):
        self.__loadSolrData(oid)
        if int(self.__solrData.get("response/numFound"))==1:
            self.__metadata = self.__solrData.getJsonList("response/docs").get(0)
            if self.__object is None:
                # Try again, indexed records might have a special storage_id
                self.__object = self.__getObject(oid)
            # Just a more usable instance of metadata
            self.__json = JsonConfigHelper(self.__solrData.getList("response/docs").get(0))
            self.__metadataMap = TreeMap(self.__json.getMap("/"))
        else:
            self.__metadata.set("id", oid)

    def getUrls(self):
        return FileUtils.readLines(self.__getHistoryFile())
    
    def saveUrl(self, url):
        historyFile = self.__getHistoryFile()
        if historyFile.exists():
            urls = FileUtils.readLines(historyFile)
            urls.add(url)
        FileUtils.writeLines(historyFile, [url])
    
    def __getHistoryFile(self):
        f = FascinatorHome.getPathFile("blog/history.txt")
        if not f.exists():
            f.getParentFile().mkdirs()
            f.createNewFile()
        return f
    
    def __getManifestContent(self, jsonManifest):
        manifest = jsonManifest.getJsonMap("manifest")
        contentStr = "<div>"
        for key in manifest.keySet():
            item = manifest.get(key)
            if item.get("hidden", "False") == "False":
                contentStr += "<div><h2>%s</h2>" % item.get("title")
                contentStr += self.__getContent(item.get("id"))
                contentStr += "</div>"
        contentStr += "</div>"
        return contentStr
    
    def __getContent(self, oid):
        print "getting content for '%s'" % oid
        content = "<div>Content not found!</div>"
        slash = oid.rfind("/")
        payload = self.__getPreviewPayload(oid)
        pid = payload.getId()
        print "preview payload: %s" % payload
        if payload is None:
            print "Failed to get content: %s" % oid
            return ""
        else:
            mimeType = payload.getContentType()
            if mimeType.startswith("image/"):
                content = '<img alt="%s" title="%s" src="%s" />' % (pid, pid, pid)
            elif mimeType in ["text/html", "text/xml", "application/xhtml+xml"]:
                content = self.__getPayloadAsString(payload)
            elif mimeType.startswith("text/"):
                content = "<html><body><pre>%s</pre></body></html>" % \
                    self.__getPayloadAsString(payload)
            else:
                content = "<div>unsupported content type: %s</div>" % mimeType
        print "before tidy"
        content, doc = self.__tidy(content)
        print "after tidy"
        content = self.__processMedia(oid, doc, content)
        return content
    
    def __getPreviewPayload(self, oid):
        object = self.__getObject(oid)
        payloadIdList = object.getPayloadIdList()
        for payloadId in payloadIdList:
            try:
                payload = object.getPayload(payloadId)
                #print "%s: %s" % (payloadId, payload.getType())
                if PayloadType.Preview == payload.getType():
                    return payload
            except Exception, e:
                pass
        return None
    
    def __getPayloadAsString(self, payload):
        out = ByteArrayOutputStream()
        IOUtils.copy(payload.open(), out)
        payload.close()
        return self.__escapeUnicode(out.toString("UTF-8"))

    def __escapeUnicode(self, unicode):
        result = list()
        for char in unicode:
            try:
                if ord(char) < 128:
                    result.append(char)
                else:
                    result.append('&%s;' % htmlentitydefs.codepoint2name[ord(char)])
            except:
                result.append(char)
        return ''.join(result)

    def __tidy(self, content):
        tidy = Tidy()
        tidy.setIndentAttributes(False)
        tidy.setIndentContent(False)
        tidy.setPrintBodyOnly(True)
        tidy.setSmartIndent(False)
        tidy.setWraplen(0)
        tidy.setXHTML(False)
        tidy.setNumEntities(True)
        tidy.setShowWarnings(False)
        tidy.setQuiet(True)
        out = ByteArrayOutputStream()
        doc = tidy.parseDOM(IOUtils.toInputStream(content, "UTF-8"), out)
        content = out.toString("UTF-8")
        return content, doc
    
    def __processMedia(self, oid, doc, content):
        content = self.__uploadMedia(oid, doc, content, "a", "href")
        content = self.__uploadMedia(oid, doc, content, "img", "src")
        return content
    
    def __uploadMedia(self, oid, doc, content, elem, attr):
        links = doc.getElementsByTagName(elem)
        for i in range(0, links.getLength()):
            elem = links.item(i)
            attrValue = elem.getAttribute(attr)
            pid = attrValue
            internalSrc = attrValue
            if attrValue == '' or attrValue.startswith("#") \
                or attrValue.startswith("mailto:") \
                or attrValue.find("://") != -1:
                continue
            else:
                split = attrValue.split("/")
                pid = split[len(split)-1]
                if len(split)>1:
                    # Normally files returned by ice rendition
                    internalSrc = "%s/%s" % (split[len(split)-2], split[len(split)-1])
                
            print "uploading '%s' (%s, %s)" % (pid, elem.tagName, attr)
            found = False
            try:
                payload = self.__getObject(oid).getPayload(pid)
                found = True
            except Exception, e:
                pid = URLDecoder.decode(pid, "UTF-8")
                try:
                    payload = self.__getObject(oid).getPayload(pid)
                    found = True
                except Exception, e:
                    try:
                        print "trying to get: %s payload" % internalSrc
                        payload = self.__getObject(oid).getPayload(internalSrc)
                        found = True
                    except Exception, e:
                        print "payload not found '%s'" % pid
            if found:
                #HACK to upload PDFs
                contentType = payload.getContentType().replace("application/", "image/")
                entry = self.__postMedia(payload.getLabel(), contentType, payload.open())
                payload.close()
                if entry is not None:
                    id = entry.getId()
                    print "replacing %s with %s" % (attrValue, id)
                    content = content.replace('%s="%s"' % (attr, attrValue),
                                              '%s="%s"' % (attr, id))
                    content = content.replace("%s='%s'" % (attr, attrValue),
                                              "%s='%s'" % (attr, id))
                else:
                    print "failed to upload %s" % pid
        return content
    
    def __getCollection(self, type):
        workspaces = self.__service.getWorkspaces()
        if len(workspaces) > 0:
            return workspaces[0].findCollection(None, type)
        return None
    
    def __postMedia(self, slug, type, media):
        print "uploading media %s, %s" % (slug, type)
        collection = self.__getCollection(type)
        if collection is not None:
            entry = collection.createMediaEntry(slug, slug, type, media)
            collection.addEntry(entry)
            return entry
        return None
    
    def __post(self, title, content):
        collection = self.__getCollection("application/atom+xml;type=entry")
        if collection is not None:
            entry = collection.createEntry()
            entry.setTitle(title)
            entry.setContent(content, Content.HTML)
            collection.addEntry(entry)
            return True, entry
        else:
            return False, "No valid collection found"
        return False, "An unknown error occured"
    
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
