/* 
 * The Fascinator - Fedora Commons 3.x storage plugin
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
package au.edu.usq.fascinator.storage.fedora;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fedora.RestClient;
import au.edu.usq.fedora.types.ListSessionType;
import au.edu.usq.fedora.types.ObjectFieldType;
import au.edu.usq.fedora.types.ResultType;

/**
 * Fedora3 storage to store DigitalObject
 * 
 * @author Linda Octalina & Oliver Lucido
 * 
 */
public class Fedora3Storage implements Storage {

    /** Default fedora url **/
    private static final String DEFAULT_URL = "http://localhost:8080/fedora";

    /** Default fedora id namespace **/
    private static final String DEFAULT_NAMESPACE = "uuid";

    /** Logger **/
    private Logger log = LoggerFactory.getLogger(Fedora3Storage.class);

    /** API to talk to Fedora **/
    private RestClient client;

    /**
     * Get the storage id
     * 
     * @return storageId
     */
    public String getId() {
        return "fedora3";
    }

    /**
     * Get the storage Name
     * 
     * @return storageName
     */
    public String getName() {
        return "Fedora Commons 3.x Storage Module";
    }

    /**
     * Fedora3 storage initialisation method
     * 
     * @param jsonFile
     */
    public void init(File jsonFile) throws StorageException {
        try {
            JsonConfig config = new JsonConfig(jsonFile);
            String url = config.get("storage/fedora3/url");

            client = new RestClient(url);
            String userName = config.get("storage/fedora3/username");
            String password = config.get("storage/fedora3/password");

            if (userName != null && password != null) {
                client.authenticate(userName, password);
            } else {
                throw new StorageException("Not Fedora 3");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new StorageException(e);
        }
    }

    /**
     * Fedora3 shut down method
     */
    public void shutdown() throws StorageException {
    }

    /**
     * Fedora3 adding new Digital Object method
     * 
     * @param object
     * @return fedoraId
     */
    public String addObject(DigitalObject object) throws StorageException {
        String fedoraId = null;
        String oid = object.getId();
        try {
            fedoraId = getFedoraId(oid);
            if (fedoraId == null) {
                log.debug("Creating new object... ");
                // fedoraId = client.createObject(oid, DEFAULT_NAMESPACE);
                fedoraId = createObject(oid);
                log.debug("Client returned Fedora PID: {}", fedoraId);
            } else {
                log.debug("Updating object {}", fedoraId);
            }

            for (Payload payload : object.getPayloadList()) {
                addPayload(oid, payload);
            }
        } catch (Exception e) {
            throw new StorageException("Failed to add object", e);
        }
        return fedoraId;
    }

    /**
     * Fedora3 removing Digital object method
     * 
     * @param oid
     */
    public void removeObject(String oid) {
        try {
            client.purgeObject(getFedoraId(oid));
        } catch (IOException ioe) {
            log.error("Failed to remove object {}", ioe);
        }
    }

    /**
     * Fedora3 creating new Digital object method
     * 
     * @param oid
     * @return fedoraId
     */
    private String createObject(String oid) {
        try {
            return client.createObject(oid, DEFAULT_NAMESPACE);
        } catch (IOException e) {
            log.debug("Failed to creatObject: " + oid, e);
        }
        return null;
    }

    /**
     * Fedora3 adding payload method
     * 
     * @param oid: object id
     * @param payload
     */
    public void addPayload(String oid, Payload payload) {
        try {
            String fedoraId = getFedoraId(oid);
            if (fedoraId == null) {
                fedoraId = createObject(oid);
            }
            String dsId = payload.getId();
            String dsLabel = payload.getLabel();
            String type = payload.getContentType();
            String payloadType = payload.getType().toString();
            File tmpFile = File.createTempFile("f3_", ".tmp");
            FileOutputStream fos = new FileOutputStream(tmpFile);
            IOUtils.copy(payload.getInputStream(), fos);
            fos.close();

            // NOTE: Fedora does not like id and altId to have special
            // characters like slash or spaces, thus, we are doing the encoding
            // here. The AltId is in PayloadType:dsId format

            client.addDatastream(fedoraId, "DS" + DigestUtils.md5Hex(dsId),
                    dsLabel, type, payloadType + ":"
                            + URLEncoder.encode(dsId, "UTF-8"), tmpFile);
            tmpFile.delete();
        } catch (IOException ioe) {
            log.debug("Failed to add " + payload + " to item " + oid, ioe);
        }
    }

    /**
     * FedoraId remove payload method NOTE: note sure if it's working yet...
     * 
     * @param oid
     * @param pid
     */
    public void removePayload(String oid, String pid) {
        String fedoraId;
        try {
            fedoraId = getFedoraId(oid);
            Payload payload = getPayload(oid, pid);
            if (fedoraId != null && payload != null) {
                client.purgeDatastream(fedoraId, payload.getId());
            }
        } catch (IOException e) {
            log.debug("Failed to remove " + pid + " from item " + oid, e);
        }

    }

    /**
     * Get Digital object method
     * 
     * @param oid
     * @return DigitalObject
     */
    public DigitalObject getObject(String oid) {
        try {
            if (oid == null || oid.equals("")) {
                return null;
            }
            String fedoraId = getFedoraId(oid);
            if (fedoraId != null) {
                log.debug("Successfully getting object: " + oid
                        + ", with fedoraid: " + fedoraId);
                return new Fedora3DigitalObject(client, fedoraId, oid);
            }
        } catch (IOException ioe) {
            log.debug("Failed to getObject: " + oid);
        }
        return null;
    }

    /**
     * Get Payload from DigitalObject method
     * 
     * @param oid
     * @param pid
     * @return Payload
     */
    public Payload getPayload(String oid, String pid) {
        DigitalObject object = getObject(oid);
        if (object != null) {
            return object.getPayload(pid);
        }
        return null;
    }

    /**
     * Get fedora id method
     * 
     * @param oid
     * @return
     * @throws IOException
     */
    private String getFedoraId(String oid) throws IOException {
        // TODO cache oid lookups?
        String pid = null;
        ResultType result = client.findObjects(oid, 1);
        List<ObjectFieldType> objects = result.getObjectFields();
        if (!objects.isEmpty()) {
            pid = objects.get(0).getPid();
        }
        // Note: Fedora resumeFindObjects has to be called until the search
        // session is completed or the server will hang after approximately 100
        // requests
        ListSessionType session = result.getListSession();
        while (session != null) {
            log.debug("resumeFindObjects session to close connection...");
            result = client.resumeFindObjects(session.getToken());
            if (result != null) {
                session = result.getListSession();
            } else {
                session = null;
            }
        }
        return pid;
    }

    /**
     * Initialisation method that accept jsonString
     * 
     * @param jsonString
     */
    @Override
    public void init(String jsonString) throws PluginException {

    }

    /**
     * Get List of FedoraDigitalObject *** To be implemented, this will be used
     * for reindexing
     */
    @Override
    public List<DigitalObject> getObjectList() {
        // Try to use findObjects but need to know what's the term value to
        // return all objects
        return null;
    }
}
