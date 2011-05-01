import array, md5

from au.edu.usq.fascinator.api.storage import PayloadType
from au.edu.usq.fascinator.common.ctag import Tag, TaggedContent
from au.edu.usq.fascinator.common.storage.impl import GenericPayload

from java.io import ByteArrayInputStream
from java.net import URLEncoder

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
                    self.__tags.append(label)
        self.setType(PayloadType.Annotation)
    
    def addTag(self, tag):
        tagNode = self.__createTag(tag)
        self.__content.addTagged(tagNode);
    
    def removeTag(self, tag):
        tagNode = self.__createTag(tag)
        self.__content.removeTagged(tagNode);
    
    def __createTag(self, tag):
        encodedTag = URLEncoder.encode(tag, "UTF-8")
        tagNode = Tag(self.__model, "urn:tags:" + encodedTag, True)
        tagNode.setMeans(self.__model.createURI("urn:tags:" + encodedTag));
        tagNode.setTaglabel(self.__model.createPlainLiteral(tag))
        return tagNode
    
    def getTags(self):
        return self.__tags
    
    def getInputStream(self):
        rdf = self.__model.serialize(Syntax.RdfXml)
        return ByteArrayInputStream(array.array('b', rdf.encode("UTF-8")))
    
    def close(self):
        self.__model.close()

class TagData:
    def __init__(self):
        tags = []
        func = formData.get("func")
        oid = formData.get("oid")
        tag = formData.get("tag")
        if func == "tags-add":
            tags = self.__add(oid, tag)
        elif func == "tags-remove":
            tags = self.__remove(oid, tag)
        elif func == "tags-load":
            tags = self.__load(oid)
        writer = response.getPrintWriter("text/plain; charset=UTF-8")
        writer.println("{tags: %s}" % str(["%s" % t for t in tags]))
        writer.close()
    
    def __add(self, oid, tag):
        print " * tags.py: Tagging '%s' with '%s'" % (oid, tag)
        p = self.__getPayload(oid)
        p.addTag(tag)
        tags = p.getTags()
        Services.storage.addPayload(oid, p)
        p.close()
        Services.indexer.index(oid)
        return tags
    
    def __remove(self, oid, tag):
        print " * tags.py: Removing '%s' from '%s'" % (tag, oid)
        p = self.__getPayload(oid)
        p.removeTag(tag)
        tags = p.getTags()
        Services.storage.addPayload(oid, p)
        p.close()
        Services.indexer.index(oid)
        return tags
    
    def __load(self, oid):
        p = self.__getPayload(oid)
        tags = p.getTags()
        p.close()
        print " * tags.py: Loaded tags from %s: %s" % (oid, tags)
        return tags
    
    def __getPayload(self, oid):
        obj = Services.storage.getObject(oid)
        return TagsPayload(obj.getPayload("tags.rdf"), oid)

scriptObject = TagData()
