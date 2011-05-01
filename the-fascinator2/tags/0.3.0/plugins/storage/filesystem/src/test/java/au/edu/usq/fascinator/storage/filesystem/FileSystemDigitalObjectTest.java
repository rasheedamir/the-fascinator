package au.edu.usq.fascinator.storage.filesystem;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Test;

public class FileSystemDigitalObjectTest {

    @Test
    public void getPath1() {
        FileSystemDigitalObject fsdo = new FileSystemDigitalObject(
                new File("."), "file:///Users/lucido/Documents/test1.doc");
        String expected = FilenameUtils
                .separatorsToSystem("./9f/19/9f193517165c524d485ddf8f1cf322da");
        Assert.assertEquals(expected, fsdo.getPath().getPath());
    }

    @Test
    public void getPath2() {
        FileSystemDigitalObject fsdo = new FileSystemDigitalObject(
                new File("."), "oai:eprints.usq.edu.au:318");
        String expected = FilenameUtils
                .separatorsToSystem("./e2/92/e292378c5b38b0d5a4aba11fd40e7151");
        Assert.assertEquals(expected, fsdo.getPath().getPath());
    }
}
