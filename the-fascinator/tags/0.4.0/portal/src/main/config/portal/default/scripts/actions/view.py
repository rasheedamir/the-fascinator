from au.edu.usq.fascinator.portal import Portal

class ViewActions:
    def __init__(self):
        print " * view.py: formData=%s" % formData
        result = "{}"
        portalManager = Services.getPortalManager()
        func = formData.get("func")
        if func == "create-view":
            id = formData.get("id")
            description = formData.get("description")
            fq = [q for q in sessionState.get("fq") if q != 'item_type:"object"']
            query = str(" OR ".join(fq))
            portal = Portal(id)
            portal.setDescription(description)
            portal.setQuery(query)
            portal.setFacetFields(portalManager.getDefault().getFacetFields())
            portalManager.add(portal)
            portalManager.save(portal)
            result = '{ url: "%s/%s/home" }' % (contextPath, id)
        elif func == "delete-view":
            print " * view.py: delete portal %s" % portalId
            Services.getPortalManager().remove(portalId)
        writer = response.getPrintWriter("text/plain")
        writer.println(result)
        writer.close()

scriptObject = ViewActions()
