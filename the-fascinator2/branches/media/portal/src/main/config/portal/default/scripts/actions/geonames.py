import au.edu.usq.fascinator.common.BasicHttpClient as BasicHttpClient
import org.apache.commons.httpclient.methods.GetMethod as GetMethod

from au.edu.usq.fascinator.common import JsonConfigHelper

class GeoNames:
    def __init__(self):
        responseType = "text/html; charset=UTF-8"
        responseMsg = ""
        func = formData.get("func")
        if func == "placeName":
            try:
                placeName = formData.get("q")
                url = "http://ws.geonames.org/searchJSON?fuzzy=0.7&q=" + placeName
                client = BasicHttpClient(url)
                get = GetMethod(url)
                statusInt = client.executeMethod(get)
                r = str(statusInt)
                jsonConfigHelper = JsonConfigHelper(get.getResponseBodyAsString().strip())
                for geoName in jsonConfigHelper.getJsonList("geonames"):
                    responseMsg += "%s, %s|%s \n" % (geoName.get("name"), geoName.get("countryName"), geoName.get("geonameId"))
            except Exception, e:
                print "exception: ", str(e)
                r = str(e), None
            responseType = "text/plain; charset=UTF-8"
            #responseMsg = "\nToowoomba, Australia\nToowoomba, Africa";
        writer = response.getPrintWriter(responseType)
        writer.println(responseMsg)
        writer.close()

scriptObject = GeoNames()

#"countryName" : "Australia",
#  "adminCode1" : "04",
#  "fclName" : "city, village,...",
#  "countryCode" : "AU",
#  "lng" : 151.9666667,
#  "fcodeName" : "populated place",
#  "fcl" : "P",
#  "name" : "Toowoomba",
#  "fcode" : "PPL",
#  "geonameId" : 2146268,
#  "lat" : -27.55,
#  "population" : 92800,
#  "adminName1" : "Queensland"
