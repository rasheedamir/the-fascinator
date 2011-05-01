from au.edu.usq.fascinator.common import JsonConfig
from au.edu.usq.fascinator.common import JsonConfigHelper

class Queues:
    def __init__(self):
        self.config = JsonConfigHelper(JsonConfig.getSystemFile())
        self.threads = self.config.getJsonList("messaging/threads")

    def getDescription(self, queue):
        for thread in self.threads:
            name = thread.get("config/name")
            if name == queue:
                return thread.get("description")

scriptObject = Queues()
