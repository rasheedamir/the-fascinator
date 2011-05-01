import time
from au.edu.usq.fascinator.api.storage import PayloadType
from au.edu.usq.fascinator.indexer.rules import AddField, CopyField, New, XslTransform

#
# Available objects:
#    indexer    : Indexer instance
#    jsonConfig : JsonConfigHelper of our harvest config file
#    rules      : RuleManager instance
#    object     : DigitalObject to index
#    payload    : Payload to index
#    params     : Metadata Properties object
#    pyUtils    : Utility object for accessing app logic
#

pyUtils.registerNamespace("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/")
pyUtils.registerNamespace("dc", "http://purl.org/dc/elements/1.1/")

oid = object.getId()
pid = payload.getId()
metaPid = params.getProperty("metaPid")
solrId = params.getProperty("fedoraPid")

if pid == metaPid:
    for payloadId in object.getPayloadIdList():
        try:
            payload = object.getPayload(payloadId)
            if str(payload.getType())=="Thumbnail":
                rules.add(AddField("thumbnail", payload.getId()))
            elif str(payload.getType())=="Preview":
                rules.add(AddField("preview", payload.getId()))
            elif str(payload.getType())=="AltPreview":
                rules.add(AddField("altpreview", payload.getId()))
        except Exception, e:
            pass

    #XslTransform is currently broken - so just process the XML directly
    #rules.add(XslTransform(pyUtils.getResource("/xsl/DublinCoreToSolr.xsl")))

    #dcPayload = object.getPayload(object.getSourceId())
    dcPayload = object.getPayload("DC")
    dc = pyUtils.getXmlDocument(dcPayload.open())
    dcPayload.close()
    nodes = dc.selectNodes("//dc:*")
    for node in nodes:
        name = "dc_" + node.getName()
        text = node.getTextTrim()
        rules.add(AddField(name, text))
    itemType = "object"
else:
    rules.add(New())
    oid += "/" + pid
    solrId = oid
    itemType = "datastream"
    rules.add(AddField("identifier", pid))

rules.add(AddField("id", solrId))
rules.add(AddField("storage_id", oid))
rules.add(AddField("last_modified", time.strftime("%Y-%m-%dT%H:%M:%SZ")))
rules.add(AddField("item_type", itemType))
rules.add(AddField("harvest_config", params.getProperty("jsonConfigOid")))
rules.add(AddField("harvest_rules",  params.getProperty("rulesOid")))
rules.add(AddField("modified", params.getProperty("modified", "false")))

# Security
roles = pyUtils.getRolesWithAccess(oid)
if roles is not None:
    for role in roles:
        rules.add(AddField("security_filter", role))
else:
    # Default to guest access if Null object returned
    schema = pyUtils.getAccessSchema("derby");
    schema.setRecordId(oid)
    schema.set("role", "guest")
    pyUtils.setAccessSchema(schema, "derby")
    rules.add(AddField("security_filter", "guest"))

rules.add(AddField("repository_name", params["repository.name"]))
rules.add(AddField("repository_type", params["repository.type"]))

rules.add(CopyField("title", "dc_title"))
rules.add(CopyField("description", "dc_description"))