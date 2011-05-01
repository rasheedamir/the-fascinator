
class HomeData:
    def __init__(self):
        action = formData.get("verb")
        portalName = formData.get("value")
        if action == "delete-portal":
            print " * home.py: delete portal %s" % portalName
            Services.portalManager.remove(portalName)
            sessionState.remove("fq")
        if action == "backup-portal":
            print "Backing up: ", portalName
            backupPathList = None
            email = ""
            portal = Services.portalManager.get(portalName)
            if portal:
                email = portal.email
                if portal.backupPaths:
                    backupPathList = portal.backupPaths
                portalQuery = portal.getQuery()
                #print " ***** portalQuery: ", portalQuery
                #print " ***** backupPathList: ", backupPathList
                #print " ***** email: ", email
                #print " ***** description: ", description
            if backupPathList is None:
                print " ** Please configure backup directory in the portal setting "
            else:
                Services.portalManager.backup(portal, email, backupPathList, portalQuery)
    
scriptObject = HomeData()
