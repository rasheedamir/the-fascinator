import au.edu.usq.fascinator.common.BasicHttpClient as BasicHttpClient

import org.apache.commons.httpclient.methods.DeleteMethod as DeleteMethod
import org.apache.commons.httpclient.methods.GetMethod as GetMethod
import org.apache.commons.httpclient.methods.PutMethod as PutMethod
import org.apache.commons.httpclient.methods.PostMethod as PostMethod

from java.lang import Exception

class Anotar:
    def __init__(self):
        self.url = formData.get("url")
        self.method = formData.get("method")
        self.payload = formData.get("json")

        self.writer = response.getPrintWriter("text/html; charset=UTF-8")
        self.client = BasicHttpClient(url)

        #response = self.process()
        response = "Hi"
        self.writer.println(response)
        self.writer.close()

    def process(self):
        print "***** " + self.method
        switch = {
            "DELETE" : self.__delete,
            "GET"    : self.__get,
            "POST"   : self.__post,
            "PUT"    : self.__put
        }
        status, reply = switch.get(self.method)()

        if status.startswith("2"):
            json = reply[:-1] + ', "status":"%s"}' % status
            return json
        else:
            return "{\"status\": \""+status+"\"}"

    def __get(self):
        try:
            get = GetMethod(self.url)
            statusInt = client.executeMethod(get)
            r = str(statusInt), get.getResponseBodyAsString().strip()
        except Exception, e:
            r = str(e), None
        return r

    def __post(self):
        try:
            post = PostMethod(self.url)
            post.setRequestBody(self.payload)
            statusInt = client.executeMethod(post)
            r = str(statusInt), post.getResponseBodyAsString().strip()
        except Exception, e:
            r = str(e), None
        return r

    def __put(self):
        try:
            put = PutMethod(self.url)
            put.setRequestBody(self.payload)
            statusInt = client.executeMethod(put)
            r = str(statusInt), put.getResponseBodyAsString().strip()
        except Exception, e:
            r = str(e), None
        return r

    def __delete(self):
        try:
            delete = DeleteMethod(self.url)
            statusInt = client.executeMethod(delete)
            r = str(statusInt), delete.getResponseBodyAsString().strip()
        except Exception, e:
            r = str(e), None
        return r
