/*
 * The Fascinator - Couch Access Control plugin
 * Copyright (C) 2008-2010 University of Southern Queensland
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
package au.edu.usq.fascinator.access.couch;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.access.AccessControl;
import au.edu.usq.fascinator.api.access.AccessControlException;
import au.edu.usq.fascinator.api.access.AccessControlSchema;
import au.edu.usq.fascinator.api.authentication.AuthenticationException;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Fascinator access control plugin using CouchDB
 *
 * @author Greg Pendlebury
 */
public class CouchAccessControl implements AccessControl {

    /** Logging */
    private final Logger log = LoggerFactory.getLogger(CouchAccessControl.class);

    /** CouchDB URL */
    private String url;

    /** HTTP Client */
    private HttpClient couch;

    /**
     * Gets an identifier for this type of plugin. This should be a simple name
     * such as "file-system" for a storage plugin, for example.
     *
     * @return the plugin type id
     */
    @Override
    public String getId() {
        return "couch";
    }

    /**
     * Gets a name for this plugin. This should be a descriptive name.
     *
     * @return the plugin name
     */
    @Override
    public String getName() {
        return "CouchDB Access Control";
    }

    /**
     * Gets a PluginDescription object relating to this plugin.
     *
     * @return a PluginDescription
     */
    @Override
    public PluginDescription getPluginDetails() {
        return new PluginDescription(this);
    }

    /**
     * Initializes the plugin using the specified JSON String
     *
     * @param jsonString JSON configuration string
     * @throws PluginException if there was an error in initialization
     */
    @Override
    public void init(String jsonString) throws AccessControlException {
        try {
            JsonConfig config = new JsonConfig(new ByteArrayInputStream(
                    jsonString.getBytes("UTF-8")));
            setConfig(config);
        } catch (UnsupportedEncodingException e) {
            throw new AccessControlException(e);
        } catch (IOException e) {
            throw new AccessControlException(e);
        }
    }

    /**
     * Initializes the plugin using the specified JSON configuration
     *
     * @param jsonFile JSON configuration file
     * @throws AccessControlException if there was an error in initialization
     */
    @Override
    public void init(File jsonFile) throws AccessControlException {
        try {
            JsonConfig config = new JsonConfig(jsonFile);
            setConfig(config);
        } catch (IOException ioe) {
            throw new AccessControlException(ioe);
        }
    }

    /**
     * Initialization of Solr Access Control plugin
     *
     * @param config The configuration to use
     * @throws AuthenticationException if fails to initialize
     */
    private void setConfig(JsonConfig config) throws AccessControlException {
        // Find our couch database
        url = config.get("accesscontrol/couch/uri");
        if (url == null) {
            throw new AccessControlException("CouchDB URL not provided");
        }
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        couch = new HttpClient();
        couch.getParams().setParameter("http.socket.buffer-size", 32000);
    }

    /**
     * Shuts down the plugin
     *
     * @throws AccessControlException if there was an error during shutdown
     */
    @Override
    public void shutdown() throws AccessControlException {
        // Don't need to do anything
    }

    /**
     * Return an empty security schema for the portal to investigate and/or
     * populate.
     *
     * @return An empty security schema
     */
    @Override
    public AccessControlSchema getEmptySchema() {
        return new CouchSchema();
    }

    /**
     * Get a list of schemas that have been applied to a record.
     *
     * @param recordId The record to retrieve information about.
     * @return A list of access control schemas, possibly zero length.
     * @throws AccessControlException if there was an error during retrieval.
     */
    @Override
    public List<AccessControlSchema> getSchemas(String recordId)
            throws AccessControlException {
        try {
            JsonConfigHelper json = get(recordId);
            if (json == null) {
                return new ArrayList();
            }
            List<String> roles = getRoleList(json);
            if (roles.isEmpty()) {
                return new ArrayList();
            }

            List<AccessControlSchema> schemas = new ArrayList();
            CouchSchema schema;
            for (String role : roles) {
                schema = new CouchSchema();
                schema.init(recordId);
                schema.set("role", role);
                schemas.add(schema);
            }
            return schemas;
        } catch (Exception ex) {
            log.error("Error searching security database: ", ex);
            throw new AccessControlException(
                    "Error searching security database");
        }
    }

