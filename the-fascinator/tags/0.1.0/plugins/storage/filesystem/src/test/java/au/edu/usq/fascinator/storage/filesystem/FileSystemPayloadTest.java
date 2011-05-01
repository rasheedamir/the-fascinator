package au.edu.usq.fascinator.storage.filesystem;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import au.edu.usq.fascinator.api.storage.PayloadType;

public class FileSystemPayloadTest {

    @Test
    public void checkMeta() throws Exception {
        File sampleOdtFile = new File(getClass().getResource("/sample.odt")
                .toURI());
        System.err.println(sampleOdtFile.getAbsolutePath());
        FileSystemPayload fsp = new FileSystemPayload(sampleOdtFile
                .getParentFile(), sampleOdtFile);
        Assert.assertEquals(PayloadType.External, fsp.getType());
        Assert.assertEquals("application/vnd.oasis.opendocument.text", fsp
                .getContentType());
        Assert.assertEquals("ICE Sample Document", fsp.getLabel());
    }
}
