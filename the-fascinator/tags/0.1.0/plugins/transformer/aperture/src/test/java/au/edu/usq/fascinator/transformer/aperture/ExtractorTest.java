/*
 * The Fascinator
 * Copyright (C) 2009  University of Southern Queensland
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
package au.edu.usq.fascinator.transformer.aperture;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

import junit.framework.Assert;

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
public class ExtractorTest {
    private ApertureTransformer ex = new ApertureTransformer("/tmp");

    private GenericDigitalObject testObject;
    private DigitalObject testObjectOutput;

    @Test
    public void testPdfFile() throws URISyntaxException, TransformerException {
        File fileNamepdf = new File(getClass().getResource("/AboutStacks.pdf")
                .toURI());

        testObject = new GenericDigitalObject(fileNamepdf.getAbsolutePath());
        testObjectOutput = ex.transform(testObject);
        Assert.assertEquals("RDF metadata", testObjectOutput.getMetadata()
                .getLabel());
        Assert.assertEquals("rdf", testObjectOutput.getMetadata().getId());
        Assert.assertEquals("application/xml+rdf", testObjectOutput
                .getMetadata().getContentType());
    }

    @Test
    public void testOdtFile() throws URISyntaxException, TransformerException {
        File fileNameodt = new File(getClass().getResource("/test Image.odt")
                .toURI());
        testObject = new GenericDigitalObject(fileNameodt.getAbsolutePath());
        testObjectOutput = ex.transform(testObject);
        Assert.assertEquals("RDF metadata", testObjectOutput.getMetadata()
                .getLabel());
        Assert.assertEquals("rdf", testObjectOutput.getMetadata().getId());
        Assert.assertEquals("application/xml+rdf", testObjectOutput
                .getMetadata().getContentType());
    }

    // Image file?
    @Test
    public void testImageFile() throws URISyntaxException, TransformerException {
        File imageFile = new File(getClass().getResource("/presentation01.jpg")
                .toURI());
        testObject = new GenericDigitalObject(imageFile.getAbsolutePath());
        testObjectOutput = ex.transform(testObject);

        // Try to print out the rdf content
        InputStream in;
        try {
            in = testObjectOutput.getMetadata().getInputStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();

            String line = null;
            try {
                while ((line = r.readLine()) != null) {
                    sb.append(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("String: " + sb.toString());
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    // It will be treated as Plaintext
    @Test
    public void testWithoutExtension() throws URISyntaxException,
            TransformerException {
        File fileWOExt = new File(getClass().getResource("/somefile").toURI());
        testObject = new GenericDigitalObject(fileWOExt.getAbsolutePath());
        testObjectOutput = ex.transform(testObject);

        // Try to print out the rdf content
        // InputStream in;
        // try {
        // in = testObjectOutput.getMetadata().getInputStream();
        // BufferedReader r = new BufferedReader(new InputStreamReader(in));
        // StringBuilder sb = new StringBuilder();
        //
        // String line = null;
        // try {
        // while ((line = r.readLine()) != null) {
        // sb.append(line + "\n");
        // }
        // } catch (IOException e) {
        // e.printStackTrace();
        // } finally {
        // try {
        // in.close();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // }
        // System.out.println("String: " + sb.toString());
        // } catch (IOException e1) {
        // // TODO Auto-generated catch block
        // e1.printStackTrace();
        // }

        Assert.assertEquals("RDF metadata", testObjectOutput.getMetadata()
                .getLabel());
        Assert.assertEquals("rdf", testObjectOutput.getMetadata().getId());
        Assert.assertEquals("application/xml+rdf", testObjectOutput
                .getMetadata().getContentType());
    }
}
