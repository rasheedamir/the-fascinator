from au.edu.usq.fascinator.common import JsonConfigHelper

class IndexData:
    def __init__(self):
        pass

    def __activate__(self, context):
        # Prepare variables
        self.index = context["fields"]
        self.object = context["object"]
        self.payload = context["payload"]
        self.params = context["params"]
        self.utils = context["pyUtils"]
        self.indexer = context["indexer"]

        # Common data
        self.__newDoc()
        
        ##self.__security()

    def __newDoc(self):
        # Read the payload json string into
        # a JsonConfigHelper object.
        self.oid = self.object.getId()
        self.pid = self.payload.getId()
        
        json = self.utils.getJsonObject(self.payload.open())
        self.payload.close()

        self.utils.add(self.index, "schemaVersion", json.get("schemaVersionUri"))  ##
        self.utils.add(self.index, "clientVersion", json.get("clientVersionUri"))  ##
        self.utils.add(self.index, "id", json.get("id"))  ##
        self.utils.add(self.index, "uri", json.get("uri"))  ##
        self.utils.add(self.index, "type", json.get("type"))  ##
        self.utils.add(self.index, "titleLiteral", json.get("title/literal", ""))
        self.utils.add(self.index, "titleUri", json.get("title/uri", ""))
        self.utils.add(self.index, "annotatesLiteral", json.get("annotates/literal", "")) 
        self.utils.add(self.index, "annotatesUri", json.get("annotates/uri"))  ##
        self.utils.add(self.index, "rootUri", json.get("annotates/rootUri"))  ##
        self.utils.add(self.index, "creatorLiteral", json.get("creator/literal", "")) 
        self.utils.add(self.index, "creatorUri", json.get("creator/uri"))  ##
        self.utils.add(self.index, "creatorEmail", json.get("creator/email/literal", ""))
        self.utils.add(self.index, "creatorEmailMd5", json.get("creator/email/md5hash", ""))
        self.utils.add(self.index, "contentType", json.get("content/mimeType"))  ##
        self.utils.add(self.index, "contentLiteral", json.get("content/literal"))  ##
        self.utils.add(self.index, "isPrivate", json.get("isPrivate"))  ##
        self.utils.add(self.index, "lang", json.get("lang"))  ##

        # Date handling, Solr only accepts UTC
        #http://lucene.apache.org/solr/api/org/apache/solr/schema/DateField.html
        dateCreated = json.get("dateCreated/literal")
        if dateCreated is not None:
            self.utils.add(self.index, "dateCreated", dateCreated[:19] + "Z")  ##
            self.utils.add(self.index, "tzCreated", dateCreated[19:])  ##
        dateModified = json.get("dateModified/literal")
        if dateModified is not None:
            self.utils.add(self.index, "dateModified", dateModified[19:] + "Z")
            self.utils.add(self.index, "tzModified", dateModified[19:])
            
        # Arrays
        for locator in json.getJsonList("annotates/locators"):
            self.utils.add(self.index, "locators", locator.toString())  ##
            self.utils.add(self.index, "locatorValue", locator.get("value"))  ##
            self.utils.add(self.index, "locatorContent", locator.get("originalContent"))  ##

        # Our full string
        self.utils.add(self.index, "jsonString", json.toString(False))  ##


    def __security(self):
        roles = self.utils.getRolesWithAccess(self.oid)
        if roles is not None:
            for role in roles:
                self.utils.add(self.index, "security_filter", role)
        else:
            # Default to guest access if Null object returned
            schema = self.utils.getAccessSchema("derby");
            schema.setRecordId(self.oid)
            schema.set("role", "guest")
            self.utils.setAccessSchema(schema, "derby")
            self.utils.add(self.index, "security_filter", "guest")