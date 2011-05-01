from au.edu.usq.fascinator.api.storage import StorageException
from au.edu.usq.fascinator.common import JsonConfigHelper
from au.edu.usq.fascinator.portal import FormData

from java.io import ByteArrayInputStream
from java.lang import String
from java.net import URLDecoder

import locale

class UploadedData:
    def __init__(self):
        self.errorMsg = None
        # Test if some actual form data is available
        self.fileName = formData.get("upload-file-file")
        self.formProcess = None
        self.localFormData = None
        self.metadata = None
        self.object = None
        self.pageService = Services.getPageService()
        self.renderer = toolkit.getDisplayComponent(self.pageService)
        self.template = None

        # Normal workflow progressions
        if self.fileName is None:
            self.hasUpload = False
            self.fileDetails = None
            oid = formData.get("oid")
            if oid is None:
                self.formProcess = False
                self.template = None
            else:
                self.formProcess = True

        # First stage, post-upload
        else:
            # Some browsers won't match what came through dispatch, resolve that
            dispatchFileName = sessionState.get("fileName")
            if dispatchFileName != self.fileName and self.fileName.find(dispatchFileName) != -1:
                self.fileName = dispatchFileName
            self.hasUpload = True
            self.fileDetails = sessionState.get(self.fileName)
            print " * workflow.py : Upload details : ", repr(self.fileDetails)
            self.template = self.fileDetails.get("template")
            self.errorMsg = self.fileDetails.get("error")

        self.getObject()
        self.getWorkflowMetadata()

        if self.formProcess:
            self.processForm()
        #print "workflow.py - UploadedData.__init__() done."

    def getError(self):
        if self.errorMsg is None:
            return ""
        else:
            return self.errorMsg

    def getFileName(self):
        if self.uploadDetails() is None:
            return ""
        else:
            return self.uploadDetails().get("name")

    def getFileSize(self):
        if self.uploadDetails() is None:
            return "0kb"
        else:
            size = float(self.uploadDetails().get("size"))
            if size is not None:
                size = size / 1024.0
            locale.setlocale(locale.LC_ALL, "")
            return locale.format("%.*f", (1, size), True) + " kb"

    def getObjectMetadata(self):
        if self.getObject() is not None:
            try:
                return self.object.getMetadata()
            except StorageException, e:
                pass
        return None

    def getWorkflowMetadata(self):
        if self.metadata is None:
            if self.getObject() is not None:
                try:
                    wfPayload = self.object.getPayload("workflow.metadata")
                    self.metadata = JsonConfigHelper(wfPayload.open())
                    wfPayload.close()
                except StorageException, e:
                    pass
        return self.metadata

    def getOid(self):
        if self.getObject() is None:
            return None
        else:
            return self.getObject().getId()

    def getObject(self):
        if self.object is None:
            # Find the OID for the object
            if self.justUploaded():
                # 1) Uploaded files
                oid = self.fileDetails.get("oid")
            else:
                # 2) or POST process from workflow change
                oid = formData.get("oid")
                if oid is None:
                    # 3) or GET on page to start the process
                    uri = URLDecoder.decode(request.getAttribute("RequestURI"))
                    basePath = portalId + "/" + pageName
                    oid = uri[len(basePath)+1:]

            # Now get the object
            if oid is not None:
                try:
                    self.object = Services.storage.getObject(oid)
                    return self.object
                except StorageException, e:
                    self.errorMsg = "Failed to retrieve object : " + e.getMessage()
                    return None
        else:
            return self.object

    def getWorkflow(self):
        return self.fileDetails.get("workflow")

    def hasError(self):
        if self.errorMsg is None:
            return False
        else:
            return True

    def isPending(self):
        metaProps = self.getObject().getMetadata()
        status = metaProps.get("render-pending")
        if status is None or status == "false":
            return False
        else:
            return True

    def justUploaded(self):
        return self.hasUpload

    def prepareTemplate(self):
        # Retrieve our workflow config
        #print "prepareTemplate()"
        try:
            objMeta = self.getObjectMetadata()
            jsonObject = Services.storage.getObject(objMeta.get("jsonConfigOid"))
            jsonPayload = jsonObject.getPayload(jsonObject.getSourceId())
            config = JsonConfigHelper(jsonPayload.open())
            jsonPayload.close()
        except Exception, e:
            self.errorMsg = "Error retrieving workflow configuration"
            return False

        # Current workflow status
        meta = self.getWorkflowMetadata()
        if meta is None:
            self.errorMsg = "Error retrieving workflow metadata"
            return False
        currentStep = meta.get("step") # Names
        nextStep = ""
        currentStage = None # Objects
        nextStage = None

        # Find next workflow stage
        stages = config.getJsonList("stages")
        if stages.size() == 0:
            self.errorMsg = "Invalid workflow configuration"
            return False

        #print "--------------"
        #print "meta='%s'" % meta        # "workflow.metadata"
        #print "currentStep='%s'" % currentStep
        #print "stages='%s'" % stages
        nextFlag = False
        for stage in stages:
            # We've found the next stage
            if nextFlag:
                nextFlag = False
                nextStage = stage
            # We've found the current stage
            if stage.get("name") == currentStep:
                nextFlag = True
                currentStage = stage

        #print "currentStage='%s'" % currentStage
        #print "nextStage='%s'" % nextStage
        #print "--------------"

        if nextStage is None:
            if currentStage is None:
                self.errorMsg = "Error detecting next workflow stage"
                return False
            else:
                nextStage = currentStage
        nextStep = nextStage.get("name")

        # Security check
        workflow_security = currentStage.getList("security")
        user_roles = page.authentication.get_roles_list()
        valid = False
        for role in user_roles:
            if role in workflow_security:
                valid = True
        if not valid:
            self.errorMsg = "Sorry, but your current security permissions don't allow you to administer this item"
            return False

        self.localFormData = FormData()     # localFormData for organiser.vm
