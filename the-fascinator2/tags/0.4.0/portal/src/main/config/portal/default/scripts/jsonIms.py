

from java.net import URLDecoder
from org.apache.commons.io import IOUtils
from java.io import BufferedReader
from java.io import InputStreamReader
from java.lang import StringBuilder



class JsonIms(object):
    def __init__(self):
        print "\n-- JsonIms init --"
        self.__mimeType = "text/plain"       # default mimeType
        request.setAttribute("Content-Type", self.__mimeType)
        json = self.__getJson()
        print json
        responseOutput.write(json)

    def __getJson(self):
        data = {}
        basePath = portalId + "/" + pageName
        uri = URLDecoder.decode(request.getAttribute("RequestURI"))
        oid = uri[len(basePath)+1:]
        payload = Services.storage.getPayload(oid, "imsmanifest.xml")
        if payload is not None:
            try:
                from xml.etree import ElementTree as ElementTree
                #xml = ElementTree()
                #IOUtils.copy(payload.inputStream, out)
                sb = StringBuilder()
                inputStream = payload.inputStream
                reader = BufferedReader(InputStreamReader(inputStream,
                            "UTF-8"))
                while True:
                    line=reader.readLine()
                    if line is None:
                        break
                    sb.append(line).append("\n")
                inputStream.close()
                xmlStr =  sb.toString()
                xml = ElementTree.XML(xmlStr)
                ns = xml.tag[:xml.tag.find("}")+1]
                resources = {}
                for res in xml.findall(ns+"resources/"+ns+"resource"):
                    resources[res.attrib.get("identifier")] = res.attrib.get("href")
                #print resources
                organizations = xml.find(ns+"organizations")
                defaultName = organizations.attrib.get("default")
                organizations = organizations.findall(ns+"organization")
                organizations = [o for o in organizations if o.attrib.get("identifier")==defaultName]
                organization = organizations[0]
                title = organization.find(ns+"title").text
                data["title"] = title
                items = []
                for item in organization.findall(ns+"item"):
                    a = item.attrib
                    isVisible = a.get("isvisible")
                    idr = a.get("identifierref")
                    id = resources.get(idr)
                    iTitle = item.find(ns+"title").text
                    if isVisible and id and id.endswith(".htm"):
                        items.append({"attributes":{"id":id}, "data":iTitle})
                data["nodes"] = items
            except Exception, e:
                 data["error"] = "Error - %s" % str(e)
                 print data["error"]
        return repr(data)


scriptObject = JsonIms()
