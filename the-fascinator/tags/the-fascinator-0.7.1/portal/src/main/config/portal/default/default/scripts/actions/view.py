from au.edu.usq.fascinator.portal import Portal

class ViewData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.vc = context
        print " * view.py: formData=%s" % self.vc["formData"]
        result = "{}"
        portalManager = Services.getPortalManager()
        func = self.vc["formData"].get("func")
        if func == "create-view":
            id = self.vc["formData"].get("id")
            description = self.vc["formData"].get("description")
            fq = [q for q in self.vc["sessionState"].get("fq") if q != 'item_type:"object"']
            query = str(" OR ".join(fq))
            searchQuery = self.vc["sessionState"].get("searchQuery")
            portal = Portal(id)
            portal.setDescription(description)
            portal.setQuery(query)
            portal.setSearchQuery(searchQuery)
            portal.setFacetFields(portalManager.getDefault().getFacetFields())
            portalManager.add(portal)
            portalManager.save(portal)
            result = '{ url: "%s/%s/home" }' % (contextPath, id)
        elif func == "delete-view":
            print " * view.py: delete portal %s" % self.vc["portalId"]
            Services.getPortalManager().remove(self.vc["portalId"])
        writer = self.vc["response"].getPrintWriter("text/plain; charset=UTF-8")
        writer.println(result)
        writer.close()