#        try:
#            autoComplete = currentStage.get("auto-complete", "")
#            self.localFormData.set("auto-complete", autoComplete)
#        except: pass
        # Check for existing data
        oldFormData = meta.getJsonList("formData")
        if oldFormData.size() > 0:
            oldFormData = oldFormData.get(0)
            fields = oldFormData.getMap("/")
            for field in fields.keySet():
                self.localFormData.set(field, fields.get(field))

        # Get data ready for progression
        self.localFormData.set("oid", self.getOid())
        self.localFormData.set("currentStep", currentStep)
        if currentStep == "pending":
            self.localFormData.set("currentStepLabel", "Pending")
        else:
            self.localFormData.set("currentStepLabel", currentStage.get("label"))
        self.localFormData.set("nextStep", nextStep)
        self.localFormData.set("nextStepLabel", nextStage.get("label"))
        self.template = nextStage.get("template")
        return True

    def processForm(self):
        # Get our metadata payload
        meta = self.getWorkflowMetadata()
        if meta is None:
            self.errorMsg = "Error retrieving workflow metadata"
            return
        # From the payload get any old form data
        oldFormData = meta.getJsonList("formData")
        if oldFormData.size() > 0:
            oldFormData = oldFormData.get(0)
        else:
            oldFormData = JsonConfigHelper()

        # Quick filter, we may or may not use these fields
        #    below, but they are not metadata
        specialFields = ["oid", "targetStep"]

        # Process all the new fields submitted
        newFormFields = formData.getFormFields()
        for field in newFormFields:
            # Special fields - we are expecting them
            if field in specialFields:
                print " *** Special Field : '" + field + "' => '" + formData.get(field) + "'"
                if field == "targetStep":
                    meta.set(field, formData.get(field))

            # Everything else... metadata
            else:
                print " *** Metadata Field : '" + field + "' => '" + formData.get(field) + "'"
                oldFormData.set(field, formData.get(field))

        # Write the form data back into the workflow metadata
        data = oldFormData.getMap("/")
        for field in data.keySet():
            meta.set("formData/" + field, data.get(field))

        # Write the workflow metadata back into the payload
        response = self.setWorkflowMetadata(meta)
        if not response:
            self.errorMsg = "Error saving workflow metadata"
            return

        # Re-index the object
        Services.indexer.index(self.getOid())
        Services.indexer.commit()

    def redirectNeeded(self):
        return self.formProcess

    def renderTemplate(self):
        r = self.renderer.renderTemplate(portalId, self.template, self.localFormData, sessionState)
        return r

    def setWorkflowMetadata(self, oldMetadata):
        try:
            jsonString = String(oldMetadata.toString())
            inStream = ByteArrayInputStream(jsonString.getBytes("UTF-8"))
            self.object.updatePayload("workflow.metadata", inStream)
            return True
        except StorageException, e:
            return False

    def uploadDetails(self):
        return self.fileDetails

scriptObject = UploadedData()
