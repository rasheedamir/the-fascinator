import time
from au.edu.usq.fascinator.indexer.rules import AddField, New
from org.dom4j.io import SAXReader

#
# Available objects:
#    indexer   : Indexer instance
#    rules     : RuleManager instance
#    object    : DigitalObject to index
#    payloadId : Payload identifier
#    storageId : Storage layer identifier
#

#def indexNode(doc, name, xPath="", value=""):
#    if value:
#        rules.add(AddField(name, value))
#        return value
#    node = doc.selectSingleNodes(xPath)
#    if node is not None and len(node)==1:
#        rules.add(AddField(name, node.getText()))
#        return node.getText()
#    return ""

def indexNodes(doc, names, xPath="", value=[]):
    nodes = doc.selectNodes(xPath)
    valueList = []
    if nodes:
        for node in nodes:
            for name in names: 
                rules.add(AddField(name, node.getText()))
            valueList.append(node.getText())
    return valueList

rules.add(New())

if isMetadata:
    solrId = object.getId();
    itemType = "object"
else:
    solrId = object.getId() + "/" + payloadId
    itemType = "datastream"
    rules.add(AddField("identifier", payloadId))

# path
path = object.getId().replace("\\", "/")
parts = path.split("/")
for i in range(1, len(parts)):
    part = "/".join(parts[:i])
    if part != "":
        if part.startswith("/"):
            part = part[1:]
        rules.add(AddField("file_path", part))

rules.add(AddField("id", solrId))
rules.add(AddField("last_modified", time.strftime("%Y-%m-%dT%H:%M:%SZ")))
rules.add(AddField("storageId", storageId))
rules.add(AddField("item_type", itemType))
rules.add(AddField("group_access", "guest"))
rules.add(AddField("item_class", "document"))

rules.add(AddField("repository_name", params["repository.name"]))
rules.add(AddField("repository_type", params["repository.type"]))

indexer.registerNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
indexer.registerNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
indexer.registerNamespace("dc", "http://purl.org/dc/elements/1.1/")
indexer.registerNamespace("foaf", "http://xmlns.com/foaf/0.1/")
indexer.registerNamespace("openofficens", "urn:oasis:names:tc:opendocument:xmlns:meta:1.0/")
indexer.registerNamespace("nie", "http://www.semanticdesktop.org/ontologies/2007/01/19/nie#")
indexer.registerNamespace("nco", "http://www.semanticdesktop.org/ontologies/2007/03/22/nco#")
indexer.registerNamespace("nfo", "http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#")
indexer.registerNamespace("dcterms", "http://purl.org/dc/terms/")


rdf = indexer.getXmlDocument(object.getMetadata())
if rdf is not None:
    rootNode = rdf.getRootElement()
    #indexNode(rootNode, "title", "./rdf:Description/dc:title")
else:
    print " * No RDF metadata found!"

#rdfString = object.getMetadata()
#if rdfString is None:
#    rdfString = object.getPayload("rdf")  #in future use object.getMetadata("rdf")

#rootNode = indexer.getXmlDocument(rdfString.getInputStream())
#rootNode = indexer.getXmlDocument(object.getMetadata())

   
#if rdf is not None:
#    title = indexNode(rootNode, "dc_title", "/rdf:RDF/rdf:Description/dcterms:title")
#    if title=="":
#        title = indexNode(rootNode, "dc_title", "/rdf:RDF/rdf:Description/dc:title")
#    if title=="":
#        title = indexNode(rootNode, "dc_title", "/rdf:RDF/rdf:Description/nie:title")
#    if title:
#        indexNode(rootNode, "dc_subject", value=title)

if isMetadata:
    indexer.registerNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
    indexer.registerNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
    indexer.registerNamespace("dc", "http://purl.org/dc/elements/1.1/")
    indexer.registerNamespace("foaf", "http://xmlns.com/foaf/0.1/")
    indexer.registerNamespace("openofficens", "urn:oasis:names:tc:opendocument:xmlns:meta:1.0/")
    indexer.registerNamespace("nie", "http://www.semanticdesktop.org/ontologies/2007/01/19/nie#")
    indexer.registerNamespace("nco", "http://www.semanticdesktop.org/ontologies/2007/03/22/nco#")
    indexer.registerNamespace("nfo", "http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#")
    indexer.registerNamespace("dcterms", "http://purl.org/dc/terms/")

    rdf = indexer.getXmlDocument(object.getMetadata())
    if rdf is not None:
        rootNode = rdf.getRootElement()
        #indexNode(rootNode, "title", "./rdf:Description/dc:title")
    else:
        print " * No RDF metadata found!"
    
    #rdfString = object.getMetadata()
    #if rdfString is None:
    #    rdfString = object.getPayload("rdf")  #in future use object.getMetadata("rdf")
    
    #rootNode = indexer.getXmlDocument(rdfString.getInputStream())
    #rootNode = indexer.getXmlDocument(object.getMetadata())
    
       
    if rdf is not None:
        title = indexNodes(rootNode, ["dc_title", "dc_subject"], "//rdf:RDF/rdf:Description/dcterms:title")
        if title == []:
            title = indexNodes(rootNode, ["dc_title", "dc_subject"], "//rdf:RDF/rdf:Description/dc:title")
        if title == []:
            title = indexNodes(rootNode, ["dc_title", "dc_subject"], "//rdf:RDF/rdf:Description/nie:title")
            
        dcDate = indexNodes(rootNode, ["dc_date"], "//rdf:RDF/rdf:Description/dcterms:date")
        if dcDate:
            dcDate = indexNodes(rootNode, ["dc_date"], "//rdf:RDF/rdf:Description/dc:date")
            
        creator = indexNodes(rootNode, ["dc_creator", "dc_author"], "//rdf:RDF/rdf:Description/nco:fullname")
        creator = indexNodes(rootNode, ["dc_creator", "dc_author"], "//rdf:RDF/rdf:Description/dc:creator")
        creator = indexNodes(rootNode, ["dc_creator", "dc_author"], "//rdf:RDF/rdf:Description/openofficens:initial-creator")
        creator = indexNodes(rootNode, ["dc_creator", "dc_author"], "//rdf:RDF/rdf:Description/dcterms:creator")
        
        format = indexNodes(rootNode, ["dc_format"], "//rdf:RDF/rdf:Description/dcterms:format")
        if format=="":
            format = indexNodes(rootNode, ["dc_format"], "//rdf:RDF/rdf:Description/nie:mimeType")
        
        indexNodes(rootNode, ["full_text"], "//rdf:RDF/rdf:Description/nie:plainTextContent")
        indexNodes(rootNode, ["dc_creationdate"], "//rdf:RDF/rdf:Description/openofficens:creation-date")












