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
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.storage.StorageUtils;

/**
 * Uses a mock Ffmpeg wrapper for testing
 * 
 * @author Linda Octalina
 */
public class FfmpegTransformerTest {

    private Storage ram;

    private DigitalObject aifObject;
    private DigitalObject movObject;
    private DigitalObject mp4Object;
    private DigitalObject qMp4Object;
    private DigitalObject m4vObject;
    private DigitalObject pngObject;
    private DigitalObject jpgObject;
    private DigitalObject invalidObject;

    @Before
    public void setup() throws Exception {
        ram = PluginManager.getStorage("ram");
        ram.init("{}");
    }

    @After
    public void shutdown() throws Exception {
        if (aifObject  != null) aifObject.close();
        if (movObject  != null) movObject.close();
        if (mp4Object  != null) mp4Object.close();
        if (qMp4Object != null) qMp4Object.close();
        if (m4vObject  != null) m4vObject.close();
        if (pngObject  != null) pngObject.close();
        if (jpgObject  != null) jpgObject.close();
        if (invalidObject != null) invalidObject.close();

        if (ram != null) ram.shutdown();
    }

    @Test
    public void transformAudio() throws Exception {
        System.out.println("\n================\n  TEST: transformAudio()\n================\n");
        aifObject = StorageUtils.storeFile(ram, new File(getClass()
                .getResource("/african_drum.aif").toURI()));

        Transformer t = new FfmpegTransformer();
        t.init(new File(getClass().getResource("/ffmpeg-config.json").toURI()));
        DigitalObject outputObject = t.transform(aifObject);

        // should have 3 payloads
        Assert.assertEquals("There should be 3 Payloads", 3, outputObject
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

    @Test
    public void invalidFfmpeg() throws Exception {
        System.out.println("\n================\n  TEST: invalidFfmpeg()\n================\n");
        invalidObject = StorageUtils.storeFile(ram, new File(getClass()
                .getResource("/mol19.cml").toURI()));

        Transformer t = new FfmpegTransformer();
        t.init(new File(getClass().getResource("/ffmpeg-config.json").toURI()));
        DigitalObject outputObject = t.transform(invalidObject);

        // Should have only 1 payload, invalid = no 'ffmpeg.info'
        Assert.assertEquals(1, outputObject.getPayloadIdList().size());
        outputObject.close();
    }

    @Test
    public void jpgMetadata() throws Exception {
        System.out.println("\n================\n  TEST: jpgMetadata()\n================\n");
        jpgObject = StorageUtils.storeFile(ram, new File(getClass()
                .getResource("/wheel.jpg").toURI()));

        Transformer t = new FfmpegTransformer();
        t.init(new File(getClass().getResource("/ffmpeg-config.json").toURI()));
        DigitalObject outputObject = t.transform(jpgObject);

        // Get extracted metadata
        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonConfigHelper metadata = new JsonConfigHelper(ffMetadata.open());
        ffMetadata.close();
        outputObject.close();

        // Verify the metadata we expected came out
        Assert.assertNull(metadata.get("audio/codec/simple"));
        Assert.assertEquals(metadata.get("format/simple"),      "image2");
        Assert.assertEquals(metadata.get("video/codec/simple"), "mjpeg");
        Assert.assertEquals(metadata.get("video/pixel_format"), "yuvj420p");
        Assert.assertEquals(metadata.get("video/width"),        "1704");
        Assert.assertEquals(metadata.get("video/height"),       "2272");
        Assert.assertEquals(metadata.get("duration"),           "0");
    }
 
    @Test
    public void pngMetadata() throws Exception {
        System.out.println("\n================\n  TEST: pngMetadata()\n================\n");
        pngObject = StorageUtils.storeFile(ram, new File(getClass()
                .getResource("/diagram.png").toURI()));

        Transformer t = new FfmpegTransformer();
        t.init(new File(getClass().getResource("/ffmpeg-config.json").toURI()));
        DigitalObject outputObject = t.transform(pngObject);

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonConfigHelper metadata = new JsonConfigHelper(ffMetadata.open());
        ffMetadata.close();
        outputObject.close();

        Assert.assertNull(metadata.get("audio/codec/simple"));
        Assert.assertEquals(metadata.get("format/simple"),      "image2");
        Assert.assertEquals(metadata.get("video/codec/simple"), "png");
        Assert.assertEquals(metadata.get("video/pixel_format"), "rgb24");
        Assert.assertEquals(metadata.get("video/width"),        "643");
        Assert.assertEquals(metadata.get("video/height"),       "645");
        Assert.assertEquals(metadata.get("duration"),           "0");
    }

    @Test
    public void m4vMetadata() throws Exception {
        System.out.println("\n================\n  TEST: m4vMetadata()\n================\n");
        m4vObject = StorageUtils.storeFile(ram, new File(getClass()
                .getResource("/ipod.m4v").toURI()));

        Transformer t = new FfmpegTransformer();
        t.init(new File(getClass().getResource("/ffmpeg-config.json").toURI()));
        DigitalObject outputObject = null;
        outputObject = t.transform(m4vObject);

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonConfigHelper metadata = new JsonConfigHelper(ffMetadata.open());
        ffMetadata.close();
        outputObject.close();

        Assert.assertEquals(metadata.get("format/simple"),      "mov,mp4,m4a,3gp,3g2,mj2");
        Assert.assertEquals(metadata.get("duration"),           "85");

        Assert.assertEquals(metadata.get("video/codec/simple"), "h264");
        Assert.assertEquals(metadata.get("video/pixel_format"), "yuv420p");
        Assert.assertEquals(metadata.get("video/width"),        "320");
        Assert.assertEquals(metadata.get("video/height"),       "240");
        Assert.assertEquals(metadata.get("video/language"),     "eng");

        Assert.assertEquals(metadata.get("audio/codec/simple"), "aac");
        Assert.assertEquals(metadata.get("audio/sample_rate"),  "44100");
        Assert.assertEquals(metadata.get("audio/channels"),     "2");
        Assert.assertEquals(metadata.get("audio/language"),     "eng");
    }

    @Test
    public void qMp4Metadata() throws Exception {
        System.out.println("\n================\n  TEST: qMp4Metadata()\n================\n");
        qMp4Object = StorageUtils.storeFile(ram, new File(getClass()
                .getResource("/quicktime.mp4").toURI()));

        Transformer t = new FfmpegTransformer();
        t.init(new File(getClass().getResource("/ffmpeg-config.json").toURI()));
        DigitalObject outputObject = null;
        outputObject = t.transform(qMp4Object);

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonConfigHelper metadata = new JsonConfigHelper(ffMetadata.open());
        ffMetadata.close();
        outputObject.close();

        Assert.assertEquals(metadata.get("format/simple"),      "mov,mp4,m4a,3gp,3g2,mj2");
        Assert.assertEquals(metadata.get("duration"),           "4");

        Assert.assertEquals(metadata.get("video/codec/simple"), "mpeg4");
        Assert.assertEquals(metadata.get("video/pixel_format"), "yuv420p");
        Assert.assertEquals(metadata.get("video/width"),        "190");
        Assert.assertEquals(metadata.get("video/height"),       "240");
        Assert.assertEquals(metadata.get("video/language"),     "eng");

        Assert.assertEquals(metadata.get("audio/codec/simple"), "aac");
        Assert.assertEquals(metadata.get("audio/sample_rate"),  "32000");
        Assert.assertEquals(metadata.get("audio/channels"),     "2");
        Assert.assertEquals(metadata.get("audio/language"),     "eng");
    }

    @Test
    public void mp4Metadata() throws Exception {
        System.out.println("\n================\n  TEST: mp4Metadata()\n================\n");
        mp4Object = StorageUtils.storeFile(ram, new File(getClass()
                .getResource("/nasa.mp4").toURI()));

        Transformer t = new FfmpegTransformer();
        t.init(new File(getClass().getResource("/ffmpeg-config.json").toURI()));
        DigitalObject outputObject = null;
        outputObject = t.transform(mp4Object);

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonConfigHelper metadata = new JsonConfigHelper(ffMetadata.open());
        ffMetadata.close();
        outputObject.close();

        Assert.assertEquals(metadata.get("format/simple"),      "mov,mp4,m4a,3gp,3g2,mj2");
        Assert.assertEquals(metadata.get("duration"),           "109");

        Assert.assertEquals(metadata.get("video/codec/simple"), "mpeg4");
        Assert.assertEquals(metadata.get("video/pixel_format"), "yuv420p");
        Assert.assertEquals(metadata.get("video/width"),        "380");
        Assert.assertEquals(metadata.get("video/height"),       "214");
        Assert.assertEquals(metadata.get("video/language"),     "eng");

        // This video has no audio
        Assert.assertNull(metadata.get("audio/codec/simple"));
    }

    @Test
    public void aifMetadata() throws Exception {
        System.out.println("\n================\n  TEST: aifMetadata()\n================\n");
        aifObject = StorageUtils.storeFile(ram, new File(getClass()
                .getResource("/african_drum.aif").toURI()));

        Transformer t = new FfmpegTransformer();
        t.init(new File(getClass().getResource("/ffmpeg-config.json").toURI()));
        DigitalObject outputObject = null;
        outputObject = t.transform(aifObject);

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonConfigHelper metadata = new JsonConfigHelper(ffMetadata.open());
        ffMetadata.close();
        outputObject.close();

        Assert.assertEquals(metadata.get("format/simple"),      "aiff");
        Assert.assertEquals(metadata.get("duration"),           "0");

        Assert.assertEquals(metadata.get("audio/codec/simple"), "pcm_s24be");
        Assert.assertEquals(metadata.get("audio/sample_rate"),  "44100");
        Assert.assertEquals(metadata.get("audio/channels"),     "1");

        // Audio only
        Assert.assertNull(metadata.get("video/codec/simple"));
    }

    @Test
    public void movMetadata() throws Exception {
        System.out.println("\n================\n  TEST: movMetadata()\n================\n");
        movObject = StorageUtils.storeFile(ram, new File(getClass()
                .getResource("/quicktime.mov").toURI()));

        Transformer t = new FfmpegTransformer();
        t.init(new File(getClass().getResource("/ffmpeg-config.json").toURI()));
        DigitalObject outputObject = null;
        outputObject = t.transform(movObject);

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonConfigHelper metadata = new JsonConfigHelper(ffMetadata.open());
        ffMetadata.close();
        outputObject.close();

        Assert.assertEquals(metadata.get("format/simple"),      "mov,mp4,m4a,3gp,3g2,mj2");
        Assert.assertEquals(metadata.get("duration"),           "5");

        Assert.assertEquals(metadata.get("video/codec/simple"), "svq1");
        Assert.assertEquals(metadata.get("video/pixel_format"), "yuv410p");
        Assert.assertEquals(metadata.get("video/width"),        "190");
        Assert.assertEquals(metadata.get("video/height"),       "240");
        Assert.assertEquals(metadata.get("video/language"),     "eng");

        Assert.assertEquals(metadata.get("audio/codec/simple"), "qdm2");
        Assert.assertEquals(metadata.get("audio/sample_rate"),  "22050");
        Assert.assertEquals(metadata.get("audio/channels"),     "2");
        Assert.assertEquals(metadata.get("audio/language"),     "eng");
    }
}
