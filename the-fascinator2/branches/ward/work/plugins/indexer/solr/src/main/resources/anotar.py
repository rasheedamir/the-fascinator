from au.edu.usq.fascinator.indexer.rules import AddField, New

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

# Read the payload json string into
# a JsonConfigHelper object.
json = pyUtils.getJsonObject(payload.open())
payload.close()

#start with blank solr document
rules.add(New())

rules.add(AddField("schemaVersion", json.get("schemaVersionUri")))
rules.add(AddField("clientVersion", json.get("clientVersionUri")))
rules.add(AddField("id", json.get("id")))
rules.add(AddField("uri", json.get("uri")))
rules.add(AddField("type", json.get("type")))
rules.add(AddField("titleLiteral", json.get("title/literal")))
rules.add(AddField("titleUri", json.get("title/uri")))
rules.add(AddField("annotatesLiteral", json.get("annotates/literal")))
rules.add(AddField("annotatesUri", json.get("annotates/uri")))
rules.add(AddField("rootUri", json.get("annotates/rootUri")))
rules.add(AddField("creatorLiteral", json.get("creator/literal")))
rules.add(AddField("creatorUri", json.get("creator/uri")))
rules.add(AddField("creatorEmail", json.get("creator/email/literal")))
rules.add(AddField("creatorEmailMd5", json.get("creator/email/md5hash")))
rules.add(AddField("contentType", json.get("content/mimeType")))
rules.add(AddField("contentLiteral", json.get("content/literal")))
rules.add(AddField("isPrivate", json.get("isPrivate")))
rules.add(AddField("lang", json.get("lang")))

# Date handling, Solr only accepts UTC
#http://lucene.apache.org/solr/api/org/apache/solr/schema/DateField.html
dateCreated = json.get("dateCreated/literal")
if dateCreated is not None:
    rules.add(AddField("dateCreated", dateCreated[:19] + "Z"))
    rules.add(AddField("tzCreated", dateCreated[19:]))
dateModified = json.get("dateModified/literal")
if dateModified is not None:
    rules.add(AddField("dateModified", dateModified[:19] + "Z"))
    rules.add(AddField("tzModified", dateModified[19:]))

# Arrays
for locator in json.getJsonList("annotates/locators"):
    rules.add(AddField("locators", locator.toString()))
    rules.add(AddField("locatorValue", locator.get("value")))
    rules.add(AddField("locatorContent", locator.get("originalContent")))

# Our full string
rules.add(AddField("jsonString", json.toString(False)))
