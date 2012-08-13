/*
 * The Fascinator - Fedora Commons 3.x storage plugin
 * Copyright (C) 2009-2011 University of Southern Queensland
 * Copyright (C) 2011 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
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
package com.googlecode.fascinator.storage.fedora;

import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.PayloadType;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.MimeTypeUtil;
import com.googlecode.fascinator.common.storage.impl.GenericDigitalObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.IOUtils;
import org.fcrepo.server.types.gen.Datastream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps a Fedora object to a Fascinator digital object.
 *
 * @author Oliver Lucido
 * @author Greg Pendlebury
 */
public class Fedora3DigitalObject extends GenericDigitalObject {
    /* Fedora log message for adding a payload */
    private static String ADD_LOG_MESSAGE =
            "Fedora3Payload added";

    /* Fedora log message for deleting a payload */
    private static String DELETE_LOG_MESSAGE =
            "Fedora3Payload deleted";

    /* Fedora log message for adding a payload */
    private static String UPDATE_LOG_MESSAGE =
            "Fedora3Payload updated";

    /** Logging */
    private Logger log = LoggerFactory.getLogger(Fedora3DigitalObject.class);

    /** Internal Fedora PID */
    private String fedoraPid;

    /**
     * Constructor
     * 
     * @param oid the Fascinator Object ID
     * @param fedoraPid the Object OID in Fedora (PID)
     */
    public Fedora3DigitalObject(String oid, String fedoraPid) {
        super(oid);
        //log.debug("Construct Fedora3DigitalObject oid={} fid={}", oid, fedoraPid);
        this.fedoraPid = fedoraPid;
        buildManifest();
    }

    /**
     * Build a metadata manifest for this object
     * 
     */
    private void buildManifest() {
        //log.debug("buildManifest({})", getId());
        // Get a (presumably) empty manifest from our superclass
        Map<String, Payload> manifest = getManifest();
        try {
            // Get all the datastreams from Fedora
            Datastream[] datastreams = Fedora3.getApiM().getDatastreams(
                    fedoraPid, null, null);
            // And loop through each one
            for (Datastream ds : datastreams) {
                String dsId = ds.getID();
                // Ignore the DC datastream
                if (!"DC".equals(dsId)) {
                    // Get our Fascinator PID
                    String pid = null;
                    if ("TF-OBJ-META".equals(dsId)) {
                        // For the core metadata payload it is also the DSID
                        pid = dsId;
                    } else {
                        // Everything else, it will be the alternate ID
                        pid = ds.getAltIDs()[1];
                    }
                    // Create a Payload object
                    Payload payload = new Fedora3Payload(ds, pid, fedoraPid);
                    // We are also on the lookout for our source payload
                    if (PayloadType.Source.equals(payload.getType())) {
                        setSourceId(pid);
                    }
                    // And finally, add it to the manifest
                    manifest.put(pid, payload);
                }
            }
        } catch (IOException ex) {
            log.error("Error in Fedora query: ", ex);
        } catch (StorageException ex) {
            log.error("Error accessing Fedora: ", ex);
        }
    }

    /**
     * Created a stored payload in storage as a datastream of this Object.
     * This is the only payload supported by this plugin.
     * 
     * @param pid the Payload ID to use
     * @param in an InputStream containing the data to store
     * @return Payload the Payload Object
     * @throws StorageException if any errors occur
     */
    @Override
    public Payload createStoredPayload(String pid, InputStream in)
            throws StorageException {
        //log.debug("createStoredPayload({},{})", getId(), pid);
        if (pid == null || in == null) {
            throw new StorageException("Error; Null parameter recieved");
        }

        // Sanity check on duplicates
        Map<String, Payload> manifest = getManifest();
        if (manifest.containsKey(pid)) {
            throw new StorageException("pID '" + pid
                    + "' already exists in manifest.");
        }

        try {
            // Translate to a Fedora DSID
            String dsId = getDatastreamId(pid);
            // We will default to ENRICHMENT
            PayloadType type = PayloadType.Enrichment;
            if (getSourceId() == null && !"TF-OBJ-META".equals(pid)) {
                // ... except on the first payload
                setSourceId(pid);
                type = PayloadType.Source;
            }

            // Cache our data to a temp file
            File tempFile = createTempFile(pid, in);
            if (tempFile == null) {
                throw new StorageException("pID '" + pid
                + "' failed to cache temp file.");
            }
            // Grab the MIME type before we delete it
            String contentType = MimeTypeUtil.getMimeType(tempFile);
            // Upload the file to Fedora
            String tempUrl = uploadData(tempFile);
            if (tempUrl == null) {
                throw new StorageException("pID '" + pid
                + "' failed to upload to Fedora.");
            }

            // Now create the datastream and point it at our temp URL
            String[] altIds = new String[] {type.toString(), pid};
            Fedora3.getApiM().addDatastream(
                fedoraPid,         // Fedora PID
                dsId,              // Fedora DSID
                altIds,            // Alt IDs
                pid,               // Label
                false,             // Versionable
                contentType,       // MIME type
                null,              // Format URI
                tempUrl,           // Datastream Location
                "M",               // Control Group
                "A",               // State
                null,              // ChecksumType
                null,              // Checksum
                ADD_LOG_MESSAGE);  // Log message

            // Tidy up and return
            manifest.put(pid, null); // A fudge for now, or the next line fails
            Payload payload = getPayload(pid);
            manifest.put(pid, payload); // Now for real
            return payload;
        } catch (IOException ioe) {
            log.error("Error in Fedora query: ", ioe);
            throw new StorageException(ioe);
        }
    }

