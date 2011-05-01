
from java.net import URLEncoder
from java.util import HashMap       #, ArrayList
import java.io.StringReader as StringReader
#import org.apache.commons.httpclient.HttpStatus as HttpStatus
import au.edu.usq.fascinator.common.BasicHttpClient as BasicHttpClient
import au.edu.usq.fascinator.common.JsonConfigHelper as JsonConfigHelper
import au.edu.usq.fascinator.common.JsonConfig as JsonConfig
import org.apache.commons.httpclient.methods.DeleteMethod as DeleteMethod
import org.apache.commons.httpclient.methods.GetMethod as GetMethod
import org.apache.commons.httpclient.methods.PutMethod as PutMethod
import org.apache.commons.httpclient.methods.PostMethod as PostMethod

import org.dom4j.Document as Document
import org.dom4j.DocumentException as DocumentException
import org.dom4j.DocumentHelper as DocumentHelper
import org.dom4j.DocumentFactory as DocumentFactory
import org.dom4j.io.SAXReader as SAXReader

import time


# Danno annotea (proxy) RDF to and from JSON
# @author Ron Ward


class Annotation:
    def __init__(self):
        #if formData.get("verb") == "clear-session":
        # http://139.86.38.58:8003/rep.TempTest-Content1/packages/ATest/one.htm
        # http://localhost:8080/danno/annotea/
        # "?w3c_annotates="
        # "?w3c_reply_tree="
        self.__baseUrl = None
        
        self.__annotationUri = "http://www.w3.org/2000/10/annotation-ns#Annotation"
        self.__annoReplyUri = "http://www.w3.org/2001/03/thread#Reply"
        self.__annoType = "http://www.w3.org/2000/10/annotationType#"   # startswith
        self.__replyType = "http://www.w3.org/2001/12/replyType#"
        ns = HashMap()
        ns.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
        ns.put("dc", "http://purl.org/dc/elements/1.1/")
        ns.put("anno", "http://www.w3.org/2000/10/annotation-ns#")
        ns.put("thread", "http://www.w3.org/2001/03/thread#")
        ns.put("adfi", "http://usq.edu.au/adfi/")
        DocumentFactory.getInstance().setXPathNamespaceURIs(ns)
        self.saxReader = SAXReader()
        jConfig = JsonConfig()
        self.__baseUrl = jConfig.get("/annotation/server")
        self.__storageType = jConfig.get("/annotation/storageType", "danno")
        

    def getContentType(self):
        return "application/json"


    def getJson(self):
        if self.__storageType=="couchdb":
            return self.__getJsonCouchDB()
        else:
            return self.__getJsonDanno()
    
    
    def __getJsonCouchDB(self):
        if self.__baseUrl is None:
            d = {"error":"no annotation server setup in the 'system-config.json' file!",
                "data":[]}
            return JsonConfigHelper(d).toString()
        method = formData.get("method")
        url = formData.get("url")
        if url is None:
            url = ""
        try:
            if method=="info":
                d = {"enabled": True, "storageType": self.__storageType,
                        "baseUrl": self.__baseUrl}
            elif method=="getAnnotation":
                # _design/anno/_list/anno/anno?key=
                key = URLEncoder.encode('"' + url + '"', "utf-8")
                #json = self.__get(self.__baseUrl + url)
                json = self.__get(self.__baseUrl + "_design/anno/_list/anno/anno?key=" + key)
                return json
            elif method=="getAnnotates":
                u = self.__baseUrl + '_design/anno/_list/annosFor/annotations?key=%s'
                u = u % URLEncoder.encode('"' + url + '"', "utf-8")
                json = self.__get(u)
                return json
            elif method=="getReplies":
                d = {"data":[]}
            elif method=="delete":
                r = self.__delete(url)
                if r:
                    d = {"deleted": "OK"}
                else:
                    d = {"delete": "Failed"}
            elif method=="close":
                pass
            elif method=="create":
                annotates = formData.get("annotates") or ""
                elemId = formData.get("elemId") or ""
                body = formData.get("body") or ""
                creator = formData.get("creator") or ""
                date = formData.get("date") or ""
                bodyTitle = formData.get("bodyTitle") or ""
                title = formData.get("title") or ""
                annotationType = formData.get("annotationType") or "Comment"
                root = formData.get("root") or ""
                inReplyTo = formData.get("inReplyTo") or ""
                content = formData.get("content") or ""
                json = self.__createJson(annotates, elemId, body, creator,
                    date, bodyTitle, title, annotationType, root, inReplyTo,
                    content)
                status, reply = self.__post(self.__baseUrl, json)
                if status.startswith("2"):
                    json = reply[:-1] + ', "status":"%s"}' % status
                    print json
                    return json
                else:
                    d = {"status": status}
            elif method=="test":
                return "Testing"
            else:
                d = {"error": "Unknown method '%s'" % method, "data":[]}
        except Exception, e:
            d = {"error": "Exception '%s'" % repr(e)}

        return JsonConfigHelper(d).toString()


    def __getJsonDanno(self):
        #s = self.__getBody("http://localhost:8080/danno/annotea/body/73D4FA5361CE5610")
        #s = self.__getAnnotates("http://139.86.38.58:8003/rep.TempTest-Content1/packages/ATest/one.htm")
        #s = self.__getReplies("http://localhost:8080/danno/annotea/A25A7112EFCBFBD2")
        if self.__baseUrl is None:
            d = {"error":"no annotation server setup in the 'system-config.json' file!",
                "data":[]}
            return JsonConfigHelper(d).toString()
        method = formData.get("method")
        url = formData.get("url")
        if url is None:
            url = ""
        try:
            if method=="info":
                d = {"enabled": True}
            elif method=="getAnnotation":
                rdf = self.__getAnnotation(url)
                d = self.__getJsonFromRdf(rdf)
            elif method=="getAnnotates":
                rdf = self.__getAnnotates(url)
                d = self.__getJsonFromRdf(rdf)
            elif method=="getReplies":
                rdf = self.__getReplies(url)
                d = self.__getJsonFromRdf(rdf)
            elif method=="delete":
                r = self.__delete(url)
                if r:
                    d = {"deleted": "OK"}
                else:
                    d = {"delete": "Failed"}
            elif method=="close":
                pass
            elif method=="create":
                annotates = formData.get("annotates") or ""
                elemId = formData.get("elemId") or ""
                body = formData.get("body") or ""
                creator = formData.get("creator")
                date = formData.get("date")
                bodyTitle = formData.get("bodyTitle") or ""
                title = formData.get("title") or ""
                annotationType = formData.get("annotationType") or "Comment"
                root = formData.get("root")
                inReplyTo = formData.get("inReplyTo")
                content = formData.get("content")
                rdf = self.__createRdf(annotates, elemId, body, creator,
                    date, bodyTitle, title, annotationType, root, inReplyTo,
                    content)
                status, reply = self.__post(self.__baseUrl, rdf)
                try:
                    url = reply.split(':about="')[1].split('"', 1)[0]
                except:
                    url = ""
                d = {"url": url, "status":status}
            elif method=="test":
                return "Testing"
            else:
                d = {"error": "Unknown method '%s'" % method, "data":[]}
        except Exception, e:
            d = {"error": "Exception '%s'" % repr(e)}

        #return "('Annotation test data', '%s')" % repr(s)
        j = JsonConfigHelper(d)
        return j.toString()
        #return repr(d)

    
    # --- Private Methods ---
    def __getJsonFromRdf(self, rdfStr):
        # selfUrl, commentType, annotates, context, title, creator,
        #   bodyUrl/body, created, rootUrl, inReplyTo
        #
        d = {}
        dataList = []
        try:
            dom = self.saxReader.read(StringReader(rdfStr))
            for descNode in dom.selectNodes("//rdf:Description"):
                def st(xpath):
                    n = descNode.selectSingleNode(xpath)
                    if n is None:
                        return ""
                    return n.getText()
                rdfTypes = [i.getText() for i in descNode.selectNodes("rdf:type/@rdf:resource")]
                commentType = "?"
                for rt in rdfTypes:
                    if rt.startswith(self.__annoType):
                        commentType = rt.split(self.__annoType)[-1]
                    if rt.startswith(self.__replyType):
                        commentType = rt.split(self.__replyType)[-1]
                bodyUrl = st("anno:body/@rdf:resource")
                body = self.__getBodyBody(bodyUrl)
                data = {
                    "about": st("@rdf:about"),
                    "commentType": commentType,
                    "annotates": st("anno:annotates/@rdf:resource"),
                    "context": st("anno:context"),
                    "title": st("dc:title"),
                    "creator": st("dc:creator"),
                    "bodyUrl": bodyUrl,
                    "body": body,
                    "created": st("anno:created"),
                    "root": st("thread:root/@rdf:resource"),
                    "inReplyTo": st("thread:inReplyTo/@rdf:resource"),
                    "content": st("adfi:content"),
                    "state" : st("adfi:state"),
                    "test": "'apos', & \"double quotes\""
                }
                data["created"] = data["created"].split(".")[0].replace("T", " ")
                dataList.append(data)
            d["data"] = dataList
        except Exception, e:
            d["error"] = repr(e)
        return d


    def __getAnnotation(self, url):
        s = self.__get(url)
        return s


    def __getAnnotates(self, aUrl):
        aUrl = URLEncoder.encode(aUrl, "utf-8")
        url = "%s?%s=%s" % (self.__baseUrl, "w3c_annotates", aUrl)
        s = self.__get(url)
        return s


    def __getReplies(self, aUrl):
        aUrl = URLEncoder.encode(aUrl, "utf-8")
        url = "%s?%s=%s" % (self.__baseUrl, "w3c_reply_tree", aUrl)
        s = self.__get(url)
        return s


    def __getBodyBody(self, bodyUrl):
        try:
            body = self.__getBody(bodyUrl)
            bDom = self.saxReader.read(StringReader(body))
            body = bDom.selectSingleNode("//*[local-name()='body']").asXML()
        except Exception, e:
            body = "ERROR: %s" % str(e)
        return body


    def __getBody(self, url):
        s = self.__get(url)
        if s.startswith("&lt;"):
            s = s.replace("&lt;", "<").replace("&gt;", ">")
        return s


    def __get(self, url):
        try:
            client = BasicHttpClient(url)
            get = GetMethod(url)
            statusInt = client.executeMethod(get)
            s = get.getResponseBodyAsString().strip()
        except Exception, e:
            print "__get() Error - '%s'" % str(e)
            s = ""
        return s


    def __post(self, url, data):
        try:
            client = BasicHttpClient(url)
            post = PostMethod(url)
            #post.addParameter("", data)
            post.setRequestBody(data)
            statusInt = client.executeMethod(post)
            r = str(statusInt), post.getResponseBodyAsString().strip()
        except Exception, e:
            r = str(e), None
        return r


    def __put(self, url, data):
        try:
            client = BasicHttpClient(url)
            put = PostMethod(url)
            put.addParameter("", data)
            statusInt = client.executeMethod(put)
            r = str(statusInt), post.getResponseBodyAsString().strip()
        except Exception, e:
            r = str(e), None
        return r


    def __delete(self, url):
        try:
            client = BasicHttpClient(url)
            delete = DeleteMethod(url)
            statusInt = client.executeMethod(delete)
            return True
        except Exception, e:
            return False


    def __createRdf(self, annotates, elemId, body, creator,
            date=None, bodyTitle="", title="", annotationType="Comment",
            root=None,  inReplyTo=None, content=""):
        rdfTemplate = """<?xml version="1.0" ?>
<r:RDF xmlns:r="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
       xmlns:a="http://www.w3.org/2000/10/annotation-ns#"
       xmlns:d="http://purl.org/dc/elements/1.1/"
       xmlns:h="http://www.w3.org/1999/xx/http#"
       xmlns:adfi="http://usq.edu.au/adfi/">
 <r:Description>
  <r:type r:resource="http://www.w3.org/2000/10/annotation-ns#Annotation"/>
  <r:type r:resource="http://www.w3.org/2000/10/annotationType#%s"/>
  <a:annotates r:resource="%s"/>
  <a:context>%s</a:context>
  <d:title>%s</d:title>
  <d:creator>%s</d:creator>
  <a:created>%s</a:created>
  <d:date>%s</d:date>
  <a:body>
   <r:Description>
    <h:ContentType>text/html</h:ContentType>
    <h:Body r:parseType="Literal">
     <html xmlns="http://www.w3.org/1999/xhtml">
      <head>
       <title>%s</title>
      </head>
      <body>
        %s
      </body>
     </html>
    </h:Body>
   </r:Description>
  </a:body>
  <adfi:content>%s</adfi:content>
 </r:Description>
</r:RDF>"""
        replyRdfTemplate = """<?xml version="1.0" ?>
<r:RDF xmlns:r="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
       xmlns:a="http://www.w3.org/2000/10/annotation-ns#"
       xmlns:d="http://purl.org/dc/elements/1.1/"
       xmlns:tr="http://www.w3.org/2001/03/thread#"
       xmlns:h="http://www.w3.org/1999/xx/http#"
       xmlns:rt="http://www.w3.org/2001/12/replyType"
       xmlns:adfi="http://usq.edu.au/adfi/">
 <r:Description>
  <r:type r:resource="http://www.w3.org/2001/03/thread#Reply"/>
  <r:type r:resource="http://www.w3.org/2001/12/replyType#%s"/>
  <tr:root r:resource="%s"/>
  <tr:inReplyTo r:resource="%s"/>
  <d:title>%s</d:title>
  <d:creator>%s</d:creator>
  <a:created>%s</a:created>
  <d:date>%s</d:date>
  <a:body>
   <r:Description>
    <h:ContentType>text/html</h:ContentType>
    <h:Body r:parseType="Literal">
     <html xmlns="http://www.w3.org/1999/xhtml">
      <head>
       <title>%s</title>
      </head>
      <body>
        %s
      </body>
     </html>
    </h:Body>
   </r:Description>
  </a:body>
  <adfi:content>%s</adfi:content>
 </r:Description>
</r:RDF>"""    #    <h:ContentLength>289</h:ContentLength>

        # annotationType(s) = SeeAlso, Question, Explanation, Example, Comment,
        #           Change, Advice
        xpointer = annotates + '#xpointer(id("%s"))' % elemId
        if date is None or date=="":
            date = time.strftime("%Y-%m-%dT%H:%M.%S")
            if time.timezone>0:
                date += "-"
            else:
                date += "+"
            date += time.strftime("%H:%M", (0,0,0, abs(time.timezone/3600), 0,0,0,0,0))
        created = date
        if creator is None:
            creator = "Anonymous"
        if title=="":
            title = annotationType
        if bodyTitle=="":
            bodyTitle = title
        replyType = annotationType  #"Comment" or "SeeAlso", "Agree", "Disagree", "Comment"
        # annotationType - Comment
        # annotates - http://serv1.example.com/some/page.html
        # (context)xpointer -  http://serv1.example.com/some/page.html#xpointer(id("Main")/p[2])
        # title - ''
        # creator - Fred
        # created - 1999-10-14T12:10Z
        # date - 1999-10-14T12:10Z
        # bodyTitle -
        # body -

        #root = ""
        #inReplyTo = ""
        #title = ""
        #creator = ""
        #created = ""
        #date = ""
        #bodyTitle = ""
        #body = ""

        if inReplyTo is None or inReplyTo=="":
            rdf = rdfTemplate % (annotationType, annotates, xpointer, title, creator,
                    created, date, bodyTitle, body, content)
        else:
            if root is None or root=="":
                root = inReplyTo
            rdf = replyRdfTemplate % (replyType, root, inReplyTo, title, creator,
                    created, date, bodyTitle, body, content)
        return rdf


    def __createJson(self, annotates, elemId, body, creator,
            date="", bodyTitle="", title="", annotationType="Comment",
            root="",  inReplyTo="", content=""):
        xpointer = annotates + '#xpointer(id("%s"))' % elemId
        if date is None or date=="":
            date = time.strftime("%Y-%m-%dT%H:%M.%S")
            if time.timezone>0:
                date += "-"
            else:
                date += "+"
            date += time.strftime("%H:%M", (0,0,0, abs(time.timezone/3600), 0,0,0,0,0))
        created = date
        if creator is None or creator=="":
            creator = "Anonymous"
        if title=="":
            title = annotationType
        if bodyTitle=="":
            bodyTitle = title
        replyType = annotationType  #"Comment" or "SeeAlso", "Agree", "Disagree", "Comment"

        d = {"type":"annotation", "annotates":annotates, "xpointer":xpointer,
             "creator":creator, "created":created, "annotationType":annotationType,
             "title":title, "bodyTitle":bodyTitle, "body":body, "content":content,
             "root":root, "inReplyTo":inReplyTo}
        return JsonConfigHelper(d).toString()





