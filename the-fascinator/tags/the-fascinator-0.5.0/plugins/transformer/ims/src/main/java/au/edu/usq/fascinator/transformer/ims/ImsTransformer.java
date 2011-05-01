/*
 * The Fascinator - Plugin - Transformer - Ims
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
package au.edu.usq.fascinator.transformer.ims;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.io.FileOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

/**
 * Unzip Ims package and store the files
 * 
 * @author Ron Ward, Linda Octalina
 */
public class ImsTransformer implements Transformer {

    /** Json config file **/
    private JsonConfigHelper config;

    /** Logging **/
    private static Logger log = LoggerFactory.getLogger(ImsTransformer.class);

    // private boolean isImsPackage = false;

    /** ims Manifest file **/
    private String manifestFile = "imsmanifest.xml";

    /**
     * ImsTransformer constructor
     */
    public ImsTransformer() {

    }

    /**
     * Transform method
     * 
     * @param object : DigitalObject to be transformed
     * @return transformed DigitalObject
     * @throws TransformerException
     * @throws StorageException
     * @throws IOException
     */
    @Override
    public DigitalObject transform(DigitalObject in)
            throws TransformerException {
        String sourceId = in.getSourceId();
        String ext = FilenameUtils.getExtension(sourceId);

        // We are only interested in ZIP files
        if (!ext.equalsIgnoreCase("zip")) {
            return in;
        }

        File inFile;
        String filePath = config.get("sourceFile");
        if (filePath != null) {
            inFile = new File(filePath);
        } else {
            try {
                inFile = File.createTempFile("ims", ".zip");
                inFile.deleteOnExit();
                FileOutputStream tempFileOut = new FileOutputStream(inFile);
                // Payload from storage
                Payload tempPayload = in.getPayload(sourceId);
                // Copy and close
                IOUtils.copy(tempPayload.open(), tempFileOut);
                tempPayload.close();
                tempFileOut.close();
            } catch (IOException ex) {
                log.error("Error writing temp file : ", ex);
                return in;
                //throw new TransformerException(ex);
            } catch (StorageException ex) {
                log.error("Error accessing storage data : ", ex);
                return in;
                //throw new TransformerException(ex);
            }
        }

        if (inFile.exists()) {
            log.info("unpacking ims");
            try {
                in = createImsPayload(in, inFile);
            } catch (StorageException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                inFile.delete();
            }
        } else {
            log.info("not found inFile {}", inFile);
        }
        return in;
    }

    /**
     * Create Payload method for Ims files
     * 
     * @param object : DigitalObject that store the payload
     * @param file : File to be stored as payload
     * @return transformed DigitalObject
     * @throws StorageException
     * @throws IOException
     */
    public DigitalObject createImsPayload(DigitalObject object, File file)
            throws StorageException, IOException {
        ZipFile zipFile = new ZipFile(file);
        ZipEntry manifestEntry = zipFile.getEntry(manifestFile);
        if (manifestEntry != null) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String name = entry.getName();
                    Payload imsPayload = StorageUtils
                            .createOrUpdatePayload(object, name, zipFile
                                    .getInputStream(entry));
                    // Set to enrichment
                    imsPayload.setType(PayloadType.Enrichment);
                    imsPayload.setLabel(name);
                    imsPayload.setContentType(MimeTypeUtil
                            .getMimeType(name));
                }
            }
        }
        return object;
    }

    /**
     * Get Transformer ID
     * 
     * @return id
     */
    @Override
    public String getId() {
        return "ims";
    }

    /**
     * Get Transformer Name
     * 
     * @return name
     */
    @Override
    public String getName() {
        return "IMS Transformer";
    }

    /**
     * Init method to initialise Ims transformer
     * 
     * @param jsonFile
     * @throws IOException
     * @throws PluginException
     */
    @Override
    public void init(File jsonFile) throws PluginException {
        try {
            config = new JsonConfigHelper(jsonFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Init method to initialise Ims transformer
     * 
     * @param jsonString
     * @throws IOException
     * @throws PluginException
     */
    @Override
    public void init(String jsonString) throws PluginException {
        try {
            config = new JsonConfigHelper(jsonString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shut down the transformer plugin
     */
    @Override
    public void shutdown() throws PluginException {

    }

    /**
     * Get File extension method
     * 
     * @param fileName
     * @return file extension
     */
    public String getFileExt(File fileName) {
        return fileName.getName()
                .substring(fileName.getName().lastIndexOf('.'));
    }

}
