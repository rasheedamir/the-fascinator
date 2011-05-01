from display.default.result import ResultData as DefaultResultData

class ResultData(DefaultResultData):
    def __activate__(self, context):
        DefaultResultData.__activate__(self, context)
        self.md = context["metadata"]

    def getVideoSummary(self):
        result = self.append("", "dc_format", "Format")
        result = self.append(result, "dc_size", "Resolution")
        result = self.append(result, "dc_duration", "Duration")
        return result

    def append(self, string, field, label):
        value = self.get(field)
        if value is None:
            return string
        else:
            return string + "<strong>%s</strong>: %s<br/>" % (label, value)

    def get(self, field):
        result = self.md.getList(field)
        if result is not None and not result.isEmpty():
            return result.get(0)
        else:
            return None
