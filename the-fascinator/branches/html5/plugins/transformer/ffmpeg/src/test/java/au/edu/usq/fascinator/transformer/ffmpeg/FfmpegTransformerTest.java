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
 * Test a variety of small media files against FFmpeg
 * 
 * @author Greg Pendlebury
 */
public class FfmpegTransformerTest {

    /** RAM Storage plugin */
    private Storage ram;

    /** FFmpeg Tranformer */
    private Transformer transformer;

    /** Level of execution available on this system */
    private String execLevel;

    /** Temp file */
    private File file;

    /** Input object */
    private DigitalObject inputObject;

    /** Output object */
    private DigitalObject outputObject;

    /**
     * Preparation to be performed before each test
     *
     * @throws Exception on failure
     */
    @Before
    public void setup() throws Exception {
        ram = PluginManager.getStorage("ram");
        ram.init("{}");

        transformer = new FfmpegTransformer();
        file = new File(getClass().getResource("/ffmpeg-config.json").toURI());
        transformer.init(file);
        this.execTest();
    }

    /**
     * Cleanup to be performed after each test
     *
     * @throws Exception on failure
     */
    @After
    public void shutdown() throws Exception {
        if (ram != null) ram.shutdown();
        if (inputObject != null) inputObject.close();
        if (outputObject != null) outputObject.close();
    }

    /**
     * Store and transform a resource
     *
     * @params resource to extract and process
     * @return DigitalObject after transformation
     * @throws Exception on failure
     */
    private DigitalObject transform(String resource) throws Exception {
        file = new File(getClass().getResource(resource).toURI());
        inputObject = StorageUtils.storeFile(ram, file);
        return transformer.transform(inputObject, "{}");
    }

    /**
     * Test for what level of execution FFmpeg can perform on this system
     *
     * @throws Exception on failure
     */
    private void execTest() throws Exception {
        String rawMetadata = transformer.getPluginDetails().getMetadata();
        JsonConfigHelper metadata = new JsonConfigHelper(rawMetadata);
        execLevel = metadata.get("debug/availability");
    }

    /**
     * Tests:
     *  1) Transform an audio file without failure
     *  2) 3 Payloads should be present (original, metadata, preview)
     *  3) That the preview payload has been correctly typed
     *
     * @throws Exception on failure
     */
    @Test
    public void transformAudio() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: transformAudio()\n======\n");
        outputObject = transform("/african_drum.aif");

        // Should have 3 payloads
        Assert.assertEquals("There should be 3 Payloads", 3, outputObject
                .getPayloadIdList().size());

