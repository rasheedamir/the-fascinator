from au.edu.usq.fascinator.api.indexer import SearchRequest
from au.edu.usq.fascinator.common import JsonConfigHelper

from java.io import ByteArrayInputStream, ByteArrayOutputStream
from java.util import HashSet

class Reharvest:
    def __init__(self):
        print "formData=%s" % formData
        func = formData.get("func")
        result = "{}"
        resultType = "text/plain; charset=UTF-8"
        oid = formData.get("oid")
        portalId = formData.get("portalId")
        portalManager = Services.getPortalManager()
        if func == "reharvest":
            if oid:
                print "Reharvesting single object: %s" % oid
                portalManager.reharvest(oid)
                result = '{ status: "ok" }'
            elif portalId:
                portal = portalManager.get(portalId)
                print " Reharvesting portal: %s" % portal.getName()
                indexer = Services.getIndexer()
                # TODO security filter
                # TODO this should loop through the whole portal,
                #      not just the first page of results
                if portal.getQuery() == "":
                    searchRequest = SearchRequest("item_type:object")
                else:
                    searchRequest = SearchRequest(portal.getQuery())
                result = ByteArrayOutputStream();
                Services.getIndexer().search(searchRequest, result)
                json = JsonConfigHelper(ByteArrayInputStream(result.toByteArray()))
                objectIds = HashSet()
                for doc in json.getJsonList("response/docs"):
                    objectIds.add(doc.get("id"))
                if not objectIds.isEmpty():
                    portalManager.reharvest(objectIds)
                result = '{ status: "ok" }'
            else:
                result = '{ status: "failed" }'
        elif func == "get-state":
            result = '{ running: "%s", lastResult: "%s" }' % \
                (sessionState.get("reharvest/running"),
                 sessionState.get("reharvest/lastResult"))
        elif func == "get-log":
            context = LoggerFactory.getILoggerFactory()
            logger = context.getLogger("au.edu.usq.fascinator.HarvestClient")
            appender = logger.getAppender("CYCLIC")
            layout = HTMLLayout()
            layout.setContext(context)
            layout.setPattern("%d%msg")
            layout.setTitle("Reharvest log")
            layout.start()
            result = "<table>"
            count = appender.getLength()
            if count == -1:
                result += "<tr><td>Failed</td></tr>"
            elif count == 0:
                result += "<tr><td>No logging events</td></tr>"
            else:
                for i in range(0, count):
                    event = appender.get(i)
                    result += layout.doLayout(event)
            result += "</table>"
            resultType = "text/html; charset=UTF-8"
        writer = response.getPrintWriter(resultType)
        writer.println(result)
        writer.close()

scriptObject = Reharvest()