    /**
     * Apply/store a new security implementation. The schema will already have
     * a recordId as a property.
     *
     * @param newSecurity The new schema to apply.
     * @throws AccessControlException if storage of the schema fails.
     */
    @Override
    public void applySchema(AccessControlSchema newSecurity)
            throws AccessControlException {
        // Find the record
        String recordId = newSecurity.getRecordId();
        if (recordId == null || recordId.equals("")) {
            throw new AccessControlException("No record provided by schema.");
        }

        // Find the new role
        String role = newSecurity.get("role");
        if (role == null || role.equals("")) {
            throw new AccessControlException(
                    "No security role provided by schema.");
        }

        // Retrieve current data
        List<String> role_list;
        JsonConfigHelper json;
        try {
            json = get(recordId);
            // New entry
            if (json == null) {
                this.put(recordId, role);
                return;
            }
        } catch (Exception ex) {
            log.error("Error updating security database: ", ex);
            throw new AccessControlException(
                    "Error updating security database");
        }

        // Check current data
        role_list = getRoleList(json);
        if (role_list != null && role_list.contains(role)) {
            throw new AccessControlException("Duplicate! That role has " +
                    "already been applied to this record.");
        }

        // Add the new relationship to the database
        try {
            role_list.add(role);
            this.put(json, role_list);
        } catch (Exception ex) {
            log.error("Error updating security database: ", ex);
            throw new AccessControlException(
                    "Error updating security database");
        }
    }

    /**
     * Remove a security implementation. The schema will already have
     * a recordId as a property.
     *
     * @param oldSecurity The schema to remove.
     * @throws AccessControlException if removal of the schema fails.
     */
    @Override
    public void removeSchema(AccessControlSchema oldSecurity)
            throws AccessControlException {
        // Find the record
        String recordId = oldSecurity.getRecordId();
        if (recordId == null || recordId.equals("")) {
            throw new AccessControlException("No record provided by schema.");
        }

        // Find the new role
        String role = oldSecurity.get("role");
        if (role == null || role.equals("")) {
            throw new AccessControlException(
                    "No security role provided by schema.");
        }

        // Retrieve current data
        List<String> role_list;
        JsonConfigHelper json;
        try {
            json = get(recordId);
            if (json == null) {
                throw new AccessControlException(
                    "No security on file for this record.");
            }
            role_list = getRoleList(json);
        } catch (Exception ex) {
            log.error("Error updating security database: ", ex);
            throw new AccessControlException(
                    "Error updating security database");
        }

        // Check current data
        if (role_list == null || !role_list.contains(role)) {
            throw new AccessControlException(
                    "That role does not have access to this record.");
        }

        // Remove from security database
        try {
            role_list.remove(role);
            this.put(json, role_list);
        } catch (Exception ex) {
            log.error("Error updating security database: ", ex);
            throw new AccessControlException(
                    "Error updating security database");
        }
    }

    /**
     * A basic wrapper for getSchemas() to return just the roles of the schemas.
     * Useful during index and/or audit when this is the only data required.
     *
     * @param recordId The record to retrieve roles for.
     * @return A list of Strings containing role names.
     * @throws AccessControlException if there was an error during retrieval.
     */
    @Override
    public List<String> getRoles(String recordId)
            throws AccessControlException {
        List<String> roles;
        try {
            JsonConfigHelper json = get(recordId);
            if (json == null) {
                return null;
            }
            roles = getRoleList(json);
        } catch (Exception ex) {
            log.error("Error updating security database: ", ex);
            throw new AccessControlException(
                    "Error updating security database");
        }

        return roles;
    }

