from au.edu.usq.fascinator.common import JsonConfigHelper

class AdminData:

    def __init__(self):
        pass

    def __activate__(self, context):
        self.velocityContext = context
        self.json = JsonConfigHelper()

    def parse_json(self, json_string):
        self.json = JsonConfigHelper(json_string)
