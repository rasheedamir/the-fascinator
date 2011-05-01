/* 
 * The Fascinator - Plugin - Harvester - Fedora
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

package au.edu.usq.fascinator.harvester.fedora;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import au.edu.usq.fascinator.api.harvester.HarvesterException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.harvester.impl.GenericHarvester;
import au.edu.usq.fascinator.common.storage.StorageUtils;
import au.edu.usq.fascinator.harvester.fedora.restclient.FedoraRestClient;
import au.edu.usq.fascinator.harvester.fedora.restclient.ListSessionType;
import au.edu.usq.fascinator.harvester.fedora.restclient.ObjectFieldType;
import au.edu.usq.fascinator.harvester.fedora.restclient.ResultType;
import au.edu.usq.fascinator.harvester.fedora.restclient.FedoraRestClient.FindObjectsType;

/**
 * Harvest objects in specified fedora repository to the fascinator
 * 
 * @author Linda Octalina, Oliver Lucido
 * 
 */
public class FedoraHarvester extends GenericHarvester {

    /** Logger for FedoraHarvester */
    private Logger log = Logger.getLogger(FedoraHarvester.class);

    /** Default date format */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /** Default date time format */
    public static final String DATETIME_FORMAT = DATE_FORMAT + "'T'hh:mm:ss'Z'";

    /** Default request size */
    private static final int DEFAULT_REQUEST_SIZE = 25;

    /** Base url where the Fedora located */
    private String baseUrl;

    /** Maximum request results should be returned by Fedora */
    private int maxRequests;

    /** Request size */
    private int requestSize;

    /** Fedora RestClient */
    private FedoraRestClient restClient;

    private Unmarshaller um;

    private JAXBContext jc;

    /** Working directory */
    private File workDir;

    /** Search term used for searching */
    private String searchTerms;

    /** Start search */
    private boolean started;

    /** Files queue */
    private Queue<File> searchFiles;

    /**
     * Fedora Harvester Constructor
     * 
     * @param id
     * @param name
     */
    public FedoraHarvester() {
        super("fedora", "Fedora Harvester");
    }

    /**
     * Initialisation of Fedora Harvester plugin
     * 
     * @throws HarvesterException if fails to initialise
     */
    @Override
    public void init() throws HarvesterException {
        JsonConfigHelper config;

        // Read config
        try {
            config = new JsonConfigHelper(getJsonConfig().toString());
        } catch (IOException ex) {
            throw new HarvesterException("Failed reading configuration", ex);
        }

        baseUrl = config.get("harvester/fedora/baseUrl");
        maxRequests = Integer.parseInt(config
                .get("harvester/fedora/maxRequests"));
        requestSize = DEFAULT_REQUEST_SIZE;
        if (config.get("harvester/fedora/requestSize") != null) {
            requestSize = Integer.parseInt(config
                    .get("harvester/fedora/requestSize"));
        }

        if (baseUrl != null) {
            restClient = new FedoraRestClient(baseUrl);
        } else {
            throw new HarvesterException("Fedora baseUrl is not set");
        }

        searchFiles = new LinkedList<File>();
        started = false;
        searchTerms = "*";
        URL u;
        try {
            u = new URL(baseUrl);
            String tmpDir = System.getProperty("java.io.tmpdir");
            String tmpName = u.getHost() + "_" + u.getPort();
            workDir = new File(tmpDir, tmpName);
            workDir.mkdirs();
            try {
                jc = JAXBContext.newInstance(ResultType.class);
                um = jc.createUnmarshaller();
            } catch (JAXBException jaxbe) {
                throw new HarvesterException(jaxbe.getMessage());
            }
        } catch (MalformedURLException e) {
            throw new HarvesterException("Malformed baseUrl: " + baseUrl, e);
        }

        //For now assume since is null....
        Date since = null;
        getItems(since);
    }

