from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.api.storage import PayloadType
from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common.storage.impl import GenericPayload
from au.edu.usq.fascinator.common import JsonConfigHelper

from java.io import BufferedReader, ByteArrayInputStream, ByteArrayOutputStream, InputStreamReader
from java.lang import String, StringBuilder, Boolean

import json, time

class AnotarData:
    def __init__(self):
        self.action = formData.get("action")
        self.rootUri = formData.get("rootUri")
        self.json = formData.get("json")
        self.type = formData.get("type")
        self.rootUriList = formData.getValues("rootUriList")
        print " * anotar.py : '" + self.action + "' : ", formData

        # ?? media fragment stuff?
        if self.rootUri and self.rootUri.find("?ticks") > -1:
            self.rootUri = self.rootUri[:self.rootUri.find("?ticks")]

        # Portal path info
        portalPath = contextPath + "/" + portalId + "/"
        self.oid = self.rootUri
        if self.oid and self.oid.startswith(portalPath):
            self.oid = self.oid[len(portalPath):]

        if self.action == "getList":
            # Repsonse is a list of object (nested)
            #print "**** anotar.py : GET_SOLR : " + self.rootUri
            result = self.search_solr()
        elif self.action == "put":
            # Response is an ID
            #print "**** anotar.py : PUT : " + self.rootUri
            result = self.put()
        elif self.action == "get-image":
            self.type = "http://www.purl.org/anotar/ns/type/0.1#Tag"
            mediaFragType = "http://www.w3.org/TR/2009/WD-media-frags-20091217"
            result = '{"result":' + self.search_solr() + '}'
            if result:
                imageTagList = []
                imageTags = JsonConfigHelper(result).getJsonList("result")
                for imageTag in imageTags:
                    imageAno = JsonConfigHelper()
                    if imageTag.getJsonList("annotates/locators"):
                        locatorValue = imageTag.getJsonList("annotates/locators").get(0).get("value")
                        locatorType = imageTag.getJsonList("annotates/locators").get(0).get("type")
                        if locatorValue and locatorValue.find("#xywh=")>-1 and locatorType == mediaFragType:
                            _, locatorValue = locatorValue.split("#xywh=")
                            left, top, width, height = locatorValue.split(",")
                            imageAno.set("top", top)
                            imageAno.set("left", left)
                            imageAno.set("width", width)
                            imageAno.set("height", height)
                            imageAno.set("creator", imageTag.get("creator/literal"))
                            imageAno.set("creatorUri", imageTag.get("creator/uri"))
                            imageAno.set("id", imageTag.get("id"))
                            #tagCount = imageTag.get("tagCount")
                            imageAno.set("text", imageTag.get("content/literal"))
                            #imageAno.set("editable", Boolean(False).toString());
                            imageTagList.append(imageAno.toString())
                result = "[" + ",".join(imageTagList) + "]"
        elif self.action == "save-image":
            jsonTemplate = """
{
  "clientVersionUri": "http://www.purl.org/anotar/client/0.1",
  "type" : "http://www.purl.org/anotar/ns/type/0.1#Tag",
  "title" : {
    "literal" : null,
    "uri" : null
  },  
  "annotates" : {
    "uri" : "%s",
    "rootUri" : "%s",
    "locators" : [ {
      "originalContent": null,
      "type" : "http://www.w3.org/TR/2009/WD-media-frags-20091217",
      "value" : "%s"
    } ]
  },
  "creator" : {
    "literal" : "%s",
    "uri" : "%s",
    "email" : {
      "literal" : null
    }
  },
  "dateCreated" : {
    "literal" : "%s",
    "uri" : null
  },
  "dateModified" : {
    "literal" : null,
    "uri" : null
  },
  "content" : {
    "mimeType" : "text/plain",
    "literal" : "%s",
    "formData" : {
    }
  },
  "contentUri": "",
  "isPrivate" : false,
  "lang" : "en"
}
"""
            mediaDimension = "xywh=%s,%s,%s,%s" % (formData.get("left"), formData.get("top"), formData.get("width"), formData.get("height"))
            locatorValue = "%s#%s" % (self.rootUri, mediaDimension)
            dateCreated = time.strftime("%Y-%m-%dT%H:%M:%SZ")
            self.json = jsonTemplate % (self.rootUri, self.rootUri, locatorValue, formData.get("creator"), formData.get("creatorUri"), \
                                        dateCreated, formData.get("text"))
            result = self.put()
        writer = response.getPrintWriter("text/plain; charset=UTF-8")
        writer.println(result)
        writer.close()

    def generate_id(self):
        counter = 0
        fileName = "anotar." + str(counter)
        payloadList = self.obj.getPayloadIdList()
        while fileName in payloadList:
            counter = counter + 1
            fileName = "anotar." + str(counter)
        self.pid = fileName
        print " * anotar.py : New ID (" + self.pid + ")"

    def modify_json(self):
        #print "**** anotar.py : add_json() : adding json : " + json
        jsonObj = JsonConfigHelper(self.json)
        jsonObj.set("id", self.pid)
        rootUri = jsonObj.get("annotates/rootUri")
        if rootUri is not None:
            baseUrl = "http://%s:%s/" % (request.serverName, serverPort)
            myUri = baseUrl + rootUri + "#" + self.pid
            jsonObj.set("uri", myUri)

        jsonObj.set("schemaVersionUri", "http://www.purl.org/anotar/schema/0.1")

        self.json = jsonObj.toString(False)

    def process_response(self, result):
        docs = []
        rootDocs = []
        docsDict = {}
        # Build a dictionary of the annotations
        for doc in result:
            #hack is done here to replace [] with null as json.py does not properly parse 
            jsonStr = unicode(doc.get("jsonString").replace("[]", "null")).encode("utf-8")
            doc = json.read(jsonStr)
            doc["replies"] = []
            docs.append(doc)
            docsDict[doc["uri"]] = doc
            if doc["annotates"]["uri"] == doc["annotates"]["rootUri"]:
                rootDocs.append(doc)

        # Now process the dictionary
        for doc in docs:
            # If we are NOT a top level annotation
            if doc["annotates"]["uri"] != doc["annotates"]["rootUri"]:
                # Find what we are annotating
                try:
                    d = docsDict[doc["annotates"]["uri"]]
                    d["replies"].append(doc) # Add ourselves to its reply list
                except:
                    # TODO KeyError
                    pass
        return json.write(rootDocs)

    def process_tags(self, result):
        tags = []
        tagsDict = {}
        # Build a dictionary of the tags
        for doc in result:
            doc = JsonConfigHelper(doc.get("jsonString"))
            tag = doc.get("content/literal")
            locs = doc.getJsonList("annotates/locators").size()
            if locs == 0:
                if tag in tagsDict:
                    d = tagsDict[tag]
                    d.set("tagCount", str(int(d.get("tagCount")) + 1))
                else:
                    doc.set("tagCount", str(1))
                    tagsDict[tag] = doc
            else:
                tags.append(doc.toString())

        for tag in tagsDict:
            tags.append(tagsDict[tag].toString())

        return "[" + ",".join(tags) + "]"

    def put(self):
        try:
            self.obj = Services.storage.getObject(self.oid)
        except StorageException, e:
            print " * anotar.py : Error creating object : ", e
            return e.getMessage()

        self.generate_id()
        self.modify_json()

        try:
            p = self.obj.createStoredPayload(self.pid, self.string_to_input_stream(self.json))
        except StorageException, e:
            print " * anotar.py : Error creating payload : ", e
            return e.getMessage()

        Services.indexer.annotate(self.oid, self.pid)
        return self.json

    def search_solr(self):
        query = "(rootUri:"
        if self.rootUriList:
            query += "(" + " OR ".join(self.rootUriList) + ")"
        else:
            query += "\"" + self.rootUri + "\""
        query += " AND type:\"" + self.type + "\")"
        print "**********", query

        req = SearchRequest(query)
        req.setParam("facet", "false")
        req.setParam("rows", str(99999))
        req.setParam("sort", "dateCreated asc")
        req.setParam("start", str(0))

        #security_roles = page.authentication.get_roles_list();
        #security_query = 'security_filter:("' + '" OR "'.join(security_roles) + '")'
        #req.addParam("fq", security_query)

        out = ByteArrayOutputStream()
        Services.indexer.annotateSearch(req, out)
        result = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
        result = result.getJsonList("response/docs")

        # Every annotation for this URI
        if self.type == "http://www.purl.org/anotar/ns/type/0.1#Tag":
            return self.process_tags(result)
        else:
            return self.process_response(result)

    def string_to_input_stream(self, inString):
        jString = String(inString)
        #print " * anotar.py : ", jString
        return ByteArrayInputStream(jString.getBytes("UTF-8"))

scriptObject = AnotarData()