/*
 * The Fascinator - Plugin - Transformer - FFMPEG
 * Copyright (C) 2010 University of Southern Queensland
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
package au.edu.usq.fascinator.transformer.ffmpeg;

import java.io.File;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.transformer.Transformer;
import au.edu.usq.fascinator.common.storage.StorageUtils;

/**
 * Uses a mock Ffmpeg wrapper for testing
 * 
 * @author Linda Octalina
 */
public class FfmpegTransformerTest {

    private Storage ram;

    private Ffmpeg ffmpeg;

    private DigitalObject sourceObject;

    @Before
    public void setup() throws Exception {
        ram = PluginManager.getStorage("ram");
        ram.init("{}");
        ffmpeg = new FfmpegMockImpl();
        sourceObject = StorageUtils.storeFile(ram, new File(getClass()
                .getResource("/african_drum.aif").toURI()));
    }

    @After
    public void shutdown() throws Exception {
        if (sourceObject != null) {
            sourceObject.close();
        }
        if (ram != null) {
            ram.shutdown();
        }
    }

    @Test
    public void transformAudio() throws Exception {
        Transformer t = new FfmpegTransformer(ffmpeg);
        t.init(new File(getClass().getResource("/ffmpeg-config.json").toURI()));
        DigitalObject outputObject = t.transform(sourceObject);

        // should have 2 payloads
        Assert.assertEquals("There should be 2 Payloads", 2, outputObject
                .getPayloadIdList().size());

        String preview = null;
        for (String i : outputObject.getPayloadIdList()) {
            Payload p = outputObject.getPayload(i);
            if (p.getType() == PayloadType.Preview) {
                preview = i;
            }
        }

        // should have a preview payload
        Assert.assertNotNull("Should have a Preview", preview);
        outputObject.close();
    }
}
