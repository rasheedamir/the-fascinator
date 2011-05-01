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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.api.transformer.Transformer;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.MimeTypeUtil;
import au.edu.usq.fascinator.common.storage.StorageUtils;

/**
 * Converts audio and video media to web friendly versions using the FFMPEG
 * library.
 * 
 * @author Oliver Lucido, Linda Octalina
 */
public class FfmpegTransformer implements Transformer {

    /** Logger */
    private Logger log = LoggerFactory.getLogger(FfmpegTransformer.class);

    /** System config file */
    private JsonConfigHelper config;

    /** Item config file */
    private JsonConfigHelper itemConfig;

    /** FFMPEG output directory */
    private File outputDir;

    /** Ffmpeg class for conversion */
    private Ffmpeg ffmpeg;

    /** Flag for first execution */
    private boolean firstRun = true;

    /**
     * Basic constructor
     * 
     */
    public FfmpegTransformer() {
        // Need a default constructor for ServiceLoader
    }

    /**
     * Instantiate the transformer with an existing instantiation of Ffmpeg
     * 
     * @param ffmpeg already instaniated ffmpeg installation
     */
    public FfmpegTransformer(Ffmpeg ffmpeg) {
        this.ffmpeg = ffmpeg;
    }

    /**
     * Init method to initialise Ffmpeg transformer
     * 
     * @param jsonFile
     * @throws IOException
     * @throws PluginException
     */
    @Override
    public void init(File jsonFile) throws PluginException {
        try {
            config = new JsonConfigHelper(jsonFile);
            reset();
        } catch (IOException ioe) {
            throw new PluginException(ioe);
        }
    }

    /**
     * Init method to initialise Ffmpeg transformer
     * 
     * @param jsonString
     * @throws IOException
     * @throws PluginException
     */
    @Override
    public void init(String jsonString) throws PluginException {
        try {
            config = new JsonConfigHelper(jsonString);
            reset();
        } catch (IOException ioe) {
            throw new PluginException(ioe);
        }
    }

    /**
     * Reset the transformer in preparation for a new object
     */
    private void reset() throws TransformerException {
        if (firstRun) {
            firstRun = false;
            testExecLevel();
            String outputPath = config.get("outputPath");
            outputDir = new File(outputPath);
            outputDir.mkdirs();
        }

        itemConfig = null;
    }

    /**
     * Test the level of functionality available on this system
     * 
     * @return String indicating the level of available functionality
     */
    private String testExecLevel() {
        // Make sure we can start
        if (ffmpeg == null) {
            ffmpeg = new FfmpegImpl(get(config, "binaries/transcoding"),
                    get(config, "binaries/metadata"));
        }
        return ffmpeg.testAvailability();
    }

    /**
     * Transforming digital object method
     * 
     * @params object: DigitalObject to be transformed
     * @return transformed DigitalObject after transformation
     * @throws TransformerException if the transformation fails
     */
    @Override
    public DigitalObject transform(DigitalObject object, String jsonConfig)
            throws TransformerException {
        if (testExecLevel() == null) {
            return object;
        }
        // Purge old data
        reset();

        try {
            itemConfig = new JsonConfigHelper(jsonConfig);
        } catch (IOException ex) {
            throw new TransformerException("Invalid configuration! '{}'", ex);
        }

        String sourceId = object.getSourceId();
        String ext = FilenameUtils.getExtension(sourceId);

        // Check our first level exclusion list
        List<String> excludeList = getList(itemConfig, "metadata/excludeExt");
        if (excludeList.contains(ext.toLowerCase())) {
            return object;
        }

        // Cache the file from storage
        File file;
        try {
            file = new File(outputDir, sourceId);
            FileOutputStream tempFileOut = new FileOutputStream(file);
            // Payload from storage
            Payload payload = object.getPayload(sourceId);
            // Copy and close
            IOUtils.copy(payload.open(), tempFileOut);
            payload.close();
            tempFileOut.close();
        } catch (IOException ex) {
            log.error("Error writing temp file : ", ex);
            return object;
        } catch (StorageException ex) {
            log.error("Error accessing storage data : ", ex);
            return object;
        }
        if (!file.exists()) {
            return object;
        }

        // Gather metadata
        FfmpegInfo info;
        try {
            info = ffmpeg.getInfo(file);
        } catch (IOException ex) {
            errorAndClose(object, file, ex);
            return object;
        }
        File metaFile = writeMetadata(info);
        // FFmpeg doesn't support this file
        if (metaFile == null) {
            return object;
        }

        try {
            Payload payload = createFfmpegPayload(object, metaFile);
            payload.setType(PayloadType.Enrichment);
            payload.close();
        } catch (Exception ex) {
            errorAndClose(object, metaFile, ex);
            return object;
        } finally {
            metaFile.delete();
        }

        // Can we even process this file?
        if (!info.isSupported()) {
            return object;
        }

        // Thumbnails
        excludeList = getList(itemConfig, "thumbnail/excludeExt");
        if (!excludeList.contains(ext.toLowerCase()) && info.hasVideo()) {
            File thumbnail;
            try {
                thumbnail = getThumbnail(file, info.getDuration());
                Payload payload = createFfmpegPayload(object, thumbnail);
                payload.setType(PayloadType.Thumbnail);
                payload.close();
                thumbnail.delete();
            } catch (Exception ex) {
                errorAndClose(object, file, ex);
                return object;
            }
        }

        // Preview
        excludeList = getList(itemConfig, "preview/excludeExt");
        if (!excludeList.contains(ext.toLowerCase())
                && (info.hasVideo() || info.hasAudio())) {
            File converted = convert(file);
            try {
                Payload payload = createFfmpegPayload(object, converted);
                payload.setType(PayloadType.Preview);
                payload.close();
            } catch (Exception ex) {
                errorAndClose(object, converted, ex);
                return object;
            } finally {
                converted.delete();
            }
        }

        // Cleanup
        closeObject(object);
        if (file.exists()) {
            file.delete();
        }
        return object;
    }

