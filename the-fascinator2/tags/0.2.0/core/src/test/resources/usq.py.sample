import time
from au.edu.usq.fascinator.indexer.rules import AddField, CopyField, XslTransform

#
# Available objects:
#    indexer   : Indexer instance
#    rules     : RuleManager instance
#    object    : DigitalObject to index
#    payloadId : Payload identifier
#    storageId : Storage layer identifier
#

# dc to solr transform
rules.add(XslTransform(indexer.getResource("/xsl/DublinCoreToSolr.xsl")))

if isMetadata:
    solrId = object.getId();
    itemType = "object"
else:
    solrId = object.getId() + "/" + payloadId
    itemType = "datastream"
    rules.add(AddField("identifier", payloadId))

rules.add(AddField("id", solrId))
rules.add(AddField("last_modified", time.strftime("%Y-%m-%dT%H:%M:%SZ")))
rules.add(AddField("storageId", storageId))
rules.add(AddField("item_type", itemType))
rules.add(AddField("group_access", "guest"))
rules.add(AddField("item_class", "document"))

rules.add(AddField("repository_name", params["repository.name"]))
rules.add(AddField("repository_type", params["repository.type"]))

rules.add(CopyField("title", "dc.title"))
rules.add(CopyField("description", "dc.description"))

indexer.registerNamespace("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/")
indexer.registerNamespace("dc", "http://purl.org/dc/elements/1.1/")

dc = indexer.getXmlDocument(object.getMetadata())
titles = dc.selectNodes("//dc:title")
for title in titles:
    print " ***", title.getText()
