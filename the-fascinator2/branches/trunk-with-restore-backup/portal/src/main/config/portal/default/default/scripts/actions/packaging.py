import md5, uuid

from au.edu.usq.fascinator import HarvestClient
from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common import FascinatorHome, JsonConfigHelper
from au.edu.usq.fascinator.common.storage import StorageUtils

from java.io import File, FileOutputStream, InputStreamReader, OutputStreamWriter
from java.lang import Exception

from org.apache.commons.io import IOUtils

class PackagingActions:
    
    def __init__(self):
        print "formData=%s" % formData

        result = "{}"
        func = formData.get("func")
        if func == "create-from-selected":
            result = self.__createFromSelected()
        elif func == "update":
            result = self.__update()
        elif func == "clear":
            result = self.__clear()
        elif func == "modify":
            result = self.__modify()
        elif func == "add-custom":
            result = self.__addCustom()
        
        writer = response.getPrintWriter("application/json; charset=UTF-8")
        writer.println(result)
        writer.close()
    
    def __createFromSelected(self):
        print "Creating package from selected..."
        
        # if modifying existing manifest, we already have an identifier,
        # otherwise create a new one
        manifestId = self.__getActiveManifestId()
        if manifestId is None:
            manifestHash = "%s.tfpackage" % uuid.uuid4()
        else:
            manifestHash = self.__getActiveManifestPid()
        
        # store the manifest file for harvesting
        packageDir = FascinatorHome.getPathFile("packages")
        packageDir.mkdirs()
        manifestFile = File(packageDir, manifestHash)
        outStream = FileOutputStream(manifestFile)
        outWriter = OutputStreamWriter(outStream, "UTF-8")
        manifest = self.__getActiveManifest()
        manifest.store(outWriter, True)
        outWriter.close()
        
        try:
            if manifestId is None:
                # harvest the package as an object
                username = sessionState.get("username")
                if username is None:
                    username = "guest" # necessary?
                harvester = None
                # set up config files if necessary
                workflowsDir = FascinatorHome.getPathFile("harvest/workflows")
                configFile = self.__getFile(workflowsDir, "packaging-config.json")
                rulesFile = self.__getFile(workflowsDir, "packaging-rules.py")
                # run the harvest client with our packaging workflow config
                harvester = HarvestClient(configFile, manifestFile, username)
                harvester.start()
                manifestId = harvester.getUploadOid()
                harvester.shutdown()
            else:
                # update existing object
                object = StorageUtils.storeFile(Services.getStorage(), manifestFile)
                object.close()
        except Exception, ex:
            error = "Packager workflow failed: %s" % str(ex)
            log.error(error, ex)
            if harvester is not None:
                harvester.shutdown()
            return '{ status: "failed" }'
        # clean up
        ##manifestFile.delete()
        self.__clear()
        # return url to workflow screen
        return '{ status: "ok", url: "%s/workflow/%s" }' % (portalPath, manifestId)
    
    def __update(self):
        print "Updating package selection..."
        activeManifest = self.__getActiveManifest()
        added = formData.getValues("added")
        if added:
            titles = formData.getValues("titles")
            for i in range(len(added)):
                id = added[i]
                title = titles[i]
                node = activeManifest.get("manifest//node-%s" % id)
                if node is None:
                    print "adding:", id, title.encode("UTF-8")
                    activeManifest.set("manifest/node-%s/id" % id, id)
                    activeManifest.set("manifest/node-%s/title" % id, title)
                else:
                    print "%s already exists" % id
        removed = formData.getValues("removed")
        if removed:
            for id in removed:
                node = activeManifest.get("manifest//node-%s" % id)
                if node is not None:
                    print "removing:", id
                    activeManifest.removePath("manifest//node-%s" % id)
        print "activeManifest: %s" % activeManifest
        return '{ count: %s }' % self.__getCount()
    
    def __clear(self):
        print "Clearing package selection..."
        sessionState.remove("package/active")
        sessionState.remove("package/active/id")
        sessionState.remove("package/active/pid")
        return "{}"
    
    def __modify(self):
        print "Set active package..."
        oid = formData.get("oid")
        try:
            object = Services.getStorage().getObject(oid)
            sourceId = object.getSourceId()
            payload = object.getPayload(sourceId)
            payloadReader = InputStreamReader(payload.open(), "UTF-8")
            manifest = JsonConfigHelper(payloadReader)
            payloadReader.close()
            payload.close()
            object.close()
            sessionState.set("package/active", manifest)
            sessionState.set("package/active/id", oid)
            sessionState.set("package/active/pid", sourceId)
        except StorageException, e:
            response.setStatus(500)
            return '{ error: %s }' % str(e)
        return '{ count: %s }' % self.__getCount()
    
    def __addCustom(self):
        id = md5.new(str(uuid.uuid4())).hexdigest()
        return '{ attributes: { id: "node-%s", rel: "blank" }, data: "Untitled" }' % id
    
    def __getActiveManifestId(self):
        return sessionState.get("package/active/id")
    
    def __getActiveManifestPid(self):
        return sessionState.get("package/active/pid")
    
    def __getActiveManifest(self):
        activeManifest = sessionState.get("package/active")
        if not activeManifest:
            activeManifest = JsonConfigHelper()
            activeManifest.set("title", "New package")
            activeManifest.set("viewId", portalId)
            sessionState.set("package/active", activeManifest)
        return activeManifest
    
    def __getCount(self):
        count = self.__getActiveManifest().getList("manifest//id").size()
        print "count:", count
        return count
    
    def __getFile(self, packageDir, filename):
        file = File(packageDir, filename)
        if not file.exists():
            out = FileOutputStream(file)
            IOUtils.copy(Services.getClass().getResourceAsStream("/workflows/" + filename), out)
            out.close()
        return file

scriptObject = PackagingActions()