    /**
     * Created a linked payload in storage as a datastream of this Object.
     * Linked payloads are not truly supported by this plugin, and the provided
     * File will instead be ingested into Fedora as stored payloads.
     * 
     * @param pid the Payload ID to use
     * @param linkPath a file path to the file to store
     * @return Payload the Payload Object
     * @throws StorageException if any errors occur
     */
    @Override
    public Payload createLinkedPayload(String pid, String linkPath)
            throws StorageException {
        log.warn("This storage plugin does not support linked payloads..."
                + " converting to stored.");
        //log.debug("createLinkedPayload({},{},{})",
        //        new String[] {getId(), pid, linkPath});
        try {
            FileInputStream in = new FileInputStream(linkPath);
            return createStoredPayload(pid, in);
        } catch (FileNotFoundException fnfe) {
            throw new StorageException(fnfe);
        }
    }

    /**
     * Retrieve and instantiate the requested payload in this Object.
     * 
     * @param pid the Payload ID to retrieve
     * @return Payload the Payload Object
     * @throws StorageException if any errors occur
     */
    @Override
    public Payload getPayload(String pid) throws StorageException {
        //log.debug("getPayload({},{})", getId(), pid);
        if (pid == null) {
            throw new StorageException("Error; Null PID recieved");
        }

        // Confirm we actually have this payload first
        Map<String, Payload> manifest = getManifest();
        if (!manifest.containsKey(pid)) {
            throw new StorageException("pID '" + pid + "': was not found");
        
        }

        String dsId = getDatastreamId(pid);
        try {
            Datastream datastream = Fedora3.getApiM().getDatastream(fedoraPid, dsId, null);
            if (datastream == null) {
                throw new StorageException("pID '" + pid + "' does not exist.");
            } else {
                return new Fedora3Payload(datastream, pid, fedoraPid);
            }
        } catch (IOException ioe) {
            log.error("Error accessing Fedora: ", ioe);
            throw new StorageException(ioe);
        }
    }

    /**
     * Remove the requested payload from this Object.
     * 
     * @param pid the Payload ID to retrieve
     * @throws StorageException if any errors occur
     */
    @Override
    public void removePayload(String pid) throws StorageException {
        //log.debug("removePayload({},{})", getId(), pid);
        if (pid == null) {
            throw new StorageException("Error; Null PID recieved");
        }

        // Confirm we actually have this payload first
        Map<String, Payload> manifest = getManifest();
        if (!manifest.containsKey(pid)) {
            throw new StorageException("pID '" + pid + "': was not found");
        
        }

        String dsId = getDatastreamId(pid);
        try {
            Fedora3.getApiM().purgeDatastream(
                    fedoraPid, // Fedora PID
                    dsId,      // Fedora DSID
                    null,      // Start datetime: null = earliest
                    null,      // End datetime: null = latest
                    DELETE_LOG_MESSAGE, // Log message
                    false
                );
            manifest.remove(pid);
        } catch (IOException ex) {
            log.error("Error in Fedora query: ", ex);
        } catch (StorageException ex) {
            log.error("Error accessing Fedora: ", ex);
        }
    }

