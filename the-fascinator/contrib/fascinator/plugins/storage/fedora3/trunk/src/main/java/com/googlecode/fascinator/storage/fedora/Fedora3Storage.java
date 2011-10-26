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

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.JsonSimpleConfig;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

import org.apache.axis.types.NonNegativeInteger;
import org.apache.commons.codec.digest.DigestUtils;
import org.fcrepo.server.types.gen.FieldSearchQuery;
import org.fcrepo.server.types.gen.FieldSearchResult;
import org.fcrepo.server.types.gen.ListSession;
import org.fcrepo.server.types.gen.ObjectFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This plugin provides storage using version 3.5 of the
 * <a href="http://www.fedora-commons.org/">Fedora Commons</a> Client.
 * It has been tested against v3.5, v3.3.1 and v2.2.4 Fedora servers,
 * but will probably support others as well.</p>
 *
 * <h3>Configuration</h3>
 * <table border="1">
 *   <tr>
 *     <th>Option</th>
 *     <th>Description</th>
 *     <th>Required</th>
 *     <th>Default</th>
 *   </tr>
 *   <tr>
 *     <td>url</td>
 *     <td>Base URL of a Fedora Commons server</td>
 *     <td><b>Yes</b></td>
 *     <td>http://localhost:8080/fedora</td>
 *   </tr>
 *   <tr>
 *     <td>username</td>
 *     <td>Fedora user account with read/write access</td>
 *     <td><b>Yes</b> (depending on server setup)</td>
 *     <td>fedoraAdmin</td>
 *   </tr>
 *   <tr>
 *     <td>password</td>
 *     <td>Password for the above user account</td>
 *     <td><b>Yes</b> (depending on server setup)</td>
 *     <td>fedoraAdmin</td>
 *   </tr>
 *   <tr>
 *     <td>namespace</td>
 *     <td>Namespace to use for Fedora Object PIDs</td>
 *     <td>No</td>
 *     <td>uuid</td>
 *   </tr>
 * </table>
 *
 * <h3>Sample configuration</h3>
 * <pre>
 * {
 *     "storage": {
 *         "type": "fedora3",
 *         "fedora3": {
 *             "url": "http://localhost:8080/fedora",
 *             "username": "fedoraAdmin",
 *             "password": "fedoraAdmin",
 *             "namespace": "uuid"
 *         }
 *     }
 * }
 * </pre>
 *
 * @author Linda Octalina
 * @author Oliver Lucido
 * @author Greg Pendlebury
 */
public class Fedora3Storage implements Storage {
    /** How many records are we ever willing to receive per result set */
    private static int SEARCH_ROW_LIMIT_PER_PAGE = 1000;

    /** FOXML Version String to send to Fedora */
    private static String FOXML_VERSION = "info:fedora/fedora-system:FOXML-1.1";

    /** Fedora log message for adding an object */
    private static String ADD_LOG_MESSAGE =
            "Fedora3DigitalObject added";

    /** Fedora log message for deleting an object */
    private static String DELETE_LOG_MESSAGE =
            "Fedora3DigitalObject deleted";

    /** Logger */
    private Logger log = LoggerFactory.getLogger(Fedora3Storage.class);

    /** System Config */
    private JsonSimpleConfig systemConfig;

    /** FOXML Template to use at object creation */
    private String foxmlTemplate;

    /**
     * Return the ID of this plugin.
     * 
     * @return String the plugin's ID.
     */
    @Override
    public String getId() {
        return "fedora3";
    }

    /**
     * Return the name of this plugin.
     * 
     * @return String the plugin's name.
     */
    @Override
    public String getName() {
        return "Fedora Commons 3.5 Storage Plugin";
    }

    /**
     * Public init method for File based configuration.
     * 
     * @param jsonFile The File containing JSON configuration
     * @throws StorageException if any errors occur
     */
    @Override
    public void init(File jsonFile) throws StorageException {
        try {
            systemConfig = new JsonSimpleConfig(jsonFile);
            Fedora3.init(jsonFile);
            init();
        } catch (IOException ioe) {
            throw new StorageException(
                    "Failed to read file configuration!", ioe);
        }
    }

    /**
     * Public init method for String based configuration.
     * 
     * @param jsonString The String containing JSON configuration
     * @throws StorageException if any errors occur
     */
    @Override
    public void init(String jsonString) throws StorageException {
        try {
            systemConfig = new JsonSimpleConfig(jsonString);
            Fedora3.init(jsonString);
            init();
        } catch (IOException ioe) {
            throw new StorageException(
                    "Failed to read string configuration!", ioe);
        }
    }

