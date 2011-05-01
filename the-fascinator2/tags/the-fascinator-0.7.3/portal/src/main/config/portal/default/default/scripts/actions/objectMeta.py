from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common import JsonConfigHelper

class ObjectMetaData:
    def __activate__(self, context):
        response = context["response"]
        json = JsonConfigHelper()
        auth = context["page"].authentication
        if auth.is_logged_in():
            formData = context["formData"]
            oid = formData.get("oid")
            if oid:
                # TODO check security on object
                json.set("oid", oid)
                try:
                    object = context["Services"].storage.getObject(oid)
                    json.setMap("meta", object.getMetadata())
                except StorageException:
                    response.setStatus(500)
                    json.set("error", "Object '%s' not found" % oid)
            else:
                response.setStatus(500)
                json.set("error", "An object identifier is required")
        else:
            response.setStatus(500)
            json.set("error", "Only registered users can access this API")
        
        writer = response.getPrintWriter("text/plain; charset=UTF-8")
        writer.println(json.toString())
        writer.close()
    