        // Should have a preview payload
        String preview = null;
        for (String i : outputObject.getPayloadIdList()) {
            Payload p = outputObject.getPayload(i);
            if (p.getType() == PayloadType.Preview) {
                preview = i;
            }
        }
        Assert.assertNotNull("Should have a Preview", preview);
    }

    /**
     * Tests:
     *  1) Fail to transform an invalid file without crashing
     *  2) Only the original payload should be present
     *
     * @throws Exception on failure
     */
    @Test
    public void invalidFfmpeg() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: invalidFfmpeg()\n======\n");
        outputObject = transform("/mol19.cml");

        // Should have only 1 payload, invalid = no 'ffmpeg.info'
        Assert.assertEquals(1, outputObject.getPayloadIdList().size());
    }

    /**
     * Tests:
     *  1) Transform a JPG image without failure
     *  2) It should correctly find a duration of 0
     *
     * If FFprobe is found
     *  3) That no audio stream was recorded
     *  4) Correct video stream data was recorded
     *
     * @throws Exception on failure
     */
    @Test
    public void jpgMetadata() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: jpgMetadata()\n======\n");
        outputObject = transform("/wheel.jpg");

        // Get extracted metadata
        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonConfigHelper metadata = new JsonConfigHelper(ffMetadata.open());
        ffMetadata.close();

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.get("duration"), "0");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
            // Verify the metadata we expected came out
            Assert.assertNull(metadata.get("audio/codec/simple"));
            Assert.assertEquals(metadata.get("format/simple"),      "image2");
            Assert.assertEquals(metadata.get("video/codec/simple"), "mjpeg");
            Assert.assertEquals(metadata.get("video/pixel_format"), "yuvj420p");
            Assert.assertEquals(metadata.get("video/width"),        "1704");
            Assert.assertEquals(metadata.get("video/height"),       "2272");
            Assert.assertEquals(metadata.get("duration"),           "0");
        }
    }
 
    /**
     * Tests:
     *  1) Transform a PNG image without failure
     *  2) It should correctly find a duration of 0
     *
     * If FFprobe is found
     *  3) That no audio stream was recorded
     *  4) Correct video stream data was recorded
     *
     * @throws Exception on failure
     */
    @Test
    public void pngMetadata() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: pngMetadata()\n======\n");
        outputObject = transform("/diagram.png");

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonConfigHelper metadata = new JsonConfigHelper(ffMetadata.open());
        ffMetadata.close();

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.get("duration"), "0");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
            Assert.assertNull(metadata.get("audio/codec/simple"));
            Assert.assertEquals(metadata.get("format/simple"),      "image2");
            Assert.assertEquals(metadata.get("video/codec/simple"), "png");
            Assert.assertEquals(metadata.get("video/pixel_format"), "rgb24");
            Assert.assertEquals(metadata.get("video/width"),        "643");
            Assert.assertEquals(metadata.get("video/height"),       "645");
            Assert.assertEquals(metadata.get("duration"),           "0");
        }
    }

    /**
     * Tests:
     *  1) Transform an M4V image without failure
     *  2) It should correctly find a duration of 85s
     *
     * If FFprobe is found
     *  3) Correct audio stream data was recorded
     *  4) Correct video stream data was recorded
     *
     * @throws Exception on failure
     */
    @Test
    public void m4vMetadata() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: m4vMetadata()\n======\n");
        outputObject = transform("/ipod.m4v");

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonConfigHelper metadata = new JsonConfigHelper(ffMetadata.open());
        ffMetadata.close();

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.get("duration"), "85");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
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
    }

    /**
     * Tests:
     *  1) Transform an MP4 video (from Apple) without failure
     *  2) It should correctly find a duration of 4s
     *
     * If FFprobe is found
     *  3) Correct audeo stream data was recorded
     *  4) Correct video stream data was recorded
     *
     * @throws Exception on failure
     */
    @Test
    public void qMp4Metadata() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: qMp4Metadata()\n======\n");
        outputObject = transform("/quicktime.mp4");

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonConfigHelper metadata = new JsonConfigHelper(ffMetadata.open());
        ffMetadata.close();

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.get("duration"), "4");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
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
    }

    /**
     * Tests:
     *  1) Transform an MP4 video (without audio) without failure
     *  2) It should correctly find a duration of 109s
     *
     * If FFprobe is found
     *  3) That no audio stream was recorded
     *  4) Correct video stream data was recorded
     *
     * @throws Exception on failure
     */
    @Test
    public void mp4Metadata() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: mp4Metadata()\n======\n");
        outputObject = transform("/nasa.mp4");

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonConfigHelper metadata = new JsonConfigHelper(ffMetadata.open());
        ffMetadata.close();

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.get("duration"), "109");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
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
    }

    /**
     * Tests:
     *  1) Transform an AIF audio file without failure
     *  2) It should correctly find a duration of 0
     *     (integer value of a 0.32s snippet)
     *
     * If FFprobe is found
     *  3) That no video stream was recorded
     *  4) Correct audio stream data was recorded
     *
     * @throws Exception on failure
     */
    @Test
    public void aifMetadata() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: aifMetadata()\n======\n");
        outputObject = transform("/african_drum.aif");

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonConfigHelper metadata = new JsonConfigHelper(ffMetadata.open());
        ffMetadata.close();

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.get("duration"), "0");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
            Assert.assertEquals(metadata.get("format/simple"),      "aiff");
            Assert.assertEquals(metadata.get("duration"),           "0");

            Assert.assertEquals(metadata.get("audio/codec/simple"), "pcm_s24be");
            Assert.assertEquals(metadata.get("audio/sample_rate"),  "44100");
            Assert.assertEquals(metadata.get("audio/channels"),     "1");

            // Audio only
            Assert.assertNull(metadata.get("video/codec/simple"));
        }
    }

    /**
     * Tests:
     *  1) Transform a MOV image without failure
     *  2) It should correctly find a duration of 5s
     *
     * If FFprobe is found
     *  3) Correct audio stream data was recorded
     *  4) Correct video stream data was recorded
     *
     * @throws Exception on failure
     */
    @Test
    public void movMetadata() throws Exception {
        // FFmpeg is not installed
        if (execLevel == null) return;

        System.out.println("\n======\n  TEST: movMetadata()\n======\n");
        outputObject = transform("/quicktime.mov");

        Payload ffMetadata = outputObject.getPayload("ffmpeg.info");
        JsonConfigHelper metadata = new JsonConfigHelper(ffMetadata.open());
        ffMetadata.close();

        // FFmpeg
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_TRANSCODE)) {
            Assert.assertEquals(metadata.get("duration"), "5");
        }

        // FFprobe
        if (execLevel.equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
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
}
