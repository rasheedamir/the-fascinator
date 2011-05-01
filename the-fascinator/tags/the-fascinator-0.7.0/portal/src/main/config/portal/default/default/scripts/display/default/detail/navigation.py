
class NavigationData:
    def __activate__(self, context):
        self.page = context["page"]
        self.metadata = context["metadata"]
    
    def hasWorkflow(self):
        self.__workflowStep = self.metadata.getList("workflow_step_label")
        if self.__workflowStep.isEmpty():
            return False
        return True
    
    def hasWorkflowAccess(self):
        userRoles = self.page.authentication.get_roles_list()
        workflowSecurity = self.metadata.getList("workflow_security")
        for userRole in userRoles:
            if userRole in workflowSecurity:
                return True
        return False
    
    def getWorkflowStep(self):
        return self.__workflowStep[0]
    
