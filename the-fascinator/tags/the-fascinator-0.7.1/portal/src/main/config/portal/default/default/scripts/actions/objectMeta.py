from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common import JsonConfigHelper

class ObjectMetaData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        #print "formData=%s" % formData
        json = JsonConfigHelper()
        oid = self.velocityContext["formData"].get("oid")
        if oid:
            # TODO access checking on the object
            json.set("oid", oid)
            try:
                object = Services.getStorage().getObject(oid)
                meta = object.getMetadata()
                json.setMap("meta", meta)
            except StorageException, se:
                json.set("error", "Object '%s' not found!" % oid)
        else:
            json.set("error", "An object identifier is required!")
        writer = self.velocityContext["response"].getPrintWriter("text/plain; charset=UTF-8")
        writer.println(json.toString())
        writer.close()
