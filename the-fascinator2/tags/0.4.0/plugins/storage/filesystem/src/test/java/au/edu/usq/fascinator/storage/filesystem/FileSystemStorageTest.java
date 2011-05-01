package au.edu.usq.fascinator.storage.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject;
import au.edu.usq.fascinator.common.storage.impl.GenericPayload;

public class FileSystemStorageTest {

    private FileSystemStorage fs;

    private GenericDigitalObject newObject, fileObject;

    private String tmpDir = System.getProperty("java.io.tmpdir");

    @Before
    public void setup() throws Exception {
        fs = new FileSystemStorage();
        fs.init(new File(getClass().getResource("/fs-config.json").toURI()));
        if (fs.getHomeDir().exists()) {
            FileUtils.deleteDirectory(fs.getHomeDir());
        }

        newObject = new GenericDigitalObject("oai:eprints.usq.edu.au:318", "DC");
        GenericPayload testPayload1 = new GenericPayload("DC",
                "Dublin Core Metadata", "text/xml");
        testPayload1.setInputStream(getClass().getResourceAsStream("/dc.xml"));
        newObject.addPayload(testPayload1);

        fileObject = new GenericDigitalObject(
                "/Users/fascinator/Documents/sample.odt");
        GenericPayload testPayload2 = new GenericPayload("sample.odt",
                "ICE Sample Document",
                "application/vnd.oasis.opendocument.text");
        testPayload2.setInputStream(getClass().getResourceAsStream(
                "/sample.odt"));
        GenericPayload testPayload3 = new GenericPayload(
                "images/ice-services.png", "ICE Services Diagram", "image/png");
        testPayload3.setInputStream(getClass().getResourceAsStream(
                "/images/ice-services.png"));
        fileObject.addPayload(testPayload2);
        fileObject.addPayload(testPayload3);
    }

    @After
    public void cleanup() throws IOException {
        FileUtils.deleteDirectory(fs.getHomeDir());
    }

    @Test
    public void addAndGetObject1() throws Exception {
        fs.addObject(newObject);
        FileSystemDigitalObject addedObject = (FileSystemDigitalObject) fs
                .getObject("oai:eprints.usq.edu.au:318");
        Assert.assertEquals(FilenameUtils.normalize(tmpDir
                + "/_fs_test/d0b1c5bd0660ad67a16b7111aafc9389/"
                + "e2/92/e292378c5b38b0d5a4aba11fd40e7151"), addedObject
                .getPath().getAbsolutePath());

        List<Payload> payloads = addedObject.getPayloadList();
        Assert.assertEquals(1, payloads.size());

        Payload payload = payloads.get(0);
        Assert.assertEquals("Dublin Core Metadata", payload.getLabel());
    }

    @Test
    public void addAndGetObject2() throws Exception {
        fs.addObject(fileObject);
        FileSystemDigitalObject addedObject = (FileSystemDigitalObject) fs
                .getObject("/Users/fascinator/Documents/sample.odt");
        Assert.assertEquals(FilenameUtils.normalize(tmpDir
                + "/_fs_test/d0b1c5bd0660ad67a16b7111aafc9389/"
                + "11/b4/11b498d057256a0b602fa0e7c4073fc3"), addedObject
                .getPath().getAbsolutePath());

        List<Payload> payloads = addedObject.getPayloadList();
        Assert.assertEquals(2, payloads.size());

        Payload payload1 = addedObject.getPayload("sample.odt");
        Assert.assertEquals("ICE Sample Document", payload1.getLabel());

        Payload payload2 = addedObject.getPayload("images/ice-services.png");
        Assert.assertEquals("ICE Services Diagram", payload2.getLabel());
    }
}
