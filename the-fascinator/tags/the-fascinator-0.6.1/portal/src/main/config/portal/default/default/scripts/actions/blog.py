import htmlentitydefs, sys

from au.edu.usq.fascinator.api.storage import PayloadType
from au.edu.usq.fascinator.common import FascinatorHome, JsonConfigHelper

from com.sun.syndication.feed.atom import Content
from com.sun.syndication.propono.atom.client import AtomClientFactory, BasicAuthStrategy

from java.io import ByteArrayOutputStream
from java.net import Proxy, ProxySelector, URL, URLDecoder
from java.lang import Exception

from org.apache.commons.io import FileUtils, IOUtils
from org.w3c.tidy import Tidy


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

class AtomEntryPoster:
    def __init__(self):
        responseType = "text/html; charset=UTF-8"
        responseMsg = ""
        func = formData.get("func")
        if func == "url-history":
            responseType = "text/plain; charset=UTF-8"
            responseMsg = "\n".join(self.getUrls())
        else:
            try:
                url = formData.get("url")
                title = formData.get("title")
                username = formData.get("username")
                password = formData.get("password")
                try:
                    auth = ProxyBasicAuthStrategy(username, password, url)
                    self.__service = AtomClientFactory.getAtomService(url, auth)
                    oid = formData.get("oid")
                    if oid is not None:
                        self.__object = Services.getStorage().getObject(oid)
                        sourceId = self.__object.getSourceId()
                        payload = self.__object.getPayload(sourceId)
                        print "payload=%s,%s" % (payload, payload.getContentType())
                        # FIXME see https://fascinator.usq.edu.au/trac/ticket/647
                        if payload and sourceId.endswith(".tfpackage"): #payload.getContentType() == "application/x-fascinator-package":
                            jsonManifest = JsonConfigHelper(payload.open())
                            #print jsonManifest.toString()
                            content = self.__getManifestContent(jsonManifest)
                            payload.close()
                        else:
                            content = self.__getContent(oid)
                        self.__object.close()
                    else:
                        content = "<div>Object not found!</div>"
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
        writer = response.getPrintWriter(responseType)
        writer.println(responseMsg)
        writer.close()
    
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
        content, doc = self.__tidy(content)
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
            if ord(char) < 128:
                result.append(char)
            else:
                result.append('&%s;' % htmlentitydefs.codepoint2name[ord(char)])
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
            if attrValue == '' or attrValue.startswith("#") \
                or attrValue.startswith("mailto:") \
                or attrValue.find("://") != -1:
                continue
            pid = attrValue
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
        if self.__object and self.__object.getId() == oid:
            return self.__object
        return Services.getStorage().getObject(oid)
    

scriptObject = AtomEntryPoster()