    /**
     * Retrieve a list of possible field values for a given field if the plugin
     * supports this feature.
     *
     * @param field The field name.
     * @return A list of String containing possible values
     * @throws AccessControlException if the field doesn't exist or there
     *          was an error during retrieval
     */
    @Override
    public List<String> getPossibilities(String field)
            throws AccessControlException {
        throw new AccessControlException(
                "Not supported by this plugin. Use any freetext role name.");
    }

    private JsonConfigHelper get(String recordId) throws Exception {
        GetMethod rq = null;
        try {
            // Send our GET query
            rq = new GetMethod(url + getIdHash(recordId));
            rq.addRequestHeader("Content-Type", "text/plain; charset=UTF-8");
            int statusCode = couch.executeMethod(rq);

            // Something went awry
            if (statusCode != HttpStatus.SC_OK) {
                // It just wasn't there
                if (statusCode == HttpStatus.SC_NOT_FOUND) {
                    return null;
                }
                // Something else happened
                throw new Exception("Database GET failed!: '" + statusCode +
                        "': " + rq.getResponseBodyAsString());
            }

            // Return the result
            String response = rq.getResponseBodyAsString();
            JsonConfigHelper result = new JsonConfigHelper(response);
            return result;
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (rq != null) {
                rq.releaseConnection();
            }
        }
    }

    private void put(String recordId, String role) throws Exception {
        // An empty docuement
        JsonConfigHelper json = new JsonConfigHelper();
        json.set("_id", getIdHash(recordId));
        // Basic list with the first role
        List<String> roles = new ArrayList();
        roles.add(role);
        // Submit using the normal method
        put(json, roles, "POST");
    }

    private void put(JsonConfigHelper oldRecord, List<String> newRoles)
            throws Exception {
        put(oldRecord, newRoles, "PUT");
    }

    private void put(JsonConfigHelper oldRecord, List<String> newRoles,
            String method) throws Exception {
        EntityEnclosingMethod rq = null;
        try {
            // Prepare to send
            String dataToSend = writeUpdateString(oldRecord, newRoles);
            String recordId = oldRecord.get("_id");

            // Get our connection ready
            if (method.equals("PUT")) {
                rq = new PutMethod(url + recordId);
            } else {
                rq = new PostMethod(url);
            }
            rq.addRequestHeader("Content-Type", "text/plain; charset=UTF-8");
            rq.setRequestBody(dataToSend);
            int statusCode = couch.executeMethod(rq);

            // Valid responses are anywhere in 200 range for this
            if (statusCode < 200 || statusCode > 299) {
                throw new Exception("Database " + method + " failed!: '" +
                        statusCode + "': " + rq.getResponseBodyAsString());
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (rq != null) {
                rq.releaseConnection();
            }
        }
    }

    private String writeUpdateString(JsonConfigHelper oldRecord,
            List<String> newRoles) {
        String id = oldRecord.get("_id");
        String rev = oldRecord.get("_rev");

        List<String> fields = new ArrayList();
        fields.add(writefield("_id", id));
        if (rev != null) {
            fields.add(writefield("_rev", rev));
        }
        fields.add(writeList("roles", newRoles));

        return "{" + StringUtils.join(fields, ",") + "}";
    }

    private String writeList(String field, List<String> data) {
        List<String> values = new ArrayList();
        for (String item : data) {
            values.add("\"" + item + "\"");
        }
        return "\"" + field + "\":[" + StringUtils.join(values, ",") + "]";
    }

    private String writefield(String field, String data) {
        return "\"" + field + "\":\"" + data + "\"";
    }

    private List<String> getRoleList(JsonConfigHelper json) {
        List<String> result = new ArrayList();
        for (Object role : json.getList("roles")) {
            result.add((String) role);
        }
        return result;
    }

    private String getIdHash(String id) {
        return DigestUtils.md5Hex(id);
    }
}
