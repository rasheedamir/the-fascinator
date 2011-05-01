from au.edu.usq.fascinator.common import JsonConfigHelper
from au.edu.usq.fascinator.portal import Portal
from java.lang import Exception

class ViewData:
    def __activate__(self, context):
        response = context["response"]
        writer = response.getPrintWriter("text/plain; charset=UTF-8")
        auth = context["page"].authentication
        result = JsonConfigHelper()
        result.set("status", "error")
        result.set("message", "An unknown error has occurred")
        if auth.is_logged_in():
            services = context["Services"]
            formData = context["formData"]
            contextPath = context["contextPath"]
            sessionState = context["sessionState"]
            func = formData.get("func")
            portalManager = services.portalManager
            if func == "create-view":
                try:
                    fq = [q for q in sessionState.get("fq") if q != 'item_type:"object"']
                    id = formData.get("id")
                    description = formData.get("description")
                    print "Creating view '%s': '%s'" % (id, description)
                    portal = Portal(id)
                    portal.setDescription(formData.get("description"))
                    portal.setQuery(" OR ".join(fq))
                    portal.setSearchQuery(sessionState.get("searchQuery"))
                    portal.setFacetFields(portalManager.default.facetFields)
                    portalManager.add(portal)
                    portalManager.save(portal)
                    result.set("status", "ok")
                    result.set("message", "View '%s' successfully created" % id)
                    result.set("url", "%s/%s/home" % (contextPath, id))
                except Exception, e:
                    response.setStatus(500)
                    result.set("message", str(e))
            elif func == "delete-view":
                defaultPortal = context["defaultPortal"]
                portalId = formData.get("view")
                if auth.is_admin():
                    if not portalId:
                        response.setStatus(500)
                        result.set("message", "No view specified to be deleted")
                    elif portalId != defaultPortal:
                        # sanity check: don't delete default portal
                        print "Deleting view '%s'" % portalId
                        try:
                            portalManager.remove(portalId)
                            result.set("status", "ok")
                            result.set("message", "View '%s' successfully removed" % portalId)
                            result.set("url", "%s/%s/home" % (contextPath, defaultPortal))
                        except Exception, e:
                            result.set("message", str(e))
                    else:
                        response.setStatus(500)
                        result.set("message", "The default view cannot be deleted")
                else:
                    response.setStatus(403)
                    result.set("message", "Only administrative users can access this API")
            else:
                response.setStatus(500)
                result.set("message", "Unknown action '%s'" % func)
        else:
            response.setStatus(403)
            result.set("message", "Only registered users can access this API")
        
        writer.println(result.toString())
        writer.close()
    