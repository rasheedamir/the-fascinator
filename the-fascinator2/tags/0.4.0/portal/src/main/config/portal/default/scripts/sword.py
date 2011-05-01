


#from org.purl.sword.client.Client import *
#import org.apache.commons.httpclient.auth.AuthChallengeProcessor as AuthChallengeProcessor
#import org.apache.commons.httpclient.HttpMethodDirector as HttpMethodDirector
import au.edu.usq.fascinator.portal.SwordSimpleServer as SwordSimpleServer
import java.io.FileOutputStream as FileOutputStream
import org.apache.commons.io.IOUtils as IOUtils
from au.edu.usq.fascinator.api import PluginManager
import au.edu.usq.fascinator.common.JsonConfig as JsonConfig
import au.edu.usq.fascinator.common.JsonConfigHelper as JsonConfigHelper
import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject as GenericDigitalObject
import au.edu.usq.fascinator.common.storage.impl.FilePayload as FilePayload
import au.edu.usq.fascinator.transformer.ims.ImsDigitalObject as ImsDigitalObject
import java.io.File as File;
import org.apache.commons.io.FileUtils as FileUtils
import au.edu.usq.fascinator.HarvestClient as HarvestClient
import au.edu.usq.fascinator.QueueStorage as QueueStorage
import java.io.FileWriter as FileWriter

class SwordHelper(object):
    def __init__(self):
        self.__processRequest()

    def __processRequest(self):
        baseUrl = "http://%s:%s%s/%s" % (request.serverName, serverPort, contextPath, portalId)
        depositUrl = "%s/sword/deposit.post" % baseUrl
        sword = SwordSimpleServer(depositUrl)
        try:
            p =  request.path.split(portalId+"/"+pageName+"/")[1]  # portalPath
        except:
            p = ""
        if p=="post":
            print "\n--- post ---"
            c = sword.getClient()
            c.clearProxy()
            c.clearCredentials()
            postMsg = sword.getPostMessage();
            postMsg.filetype = "application/zip"
            postMsg.filepath = "/home/ward/Desktop/Test.zip"
            depositResponse = c.postFile(postMsg)
            return str(depositResponse)
        elif p=="servicedocument":
            #print "\n--- servicedocument ---"
            sdr = sword.getServiceDocumentRequest()
            sdr.username = formData.get("username", "test")
            sdr.password = formData.get("password", "test")
            if formData.get("test"):
                depositUrl += "?test=1"
            sd = sword.doServiceDocument(sdr)  # get a serviceDocument
            out = response.getPrintWriter("text/xml")
            out.println(str(sd))
            out.close()
            bindings["pageName"] = "-noTemplate-"
            return sd
        elif p=="deposit.post":
            #print "\n--- deposit ---  formData='%s'" % str(formData)
            inputStream = formData.getInputStream()
            headers = {}
            for x in formData.getHeaders().entrySet():
                headers[x.getKey()] = x.getValue()
            deposit = sword.getDeposit()
            noOp = headers.get("X-No-Op") or "false"
            deposit.noOp = (noOp.lower()=="true") or \
                (formData.get("test") is not None)
            contentDisposition = headers.get("Content-Disposition", "")
            filename = ""
            if contentDisposition!="":
                try:
                    filename = contentDisposition.split("filename=")[1]
                    deposit.filename = filename
                except: pass
            slug = headers.get("Slug")
            if slug is not None and slug!="":
                deposit.slug = slug
            #elif filename!="":
            #    deposit.slug = filename

            deposit.username = "SwordUser"
            deposit.password = deposit.username
            try:
                file = File.createTempFile("tmptf", ".zip")
                file.deleteOnExit()
                fos = FileOutputStream(file.getAbsolutePath())
                IOUtils.copy(inputStream, fos)
                fos.close()
                print "copied posted data to '%s'" % file.getAbsolutePath()
            except Exception, e:
                print "--- Exception - '%s'" % str(e)
            deposit.contentDisposition = file.getAbsolutePath()         #????
            deposit.file = inputStream
            depositResponse = sword.doDeposit(deposit)
            id = str(depositResponse.getEntry().id)
            try:
                print
                #imsPlugin = PluginManager.getTransformer("ims")
                jsonConfig = JsonConfig()
                #imsPlugin.init(jsonConfig.getSystemFile())
                #harvestClient = HarvestClient(jsonConfig.getSystemFile());
                storagePlugin = PluginManager.getStorage(jsonConfig.get("storage/type"))
                #storagePlugin.init(jsonConfig.getSystemFile())

                setConfigUri = self.__getPortal().getClass().getResource("/swordRule.json").toURI()
                configFile = File(setConfigUri)
                harvestConfig = JsonConfigHelper(configFile);
                tFile = File.createTempFile("harvest", ".json")
                tFile.deleteOnExit()
                harvestConfig.set("configDir", configFile.getParent())
                harvestConfig.set("sourceFile", file.getAbsolutePath())
                harvestConfig.store(FileWriter(tFile))

                zipObject = GenericDigitalObject(id)
                zipObject.addPayload(FilePayload(file, id))
                #digitalObject = imsPlugin.transform(zipObject, file)
                qStorage = QueueStorage(storagePlugin, tFile)
                qStorage.init(jsonConfig.getSystemFile())
                qStorage.addObject(zipObject)
                if deposit.noOp:
                    print "-- Testing noOp='true' --"
                else:
                    # deposit the content
                    pass
            except Exception, e:
                print "---"
                print " -- Exception - '%s'" % str(e)
                print "---"
            response.setStatus(201)
            self.__mimeType = "text/xml"
            bindings["pageName"] = "-noTemplate-"
            responseOutput.write(str(depositResponse))
            return
        elif p=="test":
            print "\n--- testing ---"
            print "formData='%s'" % str(formData)
        return "Test"
    
    def __getPortal(self):
        return Services.portalManager.get(portalId)


scriptObject = SwordHelper()
