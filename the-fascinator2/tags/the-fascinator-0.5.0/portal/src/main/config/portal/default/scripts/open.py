from java.awt import Desktop
from java.io import File
from java.lang import Exception

class OpenData:
    def __init__(self):
        writer = response.getPrintWriter("text/plain; charset=UTF-8")
        jsonResponse = "{}"
        try:
            oid = formData.get("oid")
            object = Services.getStorage().getObject(oid);
            filePath = object.getMetadata().getProperty("file.path")
            object.close()
            print "Opening file '%s'..." % filePath
            Desktop.getDesktop().open(File(filePath))
        except Exception, e:
            jsonResponse = '{ "message": "%s" }' % e.getMessage()
        writer.println(jsonResponse)
        writer.close()

scriptObject = OpenData()
