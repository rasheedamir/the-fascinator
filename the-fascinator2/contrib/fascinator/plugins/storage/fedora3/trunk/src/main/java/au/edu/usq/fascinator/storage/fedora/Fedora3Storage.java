/* 
 * The Fascinator - Fedora Commons 3.x storage plugin
 * Copyright (C) 2009-2011 University of Southern Queensland
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
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fedora.RestClient;
import au.edu.usq.fedora.types.ObjectFieldType;
import au.edu.usq.fedora.types.ResultType;

/**
 * This plugin uses the Fedora Commons 3.x software for storage of Fascinator
 * DigitalObjects.
 *
 * <h3>Configuration</h3>
 * <table border="1">
 * <tr>
 * <th>Option</th>
 * <th>Description</th>
 * <th>Required</th>
 * <th>Default</th>
 * </tr>
 * <tr>
 * <td>url</td>
 * <td>Base URL of a Fedora Commons server</td>
 * <td><b>Yes</b></td>
 * <td>http://localhost:8080/fedora</td>
 * </tr>
 * <td>username</td>
 * <td>Fedora user account with read/write access</td>
 * <td><b>Yes</b> (depending on server setup)</td>
 * <td>fedoraAdmin</td>
 * </tr>
 * </tr>
 * <td>password</td>
 * <td>Password for the above user account</td>
 * <td><b>Yes</b> (depending on server setup)</td>
 * <td>fedoraAdmin</td>
 * </tr>
 * <td>namespace</td>
 * <td>Namespace to use for Fedora Object PIDs</td>
 * <td>No</td>
 * <td>uuid</td>
 * </tr>
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
 * <h3>Further reading</h3>
 * <p>
 * See https://fascinator.usq.edu.au/trac/wiki/Fascinator/Documents/Plugins/Storage/Fedora3
 * for a more detailed description of this plugin.
 * </p>
 *
 * @author Linda Octalina
 * @author Oliver Lucido
 */
public class Fedora3Storage implements Storage {

    /** Default Fedora base URL **/
    private static final String DEFAULT_URL = "http://localhost:8080/fedora";

    /** Default Fedora namespace **/
    private static final String DEFAULT_NAMESPACE = "uuid";

    /** Logger */
    private Logger log = LoggerFactory.getLogger(Fedora3Storage.class);

    /** Fedora REST API client */
    private RestClient client;

    private String namespace;

    @Override
    public String getId() {
        return "fedora3";
    }

    @Override
    public String getName() {
        return "Fedora Commons 3.x Storage Plugin";
    }

    @Override
    public void init(File jsonFile) throws StorageException {
        try {
            init(new JsonConfig(jsonFile));
        } catch (IOException ioe) {
            throw new StorageException("Failed to read file configuration!");
        }
    }

    @Override
    public void init(String jsonString) throws PluginException {
        try {
            init(new JsonConfig(jsonString));
        } catch (IOException ioe) {
            throw new StorageException("Failed to read string configuration!");
        }
    }

    private void init(JsonConfig config) throws StorageException {
        try {
            String url = config.get("storage/fedora3/url", DEFAULT_URL);
            client = new RestClient(url);
            String userName = config.get("storage/fedora3/username");
            String password = config.get("storage/fedora3/password");
            namespace = config.get("storage/fedora3/namespace",
                    DEFAULT_NAMESPACE);
            if (userName != null && password != null) {
                client.authenticate(userName, password);
            } else {
                throw new StorageException(
                        "Username or password must be specified!");
            }
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void shutdown() throws StorageException {
        // Don't need to do anything on shutdown
    }

    @Override
    public PluginDescription getPluginDetails() {
        return new PluginDescription(this);
    }

    @Override
    public DigitalObject createObject(String oid) throws StorageException {
        // log.debug("createObject({})", oid);
        try {
            String fedoraPid = getFedoraPid(oid);
            if (fedoraPid == null) {
                fedoraPid = client.createObject(namespace + ":" + oid, oid,
                        namespace);
            } else {
                throw new StorageException("oID '" + oid
                        + "' already exists in storage.");
            }
            return new Fedora3DigitalObject(oid, fedoraPid, client);
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
    }

    @Override
    public DigitalObject getObject(String oid) throws StorageException {
        // log.debug("getObject({})", oid);
        try {
            String fedoraPid = getFedoraPid(oid);
            if (fedoraPid != null) {
                return new Fedora3DigitalObject(oid, fedoraPid, client);
            }
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
        throw new StorageException("oID '" + oid
                + "' doesn't exist in storage.");
    }

    @Override
    public void removeObject(String oid) throws StorageException {
        // log.debug("removeObject({})", oid);
        try {
            String fid = getFedoraPid(oid);
            if (fid == null) {
                throw new StorageException("oID '" + oid + "' not found.");
            } else {
                client.purgeObject(fid);
            }
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
    }

    @Override
    public Set<String> getObjectIdList() {
        throw new RuntimeException("Not supported!");
    }

    private String getFedoraPid(String oid) throws IOException {
        // TODO cache oid lookups?
        String fid = null;
        ResultType result = client.findObjects(oid, 1);
        List<ObjectFieldType> objects = result.getObjectFields();
        if (!objects.isEmpty()) {
            fid = objects.get(0).getPid();
        }
        /*
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
        */
        return fid;
    }
}