    /**
     * Initialisation occurs here
     * 
     * @throws StorageException if any errors occur
     */
    private void init() throws StorageException {
        // A quick connection test
        Fedora3.getClient();

        // Do we have a template?
        String templatePath = systemConfig.getString(null,
                "storage", "fedora3", "foxmlTemplate");
        File templateFile = null;
        if (templatePath != null) {
            templateFile = new File(templatePath);
        } else {
            URL url = getClass().getResource("/foxml_template.xml");
            try {
                templateFile = new File(url.toURI());
            } catch (URISyntaxException ex) {
                throw new StorageException("Error; Unable to read new object "
                        + "template from disk: '" + url.getPath() + "'");
            }
        }

        // Read the template into a String
        if (!templateFile.exists()) {
            throw new StorageException("Error; The new object template"
                    + " provided does not exist: '" + templatePath + "'");
        }
        foxmlTemplate = fileToString(templateFile);
        if (foxmlTemplate == null) {
            throw new StorageException("Error; Unable to read new object "
                    + "template from disk: '" + templatePath + "'");
        }
    }

    /**
     * Not part of the API, but used in unit testing. Check the version
     * of the connected Fedora Server
     * 
     * @throws String The Fedora Server's version
     */
    public String fedoraVersion() {
        return Fedora3.getVersion();
    }

    /**
     * Initialisation occurs here
     * 
     * @throws StorageException if any errors occur
     */
    @Override
    public void shutdown() throws StorageException {
        // Don't need to do anything on shutdown
    }

    /**
     * Retrieve the details for this plugin
     * 
     * @return PluginDescription a description of this plugin
     */
    @Override
    public PluginDescription getPluginDetails() {
        return new PluginDescription(this);
    }

    /**
     * Create a new object in storage. An object identifier may be provided, or
     * a null value will try to have Fedora auto-generate the new OID.
     * 
     * @param oid the Object ID to use during creation, null is allowed
     * @return DigitalObject the instantiated DigitalObject created
     * @throws StorageException if any errors occur
     */
    @Override
    public DigitalObject createObject(String oid) throws StorageException {
        //log.debug("createObject({})", oid);
        if (oid == null) {
            throw new StorageException("Error; Null OID recieved");
        }
        String fedoraPid = safeFedoraPid(oid);
        byte[] data = null;

        // Can we see object?
        try {
            data = Fedora3.getApiM().getObjectXML(fedoraPid);
            if (data != null && data.length > 0) {
                throw new StorageException(
                        "Error; object '"+oid+"' already exists in Fedora");
            }
        } catch (RemoteException ex) {
            // This is OK... object does not exist
        } catch (Exception ex) {
            throw new StorageException("Error accessing Fedora", ex);
        }

        // New content
        try {
            data = prepareTemplate(fedoraPid, oid);
            String responsePid = Fedora3.getApiM().ingest(
                    data,             // XML Content
                    FOXML_VERSION,    // Format
                    ADD_LOG_MESSAGE); // Log message
            if (!fedoraPid.equals(responsePid)) {
                log.error("Error; PID Mismatch during creation. We sent '{}'"
                        + " but Fedora used '{}'", fedoraPid, responsePid);
                removeFedoraObject(responsePid);
                throw new StorageException("Error with Fedora PIDs. Please"
                        + " check your system logs and configuration!");
            }

            // Instantiate and return
            return new Fedora3DigitalObject(oid, fedoraPid);
        } catch (RemoteException ex) {
            throw new StorageException("Error during Fedora search", ex);
        } catch (StorageException ex) {
            throw new StorageException("Error accessing Fedora", ex);
        }
    }

    /**
     * Get the indicated object from storage.
     * 
     * @param oid the Object ID to retrieve
     * @return DigitalObject the instantiated DigitalObject requested
     * @throws StorageException if any errors occur
     */
    @Override
    public DigitalObject getObject(String oid) throws StorageException {
        //log.debug("getObject({})", oid);
        if (oid == null) {
            throw new StorageException("Error; Null OID recieved");
        }
        String fedoraPid = safeFedoraPid(oid);
        try {
            // Confirm we can see the object in Fedora
            byte[] data = Fedora3.getApiM().getObjectXML(fedoraPid);
            if (data == null || data.length == 0) {
                throw new StorageException(
                        "Error; could not find object '"+oid+"' in Fedora");
            }
            // Instantiate and return
            return new Fedora3DigitalObject(oid, fedoraPid);
        } catch (RemoteException ex) {
            throw new StorageException("Error during Fedora search", ex);
        } catch (StorageException ex) {
            throw new StorageException("Error accessing Fedora", ex);
        }
    }

