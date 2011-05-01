from java.awt import Desktop
from java.io import File

class OpenData:
    def __init__(self):
        if formData.get("func") == "open-file":
            file = formData.get("file")
            print " * open.py: Opening file %s..." % file
            Desktop.getDesktop().open(File(file))
        writer = response.getPrintWriter("text/plain")
        writer.println("{}")
        writer.close()

scriptObject = OpenData()
