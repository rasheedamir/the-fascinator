from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.common import JsonObject
from au.edu.usq.fascinator.common.solr import SolrResult

from java.io import ByteArrayInputStream, ByteArrayOutputStream

class ReharvestData:
    def __activate__(self, context):
        response = context["response"]
        writer = response.getPrintWriter("text/plain; charset=UTF-8")
        auth = context["page"].authentication
        result = JsonObject()
        result.put("status", "error")
        result.put("message", "An unknown error has occurred")
        if auth.is_logged_in() and auth.is_admin():
            services = context["Services"]
            formData = context["formData"]
            func = formData.get("func")
            oid = formData.get("oid")
            portalId = formData.get("portalId")
            portalManager = services.portalManager
            if func == "reharvest":
                if oid:
                    print "Reharvesting object '%s'" % oid
                    portalManager.reharvest(oid)
                    result.put("status", "ok")
                    result.put("message", "Object '%s' queued for reharvest")
                elif portalId:
                    print " Reharvesting view '%s'" % portalId
                    # TODO security filter
                    # TODO this should loop through the whole portal,
                    #      not just the first page of results
                    portal = portalManager.get(portalId)
                    req = SearchRequest(portal.query)
                    req.setParam("fq", 'item_type:"object"')
                    out = ByteArrayOutputStream();
                    services.indexer.search(req, out)
                    json = SolrResult(ByteArrayInputStream(out.toByteArray()))
                    objectIds = json.getFieldList("id")
                    if not objectIds.isEmpty():
                        portalManager.reharvest(objectIds)
                    result.put("status", "ok")
                    result.put("message", "Objects in '%s' queued for reharvest" % portalId)
                else:
                    response.setStatus(500)
                    result.put("message", "No object or view specified for reharvest")
            elif func == "reindex":
                if oid:
                    print "Reindexing object '%s'" % oid
                    services.indexer.index(oid)
                    services.indexer.commit()
                    result.put("status", "ok")
                    result.put("message", "Object '%s' queued for reindex" % portalId)
                else:
                    response.setStatus(500)
                    result.put("message", "No object specified to reindex")
            else:
                response.setStatus(500)
                result.put("message", "Unknown action '%s'" % func)
        else:
            response.setStatus(500)
            result.put("message", "Only administrative users can access this API")
        writer.println(result.toString())
        writer.close()