    /**
     * Try to create an error payload on the object and close it
     * 
     * @param object to close
     * @param file that had transformation errors
     * @param ex Exception that caused the error
     */
    private void errorAndClose(DigitalObject object, File file, Exception ex) {
        try {
            log.error("FFMpeg Error: {}", ex);
            createFfmpegErrorPayload(object, file, ex.getMessage());
        } catch (Exception ex1) {
            log.error("Unable to write error payload, {}", ex1);
        } finally {
            closeObject(object);
        }
    }

    /**
     * Try to close the object
     * 
     * @param object to close
     */
    private void closeObject(DigitalObject object) {
        try {
            object.close();
        } catch (StorageException ex) {
            log.error("Failed writing object metadata", ex);
        }
    }

    /**
     * Create ffmpeg error payload
     * 
     * @param object : DigitalObject to store the payload
     * @param file : File to use as payload base name
     * @param message : To store as the error details
     * @return Payload the error payload
     * @throws FileNotFoundException if the file provided does not exist
     * @throws UnsupportedEncodingException for encoding errors in the message
     */
    public Payload createFfmpegErrorPayload(DigitalObject object, File file,
            String message) throws StorageException, FileNotFoundException,
            UnsupportedEncodingException {
        String name = FilenameUtils.getBaseName(file.getName())
                + "_ffmpeg_error.htm";
        Payload payload = StorageUtils.createOrUpdatePayload(object, name,
                new ByteArrayInputStream(message.getBytes("UTF-8")));
        payload.setType(PayloadType.Error);
        payload.setContentType("text/html");
        payload.setLabel("FFMPEG conversion errors");
        return payload;
    }

    /**
     * Create converted ffmpeg payload
     * 
     * @param object DigitalObject to store the payload
     * @param file File to be stored as payload
     * @return Payload new payload
     * @throws StorageException if there is a problem trying to store
     * @throws FileNotFoundException if the file provided does not exist
     */
    public Payload createFfmpegPayload(DigitalObject object, File file)
            throws StorageException, FileNotFoundException {
        String name = file.getName();
        Payload payload = StorageUtils.createOrUpdatePayload(object, name,
                new FileInputStream(file));
        payload.setContentType(MimeTypeUtil.getMimeType(name));
        payload.setLabel(name);
        return payload;
    }

    /**
     * Generate thumbnail for the media
     * 
     * @param sourceFile original media
     * @param duration of the original
     * @return File containing generated thumbnail
     * @throws TransformerException if the conversion failed
     */
    private File getThumbnail(File sourceFile, int duration)
            throws TransformerException {
        log.info("Creating thumbnail...");
        String basename = FilenameUtils.getBaseName(sourceFile.getName());
        File outputFile = new File(outputDir, basename + "_thumbnail.jpg");
        if (outputFile.exists()) {
            FileUtils.deleteQuietly(outputFile);
        }
        try {
            List<String> params = new ArrayList<String>();
            params.add("-i");
            params.add(sourceFile.getAbsolutePath()); // input file
            // get random frame from first quarter of video
            params.add("-y"); // overwrite output file
            params.add("-deinterlace");
            params.add("-an"); // disable audio
            params.add("-ss");
            long start = (long) (Math.random() * duration * 0.25);
            params.add(Long.toString(start)); // start time offset
            params.add("-t");
            params.add("00:00:01"); // duration
            params.add("-r");
            params.add("1"); // frame rate
            params.add("-s");
            params.add(get(itemConfig, "thumbnail/size")); // size
            params.add("-vcodec");
            params.add("mjpeg");
            params.add("-f");
            params.add("mjpeg"); // mjpeg output format
            params.add(outputFile.getAbsolutePath()); // output file
            String stderr = ffmpeg.transform(params);
            if (outputFile.exists()) {
                if (outputFile.length() == 0) {
                    throw new TransformerException(
                            "File conversion failed!\n=====\n" + stderr);
                }
            } else {
                throw new TransformerException(
                        "File conversion failed!\n=====\n" + stderr);
            }
            // log.debug(stderr);
            log.info("Thumbnail created: outputFile={}", outputFile);
        } catch (IOException ioe) {
            log.error("Failed to create thumbnail!", ioe);
            throw new TransformerException(ioe);
        }
        return outputFile;
    }

