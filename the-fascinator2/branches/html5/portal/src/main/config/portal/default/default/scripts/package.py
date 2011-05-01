
from au.edu.usq.fascinator.common import JsonConfigHelper, JsonConfig
from org.apache.commons.lang import StringEscapeUtils

from json import read as jsonReader, write as jsonWriter

##
from java.awt import Point, Dimension, FlowLayout
from javax.swing import JButton, JFrame, JLabel, JDialog, JOptionPane
from javax.swing import JTextArea, JTextPane, JEditorPane
# frame = JFrame("Title", size=(400, 300))
# frame.getContentPane().setLayout(FlowLayout())
# but = JButton("text", actionPerformed=func)   #def func(event):
# frame.add(but)
 # text = JTextPane();  text.getText(); frame.add(text)
# frame.visible = True
#    def popupDebugMessage(self, msg):
#        try:
#            JOptionPane.showMessageDialog(None, msg)
#        except Exception, e:
#            print "popupDebugMessage Error - '%s'\n'message was '%s'" % (str(e), msg)
##

class Package(object):
    def __init__(self):
        print "formData=%s" % formData
        self.__meta = {}
        self.json = ""
        self.isAjax = formData.get("ajax")!=None
        if self.isAjax:
            self.__sendJsonAjaxResult({"ok":"OK"})
        metaList = [""]
        self.__selectedPackageType = formData.get("packageType", "default")
        print "selectedPackageType='%s'" % self.__selectedPackageType
        self.__meta["packageType"] = formData.get("packageType", "default")
        self.__meta["description"] = formData.get("description", "")
        #self.popupDebugMessage("Just testing.")

    def popupDebugMessage(self, msg):
        try:
            JOptionPane.showMessageDialog(None, msg)
        except Exception, e:
            print "popupDebugMessage Error - '%s'\n'message was '%s'" % (str(e), msg)

    def getFormData(self, field):
        return self.__encoded(formData.get(field, ""))

    def getMeta(self, metaName):
        return self.__encoded(self.__meta.get(metaName, ""))

    def getPackageTitle(self):
        title = self.getMeta("title")
        if title=="":
            title = "New package"
        return title

    def getOid(self):
        return self.getFormData("oid")
    
    def getPackageTypes(self):
        pt = self.__getPackageTypes().keys()
        pt.sort()
        return pt

    def getSelectedPackageType(self):
        return self.__selectedPackageType

    def __getPackageTypes(self):
        json = JsonConfigHelper(JsonConfig.getSystemFile())
        packageTypes = json.getMap("portal/packageTypes")
        packageTypes = dict(packageTypes)
        if packageTypes=={}:
            packageTypes["default"] = {"jsonconfig":"packaging-config.json"}
        return packageTypes

    def __encoded(self, text):
        text = text.replace("&", "&amp;")
        text = text.replace("<", "&lt;").replace(">", "&gt;")
        return text.replace("'", "&#x27;").replace('"', "&#x22;")

    def test(self):
        s = "data "
        json = JsonConfigHelper()
        try:
            l = json.getList("test")
            s += str(l.size())
            s += " '%s' " % json.get("test1")
        except Exception, e:
            s += "Error '%s'" % str(e)
        return s

    def __sendJsonAjaxResult(self, data):
        self.json = "(%s)" % jsonWriter(data)
        #writer = response.getPrintWriter("application/json; charset=UTF-8")
        #writer.println(self.json)
        #writer.close()

scriptObject = Package()
