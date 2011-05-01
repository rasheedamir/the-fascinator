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

import au.edu.usq.fascinator.common.JsonConfigHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic information about a media file
 * 
 * @author Oliver Lucido
 */
public class FfmpegInfo {

    /** Logger **/
    private Logger log = LoggerFactory.getLogger(FfmpegInfo.class);

    private boolean supported = true;

    // Type flags
    private boolean audio = false;
    private boolean video = false;

    // Metadata
    private String rawMediaData;
    private String metadata;
    private int duration;

    // Parsing objects
    JsonConfigHelper format = new JsonConfigHelper();
    List<JsonConfigHelper> streams = new ArrayList();
    JsonConfigHelper videoStream = new JsonConfigHelper();
    JsonConfigHelper audioStream = new JsonConfigHelper();

    public FfmpegInfo(Ffmpeg ffmpeg, File inputFile) throws IOException {
        List<String> params = new ArrayList<String>();
        params.add("-show_format");
        params.add("-show_streams");
        // For debugging
        //params.add("-pretty");
        params.add(inputFile.getAbsolutePath());

        // Process and grab output
        rawMediaData = ffmpeg.extract(params);
        if (rawMediaData.length() == 0) {
            supported = false;
            return;
        }

        //log.debug("\n\n===========\n\\/ \\/ \\/ \\/\n\n{}\n/\\ /\\ /\\ /\\\n===========\n", rawMediaData);
        parseMetadata(rawMediaData);
        getPrimaryStreams();

        // Prep some variables
        Matcher m = null;
        JsonConfigHelper mData = new JsonConfigHelper();
/*
        log.debug("\n========\nFORMAT:\n\n{}\n", format.toString());
        for (JsonConfigHelper stream : streams) {
            log.debug("\n========\nSTREAM:\n\n{}\n", stream.toString());
        }
*/
        // Duration
        String dString = getCleanValue(format, "duration");
        mData.set("duration_float", dString);
        duration = Float.valueOf(dString).intValue();
        mData.set("duration", "" + duration);

        // Generic format data
        mData.set("format/simple", getCleanValue(format, "format_name"));
        mData.set("format/label", getCleanValue(format, "format_long_name"));

        // Decode Video
        if (videoStream != null) {
            String codec = getCleanValue(videoStream, "codec_name");
            if (codec != null) video = true;

            // Language, two options
            String lang = getCleanValue(videoStream, "language");
            if (lang == null) lang = getCleanValue(videoStream, "tags/language");
            mData.set("video/language", lang);

            mData.set("video/codec/tag",        getCleanValue(videoStream, "codec_tag"));
            mData.set("video/codec/tag_string", getCleanValue(videoStream, "codec_tag_string"));
            mData.set("video/codec/simple",     getCleanValue(videoStream, "codec_name"));
            mData.set("video/codec/label",      getCleanValue(videoStream, "codec_long_name"));
            mData.set("video/width",            getCleanValue(videoStream, "width"));
            mData.set("video/height",           getCleanValue(videoStream, "height"));
            mData.set("video/pixel_format",     getCleanValue(videoStream, "pix_fmt"));
        }

        // Decode Audio
        if (audioStream != null) {
            String codec = getCleanValue(audioStream, "codec_name");
            if (codec != null) audio = true;

            // Language, two options
            String lang = getCleanValue(audioStream, "language");
            if (lang == null) lang = getCleanValue(audioStream, "tags/language");
            mData.set("audio/language", lang);

            mData.set("audio/codec/tag",        getCleanValue(audioStream, "codec_tag"));
            mData.set("audio/codec/tag_string", getCleanValue(audioStream, "codec_tag_string"));
            mData.set("audio/codec/simple",     getCleanValue(audioStream, "codec_name"));
            mData.set("audio/codec/label",      getCleanValue(audioStream, "codec_long_name"));
            String sample_rate = getCleanValue(audioStream, "sample_rate");
            if (sample_rate != null) {
                mData.set("audio/sample_rate",  "" + Float.valueOf(sample_rate).intValue());
            }
            mData.set("audio/channels",         getCleanValue(audioStream, "channels"));
        }
        metadata = mData.toString();
    }

    private String getCleanValue(JsonConfigHelper json, String path) {
        String result = json.get(path);
        if (result != null) result = result.trim();
        return result;
    }

    private void parseMetadata(String rawMetaData) {
        JsonConfigHelper stream = null;
        int eq;

        // Parse the output from FFprobe
        for (String line : rawMetaData.split("\n")) {
            // Section wrappers
            if (line.equals("[STREAM]")) {
                stream = new JsonConfigHelper();
                continue;
            }
            if (line.equals("[/STREAM]")) {
                streams.add(stream);
                stream = null;
                continue;
            }
            if (line.equals("[FORMAT]") || line.equals("[/FORMAT]")) {
                continue;
            }

            // Tags
            if (line.startsWith("TAG:")) {
                line = "tags/" + line.substring(4);
            }

            // File the data
            eq = line.indexOf("=");
            if (stream == null) {
                format.set(line.substring(0, eq), line.substring(eq+1));
            } else {
                stream.set(line.substring(0, eq), line.substring(eq+1));
            }
        }
    }

    private void getPrimaryStreams() {
        for (JsonConfigHelper stream : streams) {
            String type = stream.get("codec_type");
            // The highest index video stream should be considered primary
            if (type.equals("video") && videoStream != null) {
                videoStream = stream;
            }
            // The highest index audio stream should be considered primary
            if (type.equals("audio") && audioStream != null) {
                audioStream = stream;
            }
        }
    }

    public String getRaw() {
        return rawMediaData;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isSupported() {
        return supported;
    }

    public boolean hasAudio() {
        return audio;
    }

    public boolean hasVideo() {
        return video;
    }

    @Override
    public String toString() {
        return metadata;
    }
}
