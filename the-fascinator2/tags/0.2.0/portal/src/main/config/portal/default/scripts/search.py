import array, md5, os

from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.api.storage import Payload, PayloadType
from au.edu.usq.fascinator.api import PluginManager
from au.edu.usq.fascinator.common import JsonConfig, JsonConfigHelper
from au.edu.usq.fascinator.common.storage.impl import GenericPayload
from au.edu.usq.fascinator.portal import Pagination, Portal

from java.io import ByteArrayInputStream, ByteArrayOutputStream
from java.net import URLDecoder, URLEncoder
from java.util import HashMap

from au.edu.usq.fascinator.common.ctag import Tag, TaggedContent

from org.ontoware.rdf2go import RDF2Go
from org.ontoware.rdf2go.model import Model, Syntax
from org.ontoware.rdf2go.model.node import Node, URI

class TagsPayload(GenericPayload):
    def __init__(self, payload, oid):
        self.__tags = []
        self.__model = RDF2Go.getModelFactory().createModel();
        self.__model.open();
        self.__content = TaggedContent(self.__model, "urn:" + md5.new(oid).hexdigest(), True);
        if payload is None:
            self.setId("tags.rdf")
            self.setLabel("Tags")
            self.setContentType("application/rdf+xml")
            self.setType(PayloadType.Annotation)
        else:
            GenericPayload.__init__(self, payload)
            self.__model.readFrom(payload.getInputStream())
            tags = self.__content.getAllTagged_as().asArray()
            for tag in tags:
                labels = tag.getAllTaglabel_asNode_().asArray()
                for label in labels:
                    print " * search.py: Tag Label - %s" % label
                    self.__tags.append(label)
        self.setType(PayloadType.Annotation)
    
    def addTag(self, tag):
        encodedTag = URLEncoder.encode(tag, "UTF-8")
        tagNode = Tag(self.__model, "urn:tags:" + encodedTag, True)
        tagNode.setMeans(self.__model.createURI("urn:tags:" + encodedTag));
        tagNode.setTaglabel(self.__model.createPlainLiteral(tag))
        self.__content.addTagged(tagNode);
    
    def removeTag(self, tag):
        print " search.py: Remove Tag: %s [TODO]" % tag
    
    def getTags(self):
        return self.__tags
    
    def getInputStream(self):
        rdf = self.__model.serialize(Syntax.RdfXml)
        print rdf
        return ByteArrayInputStream(array.array('b', rdf.encode("UTF-8")))
    
    def close(self):
        self.__model.close()

class SearchData:
    def __init__(self):
        self.__tags = []
        if formData.get("verb") == "load-tags":
            self.__loadTags()
        else:
            self.__portal = Services.portalManager.get(portalId)
            self.__result = JsonConfigHelper()
            self.__pageNum = sessionState.get("pageNum", 1)
            self.__selected = []
            if formData.get("verb") == "tag":
                self.__tag()
            self.__search()
    
    def hasTags(self):
        return len(self.__tags) > 0
    
    def getTags(self):
        return self.__tags
    
    def __loadTags(self):
        oid = formData.get("oid")
        obj = Services.storage.getObject(oid)
        tagsPayload = TagsPayload(obj.getPayload("tags.rdf"), oid)
        self.__tags = tagsPayload.getTags()
        print " * search.py: Loaded tags from %s: %s" % (oid, self.__tags)
        tagsPayload.close()
    
    def __tag(self):
        oid = formData.get("oid")
        newTag = formData.get("newTag")
        print " * search.py: Tagging '%s' with '%s'" % (oid, newTag)
        # add tag to storage
        obj = Services.storage.getObject(oid)
        tagsPayload = TagsPayload(obj.getPayload("tags.rdf"), oid)
        tagsPayload.addTag(newTag)
        Services.storage.addPayload(oid, tagsPayload)
        self.__tags = tagsPayload.getTags()
        tagsPayload.close()
        # now re-index the tag
        Services.indexer.index(oid)
    
    def __search(self):
        recordsPerPage = self.__portal.recordsPerPage
        
        query = formData.get("query")
        if query is None or query == "":
            query = "*:*"
        req = SearchRequest(query)
        req.setParam("facet", "true")
        req.setParam("rows", str(recordsPerPage))
        req.setParam("facet.field", self.__portal.facetFieldList)
        req.setParam("facet.sort", "true")
        req.setParam("facet.limit", str(self.__portal.facetCount))
        req.setParam("sort", "f_dc_title asc")
        
        # setup facets
        action = formData.get("verb")
        value = formData.get("value")
        fq = sessionState.get("fq")
        if fq is not None:
            self.__pageNum = 1
            req.setParam("fq", fq)
        if action == "add_fq":
            self.__pageNum = 1
            name = formData.get("name")
            print " * add_fq: %s" % value
            req.addParam("fq", URLDecoder.decode(value, "UTF-8"))
        elif action == "remove_fq":
            self.__pageNum = 1
            req.removeParam("fq", URLDecoder.decode(value, "UTF-8"))
        elif action == "clear_fq":
            self.__pageNum = 1
            req.removeParam("fq")
        elif action == "select-page":
            self.__pageNum = int(value)
        req.addParam("fq", 'item_type:"object"')
        
        portalQuery = self.__portal.query
        print " * portalQuery=%s" % portalQuery
        if portalQuery:
            req.addParam("fq", portalQuery)
        
        self.__selected = req.getParams("fq")
        
        sessionState.set("fq", self.__selected)
        sessionState.set("pageNum", self.__pageNum)
        
        req.setParam("start", str((self.__pageNum - 1) * recordsPerPage))
        
        print " * search.py:", req.toString(), self.__pageNum
        
        out = ByteArrayOutputStream()
        Services.indexer.search(req, out)
        self.__result = JsonConfigHelper(ByteArrayInputStream(out.toByteArray()))
        if self.__result is not None:
            self.__paging = Pagination(self.__pageNum,
                                       int(self.__result.get("response/numFound")),
                                       self.__portal.recordsPerPage)
    
    def getQueryTime(self):
        return int(self.__result.get("responseHeader/QTime")) / 1000.0;
    
    def getPaging(self):
        return self.__paging
    
    def getResult(self):
        return self.__result
    
    def getFacetName(self, key):
        return self.__portal.facetFields.get(key)
    
    def getFacetCounts(self, key):
        values = HashMap()
        valueList = self.__result.getList("facet_counts/facet_fields/%s" % key)
        for i in range(0,len(valueList),2):
            name = valueList[i]
            count = valueList[i+1]
            if count > 0:
                values.put(name, count)
        return values
    
    def hasSelectedFacets(self):
        return self.__selected is not None and self.__selected.size() > 1
    
    def getSelectedFacets(self):
        return self.__selected
    
    def isPortalQueryFacet(self, fq):
        return fq == self.__portal.query
    
    def isSelected(self, fq):
        return fq in self.__selected
    
    def getSelectedFacetIds(self):
        return [md5.new(fq).hexdigest() for fq in self.__selected]
    
    def getFileName(self, path):
        return os.path.split(path)[1]
    
    def getFacetQuery(self, name, value):
        return '%s:"%s"' % (name, value)
    
    def isImage(self, format):
        return format.startswith("image/")
    
    def getThumbnail(self, oid):
        ext = os.path.splitext(oid)[1]
        url = oid[oid.rfind("/")+1:-len(ext)] + ".thumb.jpg"
        return url

scriptObject = SearchData()
