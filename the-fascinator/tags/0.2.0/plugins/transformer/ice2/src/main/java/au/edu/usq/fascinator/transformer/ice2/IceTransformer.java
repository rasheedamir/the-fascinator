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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.transformer.Transformer;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.BasicHttpClient;
import au.edu.usq.fascinator.common.JsonConfig;

/**
 * Transformer Class will send a file to ice-service to get the renditions of
 * the file
 * 
 * @author Linda Octalina
 */

public class IceTransformer implements Transformer {
    private String convertUrl = "http://ice-service.usq.edu.au/api/convert/";
    private String outputPath;
    private String imageRatio;

    private static Logger log = LoggerFactory.getLogger(IceTransformer.class);

    /**
     * Transformer constructor
     */
    public IceTransformer() {

    }

    /**
     * Transformer constructor
     * 
     * @param String convertUrl
     * @param String outputPath
     */
    public IceTransformer(String convertUrl, String outputPath) {

        if (convertUrl != null && convertUrl != "") {
            this.convertUrl = convertUrl;
        }
        this.outputPath = outputPath;
    }

    /**
     * getRendition method (fieldName)
     * 
     * @param String fileName
     * @return boolean: true if success false if fail
     */
    public boolean getRendition(String fileName) {
        File sourceFile = new File(fileName);
        if (sourceFile.exists()) {
            String extResult = getExtension(sourceFile);
            if (!extResult.startsWith("Error")) {
                convertUrl = convertUrl + extResult;
                String result = getRendition(sourceFile);
                if (!result.startsWith("Error")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * getRendition method
     * 
     * @param File sourceFile
     * @return String: filePath if success error message if fail
     */
    public String getRendition(File sourceFile) {
        log.info("Rendering file {}...", sourceFile);
        // check if the outputPath is available:
        File outputDir = new File(outputPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try {
            BasicHttpClient client = new BasicHttpClient(convertUrl);
            PostMethod filePost = new PostMethod(convertUrl);

            String[] filePart = sourceFile.getName().split("\\.");

            Part[] parts = { new StringPart("zip", "1"),
                    new StringPart("dc", "1"), new StringPart("toc", "on"),
                    new StringPart("pdflink", "on"),
                    new StringPart("pathext", ""),
                    new StringPart("template", getTemplate()),
                    new StringPart("resize", imageRatio),
                    new StringPart("mode", "download"),
                    new FilePart("file", sourceFile) };

            filePost.setRequestEntity(new MultipartRequestEntity(parts,
                    filePost.getParams()));
            int status = client.executeMethod(filePost);
            log.debug("HTTP status: {} {}", status, HttpStatus
                    .getStatusText(status));
            if (status != HttpStatus.SC_OK) {
                log.debug("Response body: {}", filePost
                        .getResponseBodyAsString());
            }
            // Get Response
            InputStream is = filePost.getResponseBodyAsStream();
            String headerType = filePost.getResponseHeader("Content-Type")
                    .getValue();
            String basename = sourceFile.getName();
            basename = FilenameUtils.getBaseName(sourceFile.getName());
            String outputFilename = outputPath + "/" + basename + ".zip";
            if (headerType.indexOf("image/jp") > -1) {
                // for resized image
                outputFilename = outputPath + "/" + basename + ".thumb.jpg";
            }
            if (headerType.indexOf("video/x-flv") > -1) {
                // for converted media file
                outputFilename = outputPath + "/" + basename + ".flv";
            }
            // Check if file is exist or not, just incase
            File outputFile = new File(outputFilename);
            if (outputFile.exists()) {
                outputFile.delete();
            }
            FileOutputStream fos = new FileOutputStream(outputFilename);
            IOUtils.copy(is, fos);
            is.close();
            fos.close();
            log.debug("ICE output at {}", outputFilename);
            return outputFilename; // Returning the location of the file
        } catch (Exception e) {
            log.error("An error occurred: {}", e.getMessage());
            // e.printStackTrace();
            return "Error " + e.getMessage();
        }
    }

    /**
     * convertUrl getExtension method
     * 
     * @param File fileObject
     * @return String
     */
    private String getExtension(File fileObject) {
        String[] parts = fileObject.getName().split("\\.");
        String ext = "";
        if (parts.length == 2) {
            ext = parts[1];
            return ext;
        }
        return "Error: Unable to detect file extension properly!";
    }

    /**
     * getTemplate method
     * 
     * @return String
     */
    private String getTemplate() {
        String template = "<html>"
                + "<head>"
                + "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>"
                + "<title>Default Template</title>" + "<style type='text/css'>"
                + ".rendition-links { text-align: right; }"
                + ".body table td { vertical-align: top; }" + "</style>"
                + "<style class='sub style-css' type='text/css'></style>"
                + "</head>" + "<body>" + "<div class='rendition-links'>"
                + "<span class='ins source-link'></span>"
                + "<span class='ins slide-link'></span>"
                + "<span class='ins pdf-rendition-link'></span>" + "</div>"
                + "<h1 class='ins title'></h1>"
                + "<div class='ins page-toc'></div>"
                + "<div class='ins body'></div>" + "</body>" + "</html>";
        return template;
    }

    /**
     * Overridden method getId
     * 
     * @return plugin id
     */
    @Override
    public String getId() {
        return "ice2";
    }

    /**
     * Overridden method getName
     * 
     * @return plugin name
     */
    @Override
    public String getName() {
        return "ICE Transformer";
    }

    /**
     * Overridden method init to initialize
     * 
     * Configuration sample: "transformer": { "conveyer":
     * "aperture-extractor, ice-transformer", "extractor": { "outputPath" :
     * "${user.home}/ice2-output" }, "ice-transformer": { "url":
     * "http://ice-service.usq.edu.au/api/convert/", "outputPath":
     * "${user.home}/ice2-output" } }
     * 
     * @param jsonFile to retrieve the configuration for Transformer
     * @throws PluginException if fail to read the config file
     */
    @Override
    public void init(File jsonFile) throws PluginException {
        try {
            JsonConfig config = new JsonConfig(jsonFile);
            outputPath = config.get("transformer/ice2/outputPath", System
                    .getProperty("user.home")
                    + File.separator + "tmp");
            convertUrl = config.get("transformer/ice2/url",
                    "http://ice-service.usq.edu.au/api/convert/");
            imageRatio = config.get("transformer/ice2/resize.image.ratio",
                    "-90");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Overridden method shutdown method
     * 
     * @throws PluginException
     */
    @Override
    public void shutdown() throws PluginException {
    }

    /**
     * Overridden transform method
     * 
     * @param DigitalObject to be processed
     * @return processed DigitalObject with the rdf metadata
     */
    @Override
    public DigitalObject transform(DigitalObject in)
            throws TransformerException {
        File inFile = new File(in.getId());
        // && !inFile.getName().endsWith(".html")
        // && !inFile.getName().endsWith(".htm")
        if (inFile.exists() && !inFile.getName().endsWith(".mp3")
                && !inFile.getName().endsWith(".m4a")) {
            String result = getRendition(inFile);
            if (!result.startsWith("Error")) {
                // Check if the file is a zip file or error returned from ice
                if (validZipFile(result) == true) {
                    IceDigitalObject iceObject = new IceDigitalObject(in,
                            result);
                    return iceObject;
                }
            }
            File resultFile = new File(result);
            if (resultFile.exists()) {
                resultFile.delete();
            }
        }
        return in;
    }

    private boolean validZipFile(String zipPath) {
        File outFile = new File(zipPath);
        try {
            FileInputStream fis = new FileInputStream(outFile);
            BufferedReader r = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line = null;
            try {
                while ((line = r.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {

            }
            if (sb.toString().indexOf("ice-error") > -1) {
                log.info("Error return from ICE-Service: " + sb.toString());
                return false;
            }
        } catch (FileNotFoundException e) {
            log.info("Not a valid zip returned from ICE: " + zipPath);
            e.printStackTrace();
        }
        return true;
    }

}
