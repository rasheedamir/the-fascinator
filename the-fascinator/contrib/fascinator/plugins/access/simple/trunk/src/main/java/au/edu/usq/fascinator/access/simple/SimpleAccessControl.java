/*
 * The Fascinator - Simple Access Control plugin
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
package au.edu.usq.fascinator.access.simple;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.access.AccessControl;
import au.edu.usq.fascinator.api.access.AccessControlException;
import au.edu.usq.fascinator.api.access.AccessControlSchema;
import au.edu.usq.fascinator.api.authentication.AuthenticationException;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.DummyFileLock;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Simple, file based access control.
 *
 * @author Greg Pendlebury
 */
public class SimpleAccessControl implements AccessControl {

    private static String DEFAULT_FILE_NAME = "access.properties";
    private final Logger log = LoggerFactory.getLogger(SimpleAccessControl.class);
    private SimpleSchema schema_object;
    private String file_path;
    private Properties file_store;
    private DummyFileLock file_lock;
    private Map<String, List<String>> record_list;

    @Override
    public String getId() {
        return "simple";
    }

    @Override
    public String getName() {
        return "Simple Access Control";
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
     * Initialisation of Simple Access Control plugin
     * 
     * @throws AuthenticationException if fails to initialise
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
     * Set default configuration method
     * 
     * @param config JSON configuration
     * @throws IOException if fail to initialise
     */
    private void setConfig(JsonConfig config) throws IOException {
        // Get the basics
        schema_object = new SimpleSchema();
        file_path   = config.get("accesscontrol/simple/path", null);
        file_lock = new DummyFileLock(file_path + ".lock");
        file_store = new Properties();
        // Parse/create our settings file
        loadRoles();
        // And release... no need to hold it locked until needed
        file_lock.release();
    }

    /** 
     * Simple load wrappers
     * 
     * @throws AccessControlException if fail to load roles
     */
    private void load() throws AccessControlException {
        try {
            loadRoles();
        } catch (IOException e) {
            throw new AccessControlException(e);
        }
    }

    /**
     * Simple release wrapper
     * 
     * @throws AccessControlException if fail to release file lock
     */
    private void release() throws AccessControlException {
        try {
            file_lock.release();
        } catch (IOException e) {
            throw new AccessControlException(e);
        }
    }

    /**
     * Load roles from disk
     * 
     * @throws IOException if fail to load roles from disk
     */
    private void loadRoles() throws IOException {
        // Load our userbase from disk
        try {
            // The real file, load defaults if it doesn't exist
            File access_file = new File(file_path);
            if (!access_file.exists()) {
                access_file.getParentFile().mkdirs();
                access_file.createNewFile();
            }
            // Our dummy lock file
            access_file = new File(file_path + ".lock");
            if (!access_file.exists()) {
                access_file.createNewFile();
            }

            file_lock.getLock();
            file_store.load(new FileInputStream(file_path));
            record_list = new LinkedHashMap<String, List<String>>();
            List<String> security_roles = new ArrayList();

            // Loop through all records
            String[] records = file_store.keySet().toArray(new String[file_store.size()]);
            for (String record : records) {
                security_roles = new ArrayList();

                for (String role : file_store.getProperty(record).split(",")) {
                    security_roles.add(role);
                }

                record_list.put(record, security_roles);
            }
        } catch (Exception e) {
            throw new IOException (e);
        }
    }

    /**
     * Save roles to disk
     * 
     * @throws IOException If fail to save roles
     */
    private void saveRoles() throws IOException {
        if (file_store != null) {
            try {
                file_store.store(new FileOutputStream(file_path), "");
            } catch (Exception e) {
                throw new IOException (e);
            }
        }
    }

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
        schema_object = new SimpleSchema();
        return schema_object;
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
        load();
        if (record_list.containsKey(recordId)) {
            List<String> roles = record_list.get(recordId);
            List<AccessControlSchema> schemas = new ArrayList();
            SimpleSchema schema;

            for (String role : roles) {
                schema = new SimpleSchema();
                schema.init(recordId);
                schema.set("role", role);
                schemas.add(schema);
            }
            release();
            return schemas;
        } else {
            release();
            return new ArrayList();
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
        load();
        // Find the record
        String record = newSecurity.getRecordId();
        if (record == null || record.equals("")) {
            release();
            throw new AccessControlException("No record provided by schema.");
        }

        // Find the new role
        String role = newSecurity.get("role");
        if (role == null || role.equals("")) {
            release();
            throw new AccessControlException("No security role provided by schema.");
        }

        // Get it's current security
        List<String> role_list = record_list.get(record);
        if (role_list == null) role_list = new ArrayList();
        if (role_list.contains(role)) {
            release();
            throw new AccessControlException("Duplicate! That role has already been applied to this record.");
        }

        // Add to the security
        role_list.add(role);
        record_list.put(record, role_list);

        // Don't forget to update our file_store
        String roles = StringUtils.join(role_list.toArray(new String[0]), ",");
        file_store.setProperty(record, roles);
        try {
            saveRoles();
            release();
        } catch (IOException e) {
            release();
            throw new AccessControlException(e);
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
        load();
        // Find the record
        String record = oldSecurity.getRecordId();
        if (record == null || record.equals("")) {
            release();
            throw new AccessControlException("No record provided by schema.");
        }

        // Find the new role
        String role = oldSecurity.get("role");
        if (role == null || role.equals("")) {
            release();
            throw new AccessControlException("No security role provided by schema.");
        }

        // Get its current security
        List<String> role_list = record_list.get(record);
        if (role_list == null || !role_list.contains(role)) {
            release();
            throw new AccessControlException("That role does not have access to this record.");
        }

        // Remove from security
        role_list.remove(role);
        record_list.put(record, role_list);

        // Don't forget to update our file_store
        String roles = StringUtils.join(role_list.toArray(new String[0]), ",");
        file_store.setProperty(record, roles);
        try {
            saveRoles();
            release();
        } catch (IOException e) {
            release();
            throw new AccessControlException(e);
        }
    }

    /**
     * A basic wrapper for getSchemas() to return ust the roles of the schemas.
     * Useful during index and/or audit when this is the only data required.
     *
     * @param recordId The record to retrieve roles for.
     * @return A list fo Strings containing role names.
     * @throws AccessControlException if there was an error during retrieval.
     */
    @Override
    public List<String> getRoles(String recordId)
            throws AccessControlException {
        load();
        if (record_list.containsKey(recordId)) {
            release();
            return record_list.get(recordId);
        } else {
            release();
            return null;
        }
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
        throw new AccessControlException("Not supported by this plugin. Use any freetext role name.");
    }

}
