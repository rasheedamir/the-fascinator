package au.edu.usq.fascinator.storage.fedora;

import java.io.File;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.StorageException;

@Ignore
public class Fedora3StorageTest {

    private Fedora3Storage storage;

    private void removeObject(String id) {
        if (storage != null) {
            try {
                storage.removeObject(DigestUtils.md5Hex(id));
            } catch (StorageException e) {
            }
        }
    }

    @Before
    public void init() throws Exception {
        storage = new Fedora3Storage();
        storage.init(new File(getClass().getResource("/fedora3-config.json")
                .toURI()));
        removeObject("testobject1");
        removeObject("testobject2");
        removeObject("testobject3");
    }

    @After
    public void cleanup() throws Exception {
        if (storage != null) {
            removeObject("testobject1");
            removeObject("testobject2");
            removeObject("testobject3");
            storage.shutdown();
        }
    }

    @Test
    public void testObject1() throws Exception {
        String oid = DigestUtils.md5Hex("testobject1");
        DigitalObject obj = null;
        DigitalObject obj2 = null;

        // try get the test object
        try {
            obj = storage.getObject(oid);
            Assert.fail();
        } catch (StorageException se) {
            // should throw exception - oID not found
        }

        // create the test object
        obj = storage.createObject(oid);
        obj2 = storage.getObject(oid);
        Assert.assertEquals(obj.getId(), obj2.getId());

        // try create an object with existing oid
        try {
            storage.createObject(oid);
            Assert.fail();
        } catch (StorageException se) {
            // should throw exception - oID exists
        } finally {
            // cleanup
            storage.removeObject(oid);
        }
    }

    @Test
    public void testPayload1() throws Exception {
        String oid = DigestUtils.md5Hex("testobject2");
        String pid = "fedora3-config.json";
        DigitalObject obj = null;
        DigitalObject obj2 = null;

        // create the test object
        obj = storage.createObject(oid);

        // add a payload
        try {
            obj.createStoredPayload(pid,
                    getClass().getResourceAsStream("/" + pid));
            Payload p = obj.getPayload(pid);
            Assert.assertEquals(pid, p.getLabel());
            Assert.assertEquals(PayloadType.Source, p.getType());
            Assert.assertEquals("text/plain", p.getContentType());
            try {
                obj.createStoredPayload(pid,
                        getClass().getResourceAsStream("/" + pid));
                Assert.fail();
            } catch (StorageException se) {
                // should throw exception - pID exists
            }
        } finally {
            // cleanup
            storage.removeObject(oid);
        }
    }

    @Test
    public void testMetadata1() throws Exception {
        String oid = DigestUtils.md5Hex("testobject3");
        DigitalObject obj = storage.createObject(oid);
        DigitalObject obj2 = null;
        // add a payload
        try {
            Properties metadata = obj.getMetadata();
            metadata.setProperty("test.data", "value");
            obj.close();
            obj2 = storage.getObject(oid);
            Properties metadata2 = obj2.getMetadata();
            Assert.assertEquals("value", metadata2.getProperty("test.data"));
            obj2.close();
        } finally {
            // cleanup
            storage.removeObject(oid);
        }
    }
}
