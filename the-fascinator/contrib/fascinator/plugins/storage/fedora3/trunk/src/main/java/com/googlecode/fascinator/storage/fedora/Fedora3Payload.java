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

import com.googlecode.fascinator.api.storage.PayloadType;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.MimeTypeUtil;
import com.googlecode.fascinator.common.storage.impl.GenericPayload;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.fcrepo.server.types.gen.Datastream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps a Fedora datastream to a Fascinator payload.
 *
 * @author Oliver Lucido
 * @author Greg Pendlebury
 */
public class Fedora3Payload extends GenericPayload {
    /* Fedora log message for updating metadata */
    private static String METADATA_LOG_MESSAGE =
            "Fedora3Payload metadata updated";

    /** Logging */
    private Logger log = LoggerFactory.getLogger(Fedora3Payload.class);

    /** Date parsing */
    private SimpleDateFormat dateParser =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    /** Fedora PID */
    private String fedoraPid;

    /** Fedora DSID */
    private String dsId;

    /**
     * Instantiate a brand new payload in Fedora
     * 
     * @param pid the Fascinator Payload ID
     * @param fedoraPid the Object OID in Fedora (PID) for this datastream
     * @param dsId the datastream ID in Fedora
     */
    public Fedora3Payload(String pid, String fedoraPid, String dsId) {
        super(pid, pid, MimeTypeUtil.DEFAULT_MIME_TYPE);
        //log.debug("Construct NEW({},{},{})",
        //        new String[] {pid, fedoraPid, dsId});
        init(fedoraPid, dsId);
    }

    /**
     * Instantiate an existing payload from Fedora
     * 
     * @param dsProfile contains Fedora Datastream information
     * @param pid the Fascinator Payload ID
     * @param fedoraPid the Object OID in Fedora (PID) for this datastream
     * @param dsId the datastream ID in Fedora
     */
    public Fedora3Payload(Datastream ds, String pid,
            String fedoraPid) {
        super(pid, ds.getLabel(), ds.getMIMEType(),
                PayloadType.valueOf(ds.getAltIDs()[0]));
        //log.debug("Construct EXISTING ({},{},{})",
        //        new String[] {pid, fedoraPid, ds.getID()});
        init(fedoraPid, ds.getID());
    }

    private void init(String fedoraPid, String dsId) {
        this.fedoraPid = fedoraPid;
        this.dsId = dsId;
    }

    /**
     * Gets the input stream to access the content for this payload
     * 
     * @return an input stream
     * @throws IOException if there was an error reading the stream
     */
    @Override
    public InputStream open() throws StorageException {
        //log.debug("open({})", getId());
        close();

        try {
            return Fedora3.getStream(fedoraPid, dsId);
        } catch (RemoteException ex) {
            log.error("Error during Fedora search: ", ex);
            return null;
        } catch (IOException ex) {
            log.error("Error accessing Fedora: ", ex);
            return null;
        }
    }

    /**
     * Close the input stream for this payload
     * 
     * @throws StorageException if there was an error closing the stream
     */
    @Override
    public void close() throws StorageException {
        //log.debug("close({})", getId());
        Fedora3.release(fedoraPid, dsId);
        if (hasMetaChanged()) {
            updateMeta();
        }
    }

    /**
     * Update payload metadata
     * 
     * @throws StorageException if there was an error
     */
    private void updateMeta() throws StorageException {
        //log.debug("updateMeta({})", getId());

        PayloadType type = getType();
        if (type == null) {
            type = PayloadType.Enrichment;
        }
        try {
            // NULL values indicate we aren't changing that parameter
            String[] altIds = new String[] {getType().toString(), getId()};
            Fedora3.getApiM().modifyDatastreamByReference(
                    fedoraPid,  // Fedora PID
                    dsId,       // Fedora DSID
                    altIds,     // Alternate IDs : Array
                    getLabel(), // Label
                    getContentType(), // MIME Type
                    null,       // Format URI
                    null,       // Location of content: ALWAYS NULL!!!
                    null,       // Checksum Type
                    null,       // Checksum
                    METADATA_LOG_MESSAGE, // Log Message
                    false       // Force an update through integrity violations
                );
            setMetaChanged(false);
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
    }

    /**
     * Return the timestamp when the payload was last modified
     *
     * @returns Long: The last modified date of the payload, or NULL if unknown
     */
    @Override
    public Long lastModified() {
        //log.debug("lastModified({})", getId());
        try {
            // Grab the history of this object's payloads
            Datastream[] datastreams = Fedora3.getApiM().getDatastreamHistory(
                    fedoraPid, dsId);
            if (datastreams == null || datastreams.length == 0) {
                log.error("Error accessing datastream history: '{}' DS '{}'",
                        fedoraPid, dsId);
                return null;
            }
            Date lastModified = dateParser.parse(
                    datastreams[0].getCreateDate());
            return lastModified.getTime();
        } catch (RemoteException ex) {
            log.error("Error in Fedora query: ", ex);
            return null;
        } catch (ParseException ex) {
            log.error("Error parsing date: ", ex);
            return null;
        } catch (StorageException ex) {
            log.error("Error accessing Fedora: ", ex);
            return null;
        }
    }

    /**
     * Return the size of the payload in bytes
     *
     * @returns Integer: The file size in bytes, or NULL if unknown
     */
    @Override
    public Long size() {
        try {
            Datastream datastream = Fedora3.getApiM().getDatastream(
                    fedoraPid, dsId, null);
            return datastream.getSize();
        } catch (RemoteException ex) {
            log.error("Error in Fedora query: ", ex);
            return null;
        } catch (StorageException ex) {
            log.error("Error accessing Fedora: ", ex);
            return null;
        }
    }
}
