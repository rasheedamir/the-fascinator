from au.edu.usq.fascinator.portal import Portal

class StateActions:
    def __init__(self):
        print " * state.py: formData=%s" % formData
        result = "{}"
        portalManager = Services.getPortalManager()
        portal = portalManager.get(portalId)
        func = formData.get("func")
        name = formData.get("name")
        if func == "set":
            value = formData.get("value")
            sessionState.set(name, value)
            result = '{ name: "%s", value: "%s" }' % (name, value)
        elif func == "get":
            value = sessionState.get(name)
            result = '{ value: "%s" }' % value
        elif func == "remove":
            value = sessionState.get(name)
            sessionState.remove(name)
            result = '{ value: "%s" }' % value
        writer = response.getPrintWriter("text/plain; charset=UTF-8")
        writer.println(result)
        writer.close()

scriptObject = StateActions()
