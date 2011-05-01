from display.default.result import ResultData as DefaultResultData

class ResultData(DefaultResultData):
    def __activate__(self, context):
        DefaultResultData.__activate__(self, context)
