package au.edu.usq.fascinator.transformer.aperture;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.SAXReader;
import org.ontoware.rdf2go.model.Syntax;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;

public class DcPayload implements Payload {
    private static Logger log = LoggerFactory.getLogger(DcPayload.class);

    private RDFContainer rdf;
    private String filePath;

    /**
     * DcPayload Constructor
     * 
     * @param rdf as RDFContainer
     */
    public DcPayload(String objectId, RDFContainer rdf) {
        filePath = objectId;
        File file = new File(filePath);
        if (file.exists()) {
            filePath = file.getName().split("\\.")[0];
        }
        this.rdf = rdf;
    }

    /**
     * getContentType method
     * 
     * @return metadata type
     */
    @Override
    public String getContentType() {
        return "application/xml+rdf";
    }

    /**
     * getId method
     * 
     * @return metadata id
     */
    @Override
    public String getId() {
        return "dc-rdf";
    }

    /**
     * getInputStream method
     * 
     * @return metadata content as InputStream
     */
    @Override
    public InputStream getInputStream() throws IOException {
        try {
            InputStream in = getClass().getResourceAsStream("/rdf2dc.xsl");

            SAXReader saxReader = new SAXReader();
            InputStream rdfInputStream = new ByteArrayInputStream(
                    stripNonValidXMLCharacters().getBytes("UTF-8"));

            Document doc = saxReader.read(new InputStreamReader(rdfInputStream,
                    "UTF-8"));

            TransformerFactory factory = TransformerFactory.newInstance();

            Templates t = factory.newTemplates(new StreamSource(in));

            Transformer transformer = t.newTransformer();
            transformer.setParameter("filePath", filePath);
            ByteArrayInputStream bis = new ByteArrayInputStream(doc.asXML()
                    .getBytes());
            Source source = new StreamSource(bis);
            DocumentResult result = new DocumentResult();

            transformer.transform(source, result);
            return new ByteArrayInputStream(result.getDocument().asXML()
                    .getBytes());

        } catch (TransformerConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
        // return new
        // ByteArrayInputStream(rdf.getModel().serialize(Syntax.RdfXml)
        // .getBytes());
    }

    /**
     * getLabel method
     * 
     * @return metadata label
     */
    @Override
    public String getLabel() {
        return "DC RDF metadata";
    }

    /**
     * getType method
     * 
     * @return payload type
     */
    @Override
    public PayloadType getType() {
        return PayloadType.Annotation;
    }

    public String stripNonValidXMLCharacters() {
        String rdfString = rdf.getModel().serialize(Syntax.RdfXml).toString();

        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (rdfString == null || ("".equals(rdfString))) {
            return "";
        }
        for (int i = 0; i < rdfString.length(); i++) {
            current = rdfString.charAt(i);
            if ((current == 0x9) || (current == 0xA) || (current == 0xD)
                    || ((current >= 0x20) && (current <= 0xD7FF))
                    || ((current >= 0xE000) && (current <= 0xFFFD))
                    || ((current >= 0x10000) && (current <= 0x10FFFF))) {
                out.append(current);
            }
        }

        return out.toString();
    }

}
