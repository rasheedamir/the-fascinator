from ch.qos.logback.classic.html import  HTMLLayout
from org.slf4j import Logger, LoggerFactory

class Reindex:
    def __init__(self):
        func = formData.get("func")
        result = "{}"
        resultType = "text/plain"
        if func == "reindex":
            file = formData.get("file")
            portalId = formData.get("portalId")
            portalManager = Services.getPortalManager()
            if file:
                print " * Reindexing: formData=%s" % file
                portalManager.indexObject(file)
                sessionState.set("reindex/lastResult", "success")
                result = '{ status: "ok" }'
            elif portalId:
                portal = portalManager.get(portalId)
                print " * Reindexing: Portal=%s" % portal.name
                portalManager.indexPortal(portal)
                sessionState.set("reindex/lastResult", "success")
                result = '{ status: "ok" }'
            else:
                sessionState.set("reindex/lastResult", "failed")
                result = '{ status: "failed" }'
        elif func == "get-state":
            result = '{ running: "%s", lastResult: "%s" }' % \
                (sessionState.get("reindex/running"),
                 sessionState.get("reindex/lastResult"))
        elif func == "get-log":
            context = LoggerFactory.getILoggerFactory()
            logger = context.getLogger("au.edu.usq.fascinator.IndexClient")
            it = logger.iteratorForAppenders()
            appender = logger.getAppender("reindex")
            layout = HTMLLayout()
            layout.setContext(context)
            layout.setPattern("%d%level%msg")
            layout.setTitle("Index log")
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
            resultType = "text/html"
        writer = response.getPrintWriter(resultType)
        writer.println(result)
        writer.close()
                
scriptObject = Reindex()