    /**
     * Update a stored payload in storage for this Object.
     * 
     * @param pid the Payload ID to use
     * @param in an InputStream containing the data to store
     * @return Payload the updated Payload Object
     * @throws StorageException if any errors occur
     */
    @Override
    public Payload updatePayload(String pid, InputStream in)
            throws StorageException {
        //log.debug("updatePayload({},{})", getId(), pid);
        if (pid == null || in == null) {
            throw new StorageException("Error; Null parameter recieved");
        }

        // Double-check it actually exists before we try to modify it
        Map<String, Payload> manifest = getManifest();
        if (!manifest.containsKey(pid)) {
            throw new StorageException("pID '" + pid + "': was not found");
        }
        Payload payload = manifest.get(pid);

        try {
            // Translate to a Fedora DSID... and make sure it exists
            String dsId = getDatastreamId(pid);

            // Cache our data to a temp file
            File tempFile = createTempFile(pid, in);
            if (tempFile == null) {
                throw new StorageException("pID '" + pid
                + "' failed to cache temp file.");
            }
            // Grab the MIME type before we delete it
            String contentType = MimeTypeUtil.getMimeType(tempFile);
            // Upload the file to Fedora
            String tempUrl = uploadData(tempFile);
            if (tempUrl == null) {
                throw new StorageException("pID '" + pid
                + "' failed to upload to Fedora.");
            }

            // Now create the datastream and point it at our temp URL
            String[] altIds = new String[] {payload.getType().toString(), pid};
            String dsLabel = payload.getLabel();
            Fedora3.getApiM().modifyDatastreamByReference(
                    fedoraPid,  // Fedora PID
                    dsId,       // Fedora DSID
                    altIds,     // Alternate IDs : Array
                    dsLabel,    // Label
                    contentType, // MIME Type
                    null,       // Format URI
                    tempUrl,    // Location
                    null,       // Checksum Type
                    null,       // Checksum
                    UPDATE_LOG_MESSAGE, // Log Message
                    false       // Force an update through integrity violations
                );
            // Remember to update our manifest
            payload = getPayload(pid);
            manifest.put(pid, payload);
            return payload;
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
    }

    /**
     * Translate a Fascinator PID into a hashed datastream ID for Fedora.
     * Should prevent any issues related to special characters being used in IDs
     * 
     * @param pid the Payload ID from Fascinator
     * @return String the dsId to use in Fedora
     */
    private String getDatastreamId(String pid) {
        if ("TF-OBJ-META".equals(pid)) {
            return pid;
        }
        return "DS" + DigestUtils.md5Hex(pid);
    }

    /**
     * Turn an InputStream into a temporary File for uploading.
     * 
     * @param pid the local Fascinator Payload ID used in creating a temp file
     * @param in an InputStream containing the data to upload
     * @return File the new temporary File, NULL if there is an error
     */
    private File createTempFile(String pid, InputStream in) {
        File tempFile = null;
        FileOutputStream out = null;

        // Work out a robust temp file name
        // TODO: 20th Oct 2011: Do we still need this? Leaving it in for now.
        String prefix = FilenameUtils.getBaseName(pid);
        String suffix = FilenameUtils.getExtension(pid);
        prefix = StringUtils.rightPad(prefix, 3, "_");
        suffix = "".equals(suffix) ? null : "." + suffix;
        try {
            // Create and access our empty file in temp space
            tempFile = File.createTempFile(prefix, suffix);
            out = new FileOutputStream(tempFile);
            // Stream the data into storage
            IOUtils.copy(in, out);
        } catch (IOException ex) {
            log.error("Error creating temp file: ", ex);
            return null;
        } finally {
            Fedora3.close(in);
            Fedora3.close(out);
        }

        return tempFile;
    }

    /**
     * Upload a File to Fedora, returning the temporary URL Fedora
     * will need to access it.
     * 
     * @param pid the local Fascinator Payload ID used in creating a temp file
     * @param in an InputStream containing the data to upload
     * @return String The temporary URL in Fedora, or NULL if a failure occurs
     */
    private String uploadData(File file) {
        try {
            return Fedora3.getClient().uploadFile(file);
        } catch (IOException ex) {
            log.error("Error sending file to Fedora: ", ex);
            return null;
        } catch (StorageException ex) {
            log.error("Error accessing Fedora: ", ex);
            return null;
        } finally {
            // Don't forget to remove our temp file
            file.delete();
        }
    }
}
