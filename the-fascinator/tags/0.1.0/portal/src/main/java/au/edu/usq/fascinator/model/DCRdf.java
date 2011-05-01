package au.edu.usq.fascinator.model;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DCRdf {
    private Logger log = LoggerFactory.getLogger(DCRdf.class);

    private String RDFns = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private String DCns = "http://purl.org/dc/elements/1.1/";
    private String J0ns = "http://www.semanticdesktop.org/ontologies/2007/01/19/nie#";
    private String J1ns = "http://www.semanticdesktop.org/ontologies/2007/03/22/nco#";
    private String J2ns = "http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#";
    private String FOAFns = "http://xmlns.com/foaf/0.1/";
    private String DCterms = "http://purl.org/dc/terms/";

    private Document document;
    private SAXReader saxReader;

    private Map<String, String> namespaces;

    public DCRdf(InputStream is) {
        try {
            settingUpNamespace();
            saxReader = new SAXReader();
            document = saxReader.read(new InputStreamReader(is, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getTitle() {
        String title = getNodeValue("./rdf:RDF/rdf:Description/dcterms:title");
        if (title == "") {
            title = getNodeValue("./rdf:RDF/rdf:Description/dc:title");
        }
        if (title == "") {
            return getNodeValue("./rdf:RDF/rdf:Description/*[local-name()='title']");
        }
        return title;
    }

    public String getFormat() {
        String format = getNodeValue("./rdf:RDF/rdf:Description/dcterms:format");
        if (format == "") {
            return getNodeValue("./rdf:RDF/rdf:Description/*[local-name()='mimeType']");
        }
        return format;
    }

    public String getDcDate() {
        String date = getNodeValue("./rdf:RDF/rdf:Description/dcterms:date");
        if (date == "") {
            return getNodeValue("./rdf:RDF/rdf:Description/dc:date");
        }
        return date;
    }

    public String getCreator() {
        List<Node> creators = document
                .selectNodes("//rdf:RDF/rdf:Description/*[local-name()='fullname']");
        String creatorStr = "";
        for (Node creator : creators) {
            creatorStr += creator.getText() + ", ";
        }
        creatorStr = creatorStr.substring(0, creatorStr.lastIndexOf(','));
        return creatorStr;

    }

    public String getFilePath() {
        Node filePath = document
                .selectSingleNode("./rdf:RDF/rdf:Description[not(starts-with(@rdf:about, 'urn:'))]");
        if (filePath != null) {
            Element filePathElem = (Element) filePath;
            return filePathElem.attributeValue("about");
        }
        return "";
    }

    public String getLabel() {
        String filePath = getFilePath();

        if (filePath.startsWith("file:")) {
            filePath = filePath.substring(5);
            try {
                filePath = URLDecoder.decode(filePath, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        File sourceFile = new File(filePath);
        if (sourceFile.exists()) {
            return sourceFile.getName();
        }
        return "";
    }

    public String getPageCount() {
        return getNodeValue("./rdf:RDF/rdf:Description/*[local-name()='page-count']");
    }

    public String getGenerator() {
        return getNodeValue("./rdf:RDF/rdf:Description/*[local-name()='generator']");
    }

    private String getNodeValue(String xPath) {
        Node pageCount = document.selectSingleNode(xPath);
        if (pageCount != null) {
            return pageCount.getText();
        }
        return "";
    }

    private void settingUpNamespace() {
        namespaces = new HashMap<String, String>();
        registerNamespace("rdf", RDFns);
        registerNamespace("dc", DCns);
        registerNamespace("j0", J0ns);
        registerNamespace("j1", J1ns);
        registerNamespace("j2", J2ns);
        registerNamespace("foaf", FOAFns);
        registerNamespace("dcterms", DCterms);
        DocumentFactory.getInstance().setXPathNamespaceURIs(namespaces);
    }

    public void registerNamespace(String prefix, String uri) {
        namespaces.put(prefix, uri);
    }

    public void unregisterNamespace(String prefix) {
        namespaces.remove(prefix);
    }
}
