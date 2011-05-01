package au.edu.usq.fascinator.transformer.ims;

import au.edu.usq.fascinator.api.PluginException;
import java.io.File;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;

import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject;
public class ImsTransformerTest {

	@Test
    public void testCheckIfZipIsImsPackage() throws URISyntaxException {
		File zipFile = new File(getClass().getResource("/mybook.zip").toURI());
		
		GenericDigitalObject zipObject = new GenericDigitalObject(zipFile.getAbsolutePath());
		ImsDigitalObject imsDigitalObject = new ImsDigitalObject(zipObject, zipFile.getAbsolutePath());
		
		Assert.assertTrue(imsDigitalObject.getIsImsPackage());
		Assert.assertEquals(imsDigitalObject.getPayloadList().size(), 194);
	}
	
	@Test
	public void testGetExt() throws URISyntaxException {
		ImsTransformer imsTransformer = new ImsTransformer();
		File zipFile = new File("mybook...zip");
		Assert.assertEquals(".zip", imsTransformer.getFileExt(zipFile));
	}

	@Test
	public void testTransform() throws URISyntaxException, TransformerException, PluginException {
		ImsTransformer imsTransformer = new ImsTransformer();
		File zipFile = new File(getClass().getResource("/mybook.zip").toURI());
		
		GenericDigitalObject zipObject = new GenericDigitalObject(zipFile.getAbsolutePath());
		
                imsTransformer.init("{}");
		DigitalObject object = imsTransformer.transform(zipObject);
		System.out.println("000 " + object.getPayloadList());
	}
	
}