    /**
     * Remove the indicated object from storage.
     * 
     * @param oid the Object ID to remove from storage
     * @throws StorageException if any errors occur
     */
    @Override
    public void removeObject(String oid) throws StorageException {
        //log.debug("removeObject({})", oid);
        if (oid == null) {
            throw new StorageException("Error; Null OID recieved");
        }
        String fedoraPid = safeFedoraPid(oid);
        removeFedoraObject(fedoraPid);
    }

    /**
     * Perform the actual removal from Fedora
     * 
     * @param fedoraPid the Fedora PID to remove from storage
     * @throws StorageException if any errors occur
     */
    private void removeFedoraObject(String fedoraPid) throws StorageException {
        try {
            Fedora3.getApiM().purgeObject(fedoraPid, DELETE_LOG_MESSAGE, false);
        } catch (RemoteException ex) {
            throw new StorageException("Error during Fedora search", ex);
        } catch (StorageException ex) {
            throw new StorageException("Error accessing Fedora", ex);
        }
    }

    /**
     * Return a list of Object IDs currently in storage.
     * 
     * @return Set<String> A Set containing all the OIDs in storage.
     */
    @Override
    public Set<String> getObjectIdList() {
        log.info("Complete storage OID list requested..."); 
        // Our response set
        Set<String> objectList = new HashSet<String>();
        // Search constants
        NonNegativeInteger limit = Fedora3.axisNum(SEARCH_ROW_LIMIT_PER_PAGE);
        String[] fields = new String[] {"pid", "label"};
        FieldSearchQuery query = new FieldSearchQuery();
        query.setTerms(Fedora3.namespace()+":*");

        try {
            log.info("... searching Fedora"); 
            FieldSearchResult result = Fedora3.getApiA().findObjects(
                    fields, limit, query);
            while (result != null) {
                for (ObjectFields object : result.getResultList()) {
                    objectList.add(object.getLabel());
                }
                ListSession session = result.getListSession();
                if (session != null) {
                    log.info("... searching Fedora : row(s) {}+",
                            session.getCursor().intValue() + 1); 
                    result = Fedora3.getApiA().resumeFindObjects(
                            session.getToken());
                } else {
                    result = null;
                }
            }
            return objectList;
        } catch (RemoteException ex) {
            log.error("Error during Fedora search: ", ex);
            return null;
        } catch (StorageException ex) {
            log.error("Error accessing Fedora: ", ex);
            return null;
        }
    }

    /**
     * Translate a Fascinator OID into a hashed Fedora ID with namespace.
     * Should prevent any issues related to special characters being used in IDs
     * 
     * @param oid the Object ID from Fascinator
     * @return String the Fedora PID to use
     */
    private String safeFedoraPid(String oid) {
        return Fedora3.namespace() + ":" + DigestUtils.md5Hex(oid);
    }

    /**
     * Reads a File into a String.
     * 
     * @param file the File to read
     * @return String the Fedora PID to use
     */
    private String fileToString(File file) {
        // Prepare
        int expectedLength = (int) file.length();
        byte[] buffer = new byte[expectedLength];
        BufferedInputStream inputStream = null;
        try {
            // Perform
            inputStream = new BufferedInputStream(new FileInputStream(file));
            int bytesRead = inputStream.read(buffer);
            // Validate
            if (bytesRead != expectedLength) {
                log.error("Error reading file data; {} bytes read; expected {}",
                        bytesRead, expectedLength);
                return null;
            }

        } catch (Exception ex) {
            log.error("Error accessing file '{}': ", file.getName(), ex);
            return null;
        } finally {
            Fedora3.close(inputStream);
        }
        return new String(buffer);
    }

    /**
     * Prepare a FOXML Template for Fedora, including the provided PID.
     * 
     * @param pid The desired Fedora PID
     * @return byte[] The evaluated template as a byte array to send to Fedora
     */
    private byte[] prepareTemplate(String pid, String oid) {
        String output = foxmlTemplate.replace("[[PID]]", pid);
        output = output.replace("[[OID]]", oid);
        try {
            return output.getBytes("UTF-8");
        } catch (Exception ex) {
            log.error("Encoding error in template: ", ex);
            return null;
        }
    }
}
