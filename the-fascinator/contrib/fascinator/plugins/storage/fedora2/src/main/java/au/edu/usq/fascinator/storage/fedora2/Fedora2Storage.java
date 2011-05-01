/* 
 * The Fascinator - Fedora2 storage plugin
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
package au.edu.usq.fascinator.storage.fedora2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fedora.client.FedoraClient;
import fedora.server.management.FedoraAPIM;
import fedora.server.utilities.StreamUtility;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.FascinatorHome;
import au.edu.usq.fascinator.common.JsonConfig;

/**
 * Fedora2 storage plugin Using SOAP
 * 
 * @author Linda Octalina
 */
public class Fedora2Storage implements Storage {

    private static final String FOXML_TEMPLATE = "/fedora/foxml11_template.xml";
    
    /** Logger **/
    private Logger log = LoggerFactory.getLogger(Fedora2Storage.class);

    /** Fedora management API */
    private FedoraAPIM manage;

    /** Fedora client */
    private FedoraClient client;

    /** Whether or not a connection to the registry is established. */
    private boolean connected;
    
    private String baseUrl;
    
    private String userName;
    
    private String password;

    @Override
    public String getId() {
        return "fedora2";
    }

    @Override
    public String getName() {
        return "Fedora Commons 2.x Storage Module";
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
     * Fedora3 storage initialisation method
     * 
     * @param jsonFile
     */
    public void init(File jsonFile) throws StorageException {
        try {
            JsonConfig config = new JsonConfig(jsonFile);
            baseUrl = config.get("storage/fedora3/url");

            userName = config.get("storage/fedora3/username");
            password = config.get("storage/fedora3/password");

            
            //            if (userName != null && password != null) {
            //                client.authenticate(userName, password);
            //            } else {
            //                throw new StorageException("Not Fedora 3");
            //            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new StorageException(e);
        }
    }

    public void connect() throws StorageException {
        if (connected) {
            log.warn("Connect request already connected...");
        } else {
                try {
                    client = new FedoraClient(baseUrl, userName, password);
                    manage = client.getAPIM();
                    connected = true;
                } catch (MalformedURLException e) {
                    throw new StorageException("Malformed url exception: " + e.getMessage());
                } catch (Exception e) {
                    throw new StorageException("Get APIM exception: " + e.getMessage());
                }
        }
    }
    
    public String createObject(Map<String, String> options)
              throws StorageException {
      
              if (!connected) {
                  throw new StorageException("Not connected");
              }
      
             String pid = null;
             String comment = "";
             if (options != null && options.containsValue("comment")) {
                 comment = options.get("comment");
             }
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             try {
                 StreamUtility.pipeStream(getClass().getResourceAsStream(
                     FOXML_TEMPLATE), out, 4096);
//                 pid = manage.ingest(out.toByteArray(),
//                     FedoraClient.FOXML1_1.toString(), comment);
                 log.debug("Created new object: PID = " + pid);
             } catch (Exception e) {
                 throw new StorageException(e);
             }
             return pid;
         }
     
         public void addDatastream(String pid, String dsId, InputStream data,
             Map<String, String> options) throws StorageException {
     
             if (!connected) {
                 throw new StorageException("Not connected");
             }
     
             try {
                 String[] altIds = new String[] {};
                 String dsLabel = options.get("dsLabel");
                 boolean versionable = true;
                 String mimeType = options.get("mimeType");
                 String formatUri = "";
                 String controlGroup = options.get("controlGroup");
                 String dsState = "A"; // Active;
                 String logMessage = options.get("logMessage");
     
                 File dcFile = File.createTempFile("f3r_tmp", ".xml");
                 FileOutputStream fos = new FileOutputStream(dcFile);
                 StreamUtility.pipeStream(data, fos, 4096);
                 fos.close();
                 String dsLocation = client.uploadFile(dcFile);
                 manage.addDatastream(pid, dsId, altIds, dsLabel, versionable,
                     mimeType, formatUri, dsLocation, controlGroup, dsState,
                     "DISABLED", "none", logMessage);
     
                 log.info(String.format("Added datastream; ID = %s, Label = %s",
                     dsId, dsLabel));
             } catch (Exception e) {
                 throw new StorageException(e);
             }
         }

        @Override
        public void init(String jsonString) throws PluginException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void shutdown() throws PluginException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public DigitalObject createObject(String oid) throws StorageException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public DigitalObject getObject(String oid) throws StorageException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void removeObject(String oid) throws StorageException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Set<String> getObjectIdList() {
            // TODO Auto-generated method stub
            return null;
        }
     }