scriptObject = Annotation()


sampleRdf = """
<rdf:RDF
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">

<rdf:Description rdf:about="http://localhost:8080/danno/annotea/A25A7112EFCBFBD2">
	<rdf:type rdf:resource="http://www.w3.org/2000/10/annotation-ns#Annotation"/>
	<rdf:type rdf:resource="http://www.w3.org/2000/10/annotationType#Comment"/>
	<annotates xmlns="http://www.w3.org/2000/10/annotation-ns#" rdf:resource="http://139.86.38.58:8003/rep.TempTest-Content1/packages/ATest/one.htm"/>
	<context xmlns="http://www.w3.org/2000/10/annotation-ns#">http://139.86.38.58:8003/rep.TempTest-Content1/packages/ATest/one.htm#xpointer(id("hf136b6cfp1"))</context>
	<title xmlns="http://purl.org/dc/elements/1.1/">Comment</title>

	<creator xmlns="http://purl.org/dc/elements/1.1/">ward</creator>
	<body xmlns="http://www.w3.org/2000/10/annotation-ns#" rdf:resource="http://localhost:8080/danno/annotea/body/73D4FA5361CE5610"/>
	<date xmlns="http://purl.org/dc/elements/1.1/">2009-1201T16:21+10:00</date>
	<created xmlns="http://www.w3.org/2000/10/annotation-ns#">2009-12-01T16:21:46.388+10:00</created>
</rdf:Description>

</rdf:RDF>

-- REPLY --
<rdf:RDF
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">

<rdf:Description rdf:about="http://localhost:8080/danno/annotea/E7FBE8AC6298145F">
	<rdf:type rdf:resource="http://www.w3.org/2001/03/thread#Reply"/>
	<rdf:type rdf:resource="http://www.w3.org/2001/12/replyType#Comment"/>
	<title xmlns="http://purl.org/dc/elements/1.1/">Comment</title>
	<creator xmlns="http://purl.org/dc/elements/1.1/">ward</creator>
	<body xmlns="http://www.w3.org/2000/10/annotation-ns#" rdf:resource="http://localhost:8080/danno/annotea/body/3B81761E1269FB67"/>

	<date xmlns="http://purl.org/dc/elements/1.1/">2009-1201T16:23+10:00</date>
	<created xmlns="http://www.w3.org/2000/10/annotation-ns#">2009-12-01T16:23:48.822+10:00</created>
	<root xmlns="http://www.w3.org/2001/03/thread#" rdf:resource="http://localhost:8080/danno/annotea/A25A7112EFCBFBD2"/>
	<inReplyTo xmlns="http://www.w3.org/2001/03/thread#" rdf:resource="http://localhost:8080/danno/annotea/A25A7112EFCBFBD2"/>
</rdf:Description>

</rdf:RDF>

-- BODY --
     &lt;html xmlns="http://www.w3.org/1999/xhtml"&gt;
      &lt;head xmlns="http://www.w3.org/1999/xhtml"&gt;
       &lt;title xmlns="http://www.w3.org/1999/xhtml"&gt;Comment</title>
      </head>
      &lt;body xmlns="http://www.w3.org/1999/xhtml"&gt;
        Xxxx
      </body>

     </html>

"""





#######################################################

#from urlparse import urlsplit
#from httplib import HTTP
#
#def pyPost(url, data):
#    """
#        formDataList is a list/(sequence) of (formName, formData) pairs for normal form fields, or
#        (formName, fileType) or (formName, (filename, fileData)) for a file upload form element.
#        Return the server response data
#    """
#    return __putPost("POST", url, data)
#
#def __putPost(method, url, data):
#    """
#        formDataList is a list/(sequence) of (formName, formData) pairs for normal form fields, or
#        (formName, fileType) or (formName, (filename, fileData)) for a file upload form element.
#        Return the server response data
#    """
#    return __putPostRequest(method, url, data)
#
#def __putPostRequest(method, url, data):
#    """
#        formDataList is a list/(sequence) of (formName, formData) pairs for normal form fields, or
#        (formName, fileType) or (formName, (filename, fileData)) for a file upload form element.
#        Return the server response data
#    """
#    host, path = urlsplit(url)[1:3]
#    http = HTTP(host)
#    http.putrequest(method, path)
#    http.putheader("Content-Length", str(len(data)))
#    http.endheaders()
#    http.send(data)
#    errCode, errMsg, headers = http.getreply()
#    response = http.file.read()
#    return response
