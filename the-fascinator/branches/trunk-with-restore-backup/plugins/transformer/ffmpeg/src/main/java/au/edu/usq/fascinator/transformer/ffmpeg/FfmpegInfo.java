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
 * Extract metadata about a media file
 * 
 * @author Oliver Lucido
 */
public class FfmpegInfo {

    /** Logger */
    private Logger log = LoggerFactory.getLogger(FfmpegInfo.class);

    /** Supported file format */
    private boolean supported = true;

    /** Audio flag */
    private boolean audio = false;

    /** Video flag */
    private boolean video = false;

    /** Raw output */
    private String rawMediaData;

    /** Processed output */
    private String metadata;

    /** Media duration */
    private int duration;

    /** Format data */
    private JsonConfigHelper format = new JsonConfigHelper();

    /** Raw stream data */
    private List<JsonConfigHelper> streams = new ArrayList();

    /** Processed video stream data */
    private JsonConfigHelper videoStream = new JsonConfigHelper();

    /** Processed video stream data */
    private JsonConfigHelper audioStream = new JsonConfigHelper();

    /**
     * Extract metadata from the given file using the provided FFmpeg object
     *
     * @param ffmpeg implementation to used for extract
     * @param inputFile to extract metadata from
     * @throws IOException if execution failed
     */
    public FfmpegInfo(Ffmpeg ffmpeg, File inputFile) throws IOException {
        // Process and grab output
        log.debug("Extraction: {}", inputFile);
        rawMediaData = ffmpeg.extract(inputFile);
        if (rawMediaData == null || rawMediaData.length() == 0) {
            supported = false;
            return;
        }

        //log.debug("\n\n=====\n\\/ \\/\n\n{}\n/\\ /\\\n=====\n", rawMediaData);

        // FFprobe
        if (ffmpeg.testAvailability().equals(Ffmpeg.DEFAULT_BIN_METADATA)) {
            parseFFprobeMetadata(rawMediaData);
            processFFprobeMetadata();

        // FFmpeg
        } else {
            parseFFmpegMetadata(rawMediaData);
            JsonConfigHelper mData = new JsonConfigHelper();
            mData.set("duration", "" + duration);
            metadata = mData.toString();
        }
    }

    /**
     * Get and clean a value from the raw ouput
     *
     * @param json raw output
     * @param path where the data should be stored
     * @return String containing the cleaned output
     */
    private String getCleanValue(JsonConfigHelper json, String path) {
        String result = json.get(path);
        if (result != null) result = result.trim();
        return result;
    }

    /**
     * Parse raw output from FFprobe into object properties
     *
     * @param rawMetaData to parse
     */
    private void parseFFprobeMetadata(String rawMetaData) {
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

    /**
     * Parse raw output from FFmpeg into object properties
     *
     * @param rawMetaData to parse
     */
    private void parseFFmpegMetadata(String rawMetaData) {
        // Check if supported
        if (supported = (rawMetaData.indexOf(": Unknown format") == -1)) {
            // get duration
            Pattern p = Pattern.compile("Duration: ((\\d+):(\\d+):(\\d+))");
            Matcher m = p.matcher(rawMetaData);
            if (m.find()) {
                long hrs = Long.parseLong(m.group(2)) * 3600;
                long min = Long.parseLong(m.group(3)) * 60;
                long sec = Long.parseLong(m.group(4));
                duration = Long.valueOf(hrs + min + sec).intValue();
            }
            // check for video
            video = Pattern.compile("Stream #.*Video:.*")
                    .matcher(rawMetaData).find();
            // check for audio
            audio = Pattern.compile("Stream #.*Audio:.*")
                    .matcher(rawMetaData).find();
        }
    }

    /**
     * Process parsed output from object properties into a return value
     *
     */
    private void processFFprobeMetadata() {
        getPrimaryStreams();
        JsonConfigHelper mData = new JsonConfigHelper();

        //log.debug("\n========\nFORMAT:\n\n{}\n", format.toString());
        //for (JsonConfigHelper stream : streams) {
        //    log.debug("\n========\nSTREAM:\n\n{}\n", stream.toString());
        //}

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

    /**
     * Process raw stream data from object properties
     * into primary video/audio streams
     *
     */
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

    /**
     * Return the raw ouput that came from the binary
     *
     * @return String containing the raw output
     */
    public String getRaw() {
        return rawMediaData;
    }

    /**
     * Return the duration of the media
     *
     * @return int duration in seconds
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Is the file format supported?
     *
     * @return boolean flag if supported
     */
    public boolean isSupported() {
        return supported;
    }

    /**
     * Does the media have audio?
     *
     * @return boolean if media has audio
     */
    public boolean hasAudio() {
        return audio;
    }

    /**
     * Does the media have video?
     *
     * @return boolean if media has video
     */
    public boolean hasVideo() {
        return video;
    }

    /**
     * Return the processed metadata as a string
     *
     * @return String containing the processed output
     */
    @Override
    public String toString() {
        return metadata;
    }
}
