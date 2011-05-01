/*
 * The Fascinator - Plugin - Transformer - ICE 2
 * Copyright (C) 2009 University of Southern Queensland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package au.edu.usq.fascinator.transformer.ice2;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject;

/**
 * @author Linda Octalina
 * 
 */

@Ignore
public class IceTransformerTest {
    private GenericDigitalObject testObject;
    private DigitalObject testObjectOutput;
    private IceTransformer tf = new IceTransformer("", "/tmp");

    @Test
    public void testOdpFile() throws URISyntaxException, TransformerException {
        File fileNameodp = new File(getClass().getResource("/presentation.odp")
                .toURI());
        testObject = new GenericDigitalObject(fileNameodp.getAbsolutePath());
        testObjectOutput = tf.transform(testObject);

        // Assert.assertEquals("ICE rendition", testObjectOutput.getMetadata()
        // .getLabel());
        // Assert.assertEquals("ice-rendition.zip",
        // testObjectOutput.getMetadata().getId());
        // Assert.assertEquals("application/zip", testObjectOutput
        // .getMetadata().getContentType());

    }

    @Test
    public void testOdtFile() throws URISyntaxException, TransformerException {
        File fileNameodt = new File(getClass().getResource("/testImage.odt")
                .toURI());
        testObject = new GenericDigitalObject(fileNameodt.getAbsolutePath());
        testObjectOutput = tf.transform(testObject);
        // Assert.assertEquals("ICE rendition", testObjectOutput.getMetadata()
        // .getLabel());
        // Assert.assertEquals("ice-rendition.zip",
        // testObjectOutput.getMetadata().getId());
        // Assert.assertEquals("application/zip", testObjectOutput
        // .getMetadata().getContentType());
    }

    // Test against the file that is not supported in ice
    @Test
    public void testNotSupportedExtensionInIce() throws URISyntaxException,
            TransformerException {
        File fileNameodt = new File(getClass().getResource("/AboutStacks.pdf")
                .toURI());
        testObject = new GenericDigitalObject(fileNameodt.getAbsolutePath());
        testObjectOutput = tf.transform(testObject);
        Assert.assertEquals(testObjectOutput instanceof GenericDigitalObject,
                true);
    }

    // Need to test against the file that is not exist
    @Test
    public void testFileNotSupportedInIce() throws URISyntaxException,
            TransformerException {
        File fileNamePlain = new File(getClass().getResource("/somefile")
                .toURI());
        testObject = new GenericDigitalObject(fileNamePlain.getAbsolutePath());
        testObjectOutput = tf.transform(testObject);
        Assert.assertEquals(testObjectOutput instanceof GenericDigitalObject,
                true);
    }

}
