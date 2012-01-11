/*
 * The Fascinator - Solr Access Control plugin
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
package au.edu.usq.fascinator.access.solr;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.access.AccessControl;
import au.edu.usq.fascinator.api.access.AccessControlException;
import au.edu.usq.fascinator.api.access.AccessControlSchema;
import au.edu.usq.fascinator.api.authentication.AuthenticationException;
import au.edu.usq.fascinator.common.JsonConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.request.DirectXmlRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Fascinator access control plugin implementation using a security solr core
 *
 * @author Greg Pendlebury
 */
public class SolrAccessControl implements AccessControl {

    /** Logging */
    private final Logger log = LoggerFactory.getLogger(SolrAccessControl.class);

    /** Solr URI */
    private URI uri;

    /** Solr Core */
    private CommonsHttpSolrServer core;

    /**
     * Gets an identifier for this type of plugin. This should be a simple name
     * such as "file-system" for a storage plugin, for example.
     *
     * @return the plugin type id
     */
    @Override
    public String getId() {
        return "solr";
    }

    /**
     * Gets a name for this plugin. This should be a descriptive name.
     *
     * @return the plugin name
     */
    @Override
    public String getName() {
        return "Solr Access Control";
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
        try {
            // Find our solr index
            uri = new URI(config.get("accesscontrol/solr/uri"));
            core = new CommonsHttpSolrServer(uri.toURL());
            // Small sleep whilst the solr index is still coming online
            Thread.sleep(200);
            // Make sure it is online
            core.ping();
        } catch (Exception ex) {
            throw new AccessControlException(ex);
        }
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
        return new SolrSchema();
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
        List<AccessControlSchema> result = new ArrayList();
        try {
            result = search(recordId);
        } catch (Exception ex) {
            log.error("Error searching security index: ", ex);
            throw new AccessControlException("Error searching security index");
        }
        return result;
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

        // Get record's current security
        List<String> role_list = getRoles(recordId);
        if (role_list != null && role_list.contains(role)) {
            throw new AccessControlException("Duplicate! That role has " +
                    "already been applied to this record.");
        }

        // Add the new relationship to the index
        try {
            addToIndex(recordId, role);
        } catch (Exception ex) {
            log.error("Error updating security index: ", ex);
            throw new AccessControlException("Error updating security index");
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

        // Get its current security
        List<String> role_list = getRoles(recordId);
        if (role_list == null || !role_list.contains(role)) {
            throw new AccessControlException(
                    "That role does not have access to this record.");
        }

        // Remove from security
        try {
            removeFromIndex(recordId, role);
        } catch (Exception ex) {
            log.error("Error updating security index: ", ex);
            throw new AccessControlException("Error updating security index");
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
        List<AccessControlSchema> schemas = getSchemas(recordId);
        List<String> roles = new ArrayList();

        for (AccessControlSchema schema : schemas) {
            roles.add(schema.get("role"));
        }

        if (roles.isEmpty()) {
            return null;
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

    private List<AccessControlSchema> search(String recordId) throws Exception {
        return search(recordId, null);
    }

    private List<AccessControlSchema> search(String recordId, String role)
            throws Exception {
        // An empty response
        List<AccessControlSchema> schemas = new ArrayList();
        SolrSchema schema;

        // Run the query
        String q = writeQueryString(recordId, role);
        SolrQuery query = new SolrQuery().setQuery(q);
        QueryResponse result = core.query(query);

        // Loop through the responses
        Iterator<SolrDocument> iter = result.getResults().iterator();
        while (iter.hasNext()) {
            // Get the details
            SolrDocument resultDoc = iter.next();
            String rRecord = (String) resultDoc.getFieldValue("recordId");
            String rRole   = (String) resultDoc.getFieldValue("role");
            // Build a schema from details
            schema = new SolrSchema();
            schema.init(rRecord);
            schema.set("role", rRole);
            // Add to our response
            schemas.add(schema);
        }

        return schemas;
    }

    private void removeFromIndex(String recordId, String role)
            throws Exception {
        String doc = writeDeleteString(recordId, role);
        core.request(new DirectXmlRequest("/update", doc));
        core.commit();
    }

    private void addToIndex(String recordId, String role) throws Exception {
        String doc = writeUpdateString(recordId, role);
        core.request(new DirectXmlRequest("/update", doc));
        core.commit();
    }

    private String writeDeleteString(String recordId, String role) {
        String fRecord = "recordId:\"" + recordId + "\"";
        String fRole   = "role:\"" + role + "\"";
        return "<delete><query>" + fRecord + " AND "
                + fRole + "</query></delete>";
    }

    private String writeQueryString(String recordId, String role) {
        String fRecord = "recordId:\"" + recordId + "\"";
        if (role == null) {
            return fRecord;
        } else {
            return fRecord + " AND role:\"" + role + "\"";
        }
    }

    private String writeUpdateString(String recordId, String role) {
        String fRecord = "<field name=\"recordId\">" + recordId + "</field>";
        String fRole   = "<field name=\"role\">" + role + "</field>";
        return "<add><doc>" + fRecord + fRole + "</doc></add>";
    }
}
