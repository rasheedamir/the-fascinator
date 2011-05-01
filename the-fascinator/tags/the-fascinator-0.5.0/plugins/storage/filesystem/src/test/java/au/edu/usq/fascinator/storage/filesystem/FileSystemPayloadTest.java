package au.edu.usq.fascinator.storage.filesystem;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import au.edu.usq.fascinator.api.storage.PayloadType;

public class FileSystemPayloadTest {

    @Test
    public void checkMeta() throws Exception {
        // Create a test file
        File sampleOdtFile =
                new File(getClass().getResource("/sample.odt").toURI());

        // Create a payload
        FileSystemPayload fsp = new FileSystemPayload("test", sampleOdtFile);
        fsp.writeMetadata();
        Assert.assertEquals("sample.odt", fsp.getLabel());
        fsp.setLabel("ICE Sample Document");
        fsp.writeMetadata();

        Assert.assertEquals(PayloadType.Source, fsp.getType());
        Assert.assertEquals("application/vnd.oasis.opendocument.text",
                fsp.getContentType());
        Assert.assertEquals("ICE Sample Document", fsp.getLabel());
    }
}