    /**
     * Write FFMPEG metadata to disk
     * 
     * @param info extracted metadata
     * @return File containing metadata
     * @throws TransformerException if the write failed
     */
    private File writeMetadata(FfmpegInfo info) throws TransformerException {
        if (!info.isSupported()) {
            return null;
        }

        File outputFile = new File(outputDir, "ffmpeg.info");
        if (outputFile.exists()) {
            FileUtils.deleteQuietly(outputFile);
        }
        try {
            outputFile.createNewFile();
            FileUtils.writeStringToFile(outputFile, info.toString(), "utf-8");
        } catch (IOException ioe) {
            throw new TransformerException(ioe);
        }
        return outputFile;
    }

    /**
     * Convert audio/video to flv format
     * 
     * @param sourceFile to be converted
     * @return File containing converted media
     * @throws TransformerException if the conversion failed
     */
    private File convert(File sourceFile) throws TransformerException {
        String outputExt = get(itemConfig, "preview/outputExt");
        log.info("Converting to {}: {}", outputExt, sourceFile);
        String filename = sourceFile.getName();
        String basename = FilenameUtils.getBaseName(filename);
        File outputFile = new File(outputDir, basename + "." + outputExt);
        if (outputFile.exists()) {
            FileUtils.deleteQuietly(outputFile);
        }
        try {
            List<String> params = new ArrayList<String>();
            params.add("-i");
            params.add(sourceFile.getAbsolutePath()); // input file
            params.add("-y"); // overwrite output file
            // load extension specific parameters or use defaults if not found
            String configParams = get(itemConfig, "preview/params/default");
            log.debug("configParams: ", configParams);
            String ext = FilenameUtils.getExtension(filename);
            if (!"".equals(ext)) {
                log.debug("Loading params for {}...", ext);
                configParams = get(itemConfig, "preview/params/" + ext,
                        configParams);
            }
            params.addAll(Arrays.asList(StringUtils.split(configParams, ' ')));
            params.add(outputFile.getAbsolutePath()); // output file
            String stderr = ffmpeg.transform(params);
            if (outputFile.exists()) {
                if (outputFile.length() == 0) {
                    throw new TransformerException(
                            "File conversion failed!\n=====\n" + stderr);
                }
            } else {
                throw new TransformerException(
                        "File conversion failed!\n=====\n" + stderr);
            }
            // log.debug(stderr);
            log.info("Conversion successful: outputFile={}", outputFile);
        } catch (IOException ioe) {
            log.error("Failed to convert!", ioe);
            throw new TransformerException(ioe);
        }
        return outputFile;
    }

    /**
     * Get Transformer id
     * 
     * @return id
     */
    @Override
    public String getId() {
        return "ffmpeg";
    }

    /**
     * Get Transformer name
     * 
     * @return name
     */
    @Override
    public String getName() {
        return "FFMPEG Transformer";
    }

    /**
     * Gets a PluginDescription object relating to this plugin.
     * 
     * @return a PluginDescription
     */
    @Override
    public PluginDescription getPluginDetails() {
        PluginDescription pd = new PluginDescription(this);
        JsonConfigHelper metadata = new JsonConfigHelper();
        metadata.set("debug/availability", testExecLevel());

        pd.setMetadata(metadata.toString());
        return pd;
    }

    /**
     * Get a list from item JSON, falling back to system JSON if not found
     * 
     * @param json Config object containing the json data
     * @param key path to the data in the config file
     * @return List<String> containing the config data
     */
    private List<String> getList(JsonConfigHelper json, String key) {
        String configEntry = get(json, key);
        if (configEntry == null) {
            return new ArrayList();
        } else {
            return Arrays.asList(StringUtils.split(configEntry, ','));
        }
    }

    /**
     * Get data from item JSON, falling back to system JSON, then to provided
     * default value if not found
     * 
     * @param json Config object containing the json data
     * @param key path to the data in the config file
     * @param value default to use if not found
     * @return String containing the config data
     */
    private String get(JsonConfigHelper json, String key, String value) {
        String configEntry = json.get(key, null);
        if (configEntry == null) {
            configEntry = config.get(key, value);
        }
        return configEntry;
    }

    /**
     * Get data from item JSON, falling back to system JSON if not found
     * 
     * @param json Config object containing the json data
     * @param key path to the data in the config file
     * @return String containing the config data
     */
    private String get(JsonConfigHelper json, String key) {
        String configEntry = json.get(key, null);
        if (configEntry == null) {
            configEntry = config.get(key, null);
        }
        return configEntry;
    }

    /**
     * Shut down the transformer plugin
     */
    @Override
    public void shutdown() throws PluginException {
    }
}