    /**
     * Get the next object due to be harvested
     * 
     * @return The next object to harvest, null if none
     */
    public Set<String> getObjectIdList() throws HarvesterException {
        Set<String> pidLists = new HashSet<String>();
        File searchFile = searchFiles.poll();
        log.info("Loading search results: " + searchFile);
        try {
            ResultType results = (ResultType) um.unmarshal(searchFile);
            for (ObjectFieldType object : results.getObjectFields()) {
                String fedoraItemId;
                try {
                    fedoraItemId = createFedoraItem(object.getPid());
                    pidLists.add(fedoraItemId);
                } catch (StorageException e) {
                    throw new HarvesterException(
                            "Fail to create Digital object for: "
                                    + object.getPid(), e);
                }
            }
        } catch (JAXBException jaxbe) {
            throw new HarvesterException(jaxbe);
        } finally {
            //searchFile.delete();
        }
        return pidLists;
    }

    private String createFedoraItem(String objectPid)
            throws HarvesterException, StorageException {
        FedoraItem fedoraItem = new FedoraItem(restClient, objectPid);
        List<FedoraDatastream> datastreams = fedoraItem.getDatastreams();
        String oid = DigestUtils.md5Hex(fedoraItem.getId());
        DigitalObject object = null;

        boolean inStorage = true;
        try {
            object = getStorage().getObject(oid);
        } catch (StorageException ex) {
            inStorage = false;
        }

        // New items
        if (!inStorage) {
            try {
                object = getStorage().createObject(oid);
            } catch (StorageException ex) {
                throw new HarvesterException("Error creating new object: ", ex);
            }
        }

        for (FedoraDatastream datastream : datastreams) {
            try {

                Payload payload = StorageUtils.createOrUpdatePayload(object,
                        datastream.getId(), datastream.getContentAsStream());
                payload.setContentType(datastream.getMimeType());
                payload.close();
            } catch (IOException e) {
                throw new HarvesterException(
                        "Fail to create payload for object: " + objectPid
                                + ", with datastream: " + datastream.getId(), e);
            }
        }

        //Update object metadata
        Properties props = object.getMetadata();
        props.setProperty("render-pending", "true");
        props.setProperty("fedoraPid", objectPid);
        
        String objectId = object.getId();
        object.close();
        return objectId;
    }

    /**
     * Set search term
     * 
     * @param searchTerms used for searching
     */
    public void setSearchItems(String searchTerms) {
        this.searchTerms = searchTerms;
    }

    /**
     * Retrieve the next file specified as a target in configuration
     * 
     * @return The next target file, null if none
     */
    public boolean hasMoreObjects() {
        boolean more = !(started && searchFiles.isEmpty());
        if (!more) {
            log.info("Deleting work directory: " + workDir.getAbsolutePath());
            workDir.delete();
        }
        return more;
    }

    /**
     * Get list of items
     * 
     * @param since the last search
     * @return list of item found
     * @throws HarvesterException
     */
    public List<FedoraItem> getItems(Date since) throws HarvesterException {
        if (!started) {
            started = true;
            cacheResults(since);
        }
        return null;
        //return getCachedItems();
    }

    /**
     * Cache search result
     * 
     * @param since the last search
     * @throws HarvesterException
     */
    private void cacheResults(Date since) throws HarvesterException {
        log.info("Caching search results in " + workDir.getAbsolutePath());
        try {
            Marshaller m = jc.createMarshaller();
            boolean done = false;
            boolean first = true;
            int count = 0;
            ResultType results;
            String token = null;
            while (!done) {
                if (first) {
                    first = false;
                    if (since == null) {
                        log.info("Search for ALL records");
                        results = restClient.findObjects(FindObjectsType.TERMS,
                                searchTerms, requestSize);
                    } else {
                        DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
                        String from = df.format(since);
                        log.info("Search for records from [" + from + "]");
                        results = restClient.findObjects(FindObjectsType.QUERY,
                                "mDate>" + from, requestSize);
                    }
                } else {
                    results = restClient.resumeFindObjects(token);
                    log.info("Resuming search using token: " + token);
                }

                File tmpFile = new File(workDir, count + ".xml");
                m.marshal(results, tmpFile);
                searchFiles.add(tmpFile);
                
                ListSessionType session = results.getListSession();
                if (session != null) {
                    token = results.getListSession().getToken();
                } else {
                    token = null;
                }
                Thread.sleep(2000);
                ++count;
                done = token == null || (maxRequests != -1 &&  count> maxRequests);
            }
            log.info("Cached " + (searchFiles.size() - 1) + " search results");
        } catch (Exception e) {
            throw new HarvesterException(e);
        }
    }
}
