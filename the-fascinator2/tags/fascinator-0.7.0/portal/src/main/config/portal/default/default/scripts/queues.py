from au.edu.usq.fascinator.common import JsonConfig, JsonConfigHelper
from au.edu.usq.fascinator.common import MessagingServices

class QueuesData:
    def __init__(self):
        pass

    def __activate__(self, context):
        self.request = context["request"]
        self.response = context["response"]
        self.formData = context["formData"]

        if self.request.isXHR():
            print " **** formData: %s" % self.formData
            queue = self.formData.get("queueName")
            msg = self.formData.get("queueMessage")
            self.queueMessage(queue, msg);
            out = self.response.getPrintWriter("text/plain")
            out.println(self.formData)
            out.close()

        self.config = JsonConfigHelper(JsonConfig.getSystemFile())
        self.threads = self.config.getJsonList("messaging/threads")

    def getDescription(self, queue):
        for thread in self.threads:
            name = thread.get("config/name")
            if name == queue:
                return thread.get("description")

    def queueMessage(self, queue, msg):
        ms = MessagingServices.getInstance()
        ms.queueMessage(queue, msg);
        ms.release()

