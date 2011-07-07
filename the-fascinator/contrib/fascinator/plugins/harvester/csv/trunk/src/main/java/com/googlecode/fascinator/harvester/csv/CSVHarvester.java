/*
 * The Fascinator - Plugin - Harvester - CSV
 * Copyright (C) 2010-2011 University of Southern Queensland
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
package com.googlecode.fascinator.harvester.csv;

import au.com.bytecode.opencsv.CSVReader;

import com.googlecode.fascinator.api.harvester.HarvesterException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.harvester.impl.GenericHarvester;
import com.googlecode.fascinator.common.storage.StorageUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Harvester for CSV files.
 * <p>
 * Configuration options:
 * <ul>
 * <li>fileLocation: The location of the csv file (required)</li>
 * <li>idColumn: the column holding the primary key. 
 * 	If not provided, the row number will be used.</li>
 * <li>recordIDPrefix: Adds a prefix to the value found in the ID column.
 *	For example, setting this as "http://id.example.com/" with an ID value of "453"
 *	will result in http://id.example.com/453 as the ID. 
 * <li>delimiter: The csv delimiter. Comma (,) is the default</li>
 * <li>ignoredFields: An array of fields (columns) ignored by the harvest.</li>
 * <li>includedFields: An array of fields (columns) included by the harvest</li>
 * <li>payloadId: The payload identifier used to store the JSON data (defaults to "metadata.json")</li>
 * <li>batchSize: The number of rows in the CSV file to process, before being harvested (defaults to 50)</li>
 * <li>maxRows: The number of rows to process where -1 means harvest all (defaults to -1)</li>
 * </ul>
 * <p>
 * You can select to harvest all columns (fields) or be selective:
 * <ul>
 * <li>If you don't set ignoredFields or includedFields: all fields are harvested.</li>
 * <li>ignoredFields has precedence over includedFields: 
 * 	If you have the same field in both lists, it will get ignored.</li>
 * <li>If you only provide fields in ignoredFields (and leave includedFields blank), all other fields will be harvested</li>
 * <li>If you only provide fields in includedFields (and leave ignoredFields blank), only these fields will be harvested</li>
 * </ul>
 * <p>
 * Fields with repeated names result in only the first value being stored.
 * <p>
 * Based on Greg Pendlebury's CallistaHarvester.
 * 
 * @author Duncan Dickinson
 */
public class CSVHarvester extends GenericHarvester {

    /** Default column delimiter */
    private static final char DEFAULT_DELIMITER = ',';

    /** Default payload ID */
    private static final String DEFAULT_PAYLOAD_ID = "metadata.json";

    /** Default batch size */
    private static final int DEFAULT_BATCH_SIZE = 50;

    /** Logging */
    private Logger log = LoggerFactory.getLogger(CSVHarvester.class);

    /** Field names (columns) */
    private List<String> dataFields;

    /** Ignored field names (column) */
    private List<String> ignoredFields;

    /** Included field names (column) */
    private List<String> includedFields;

    /** The name of the column holding the ID */
    private String idColumn;

    /** A prefix for generating the object's ID */
    private String idPrefix;

    /** Debugging limit */
    private long maxRows;

    /** Payload ID */
    private String payloadId;

    /** Batch size */
    private int batchSize;

    /** Current row */
    private long currentRow;

    /** Whether or not there are more files to harvest */
    private boolean hasMore;

    /** CSV Reader */
    private CSVReader csvReader;

    /** File name */
    private String filename;

    /**
     * Constructs the CSV harvester plugin.
     */
    public CSVHarvester() {
        super("csv", "CSV Harvester");
    }

    /**
     * Initialise the CSV harvester plugin.
     *
     * @throws HarvesterException if an error occurred
     */
    @Override
    public void init() throws HarvesterException {
        JsonSimple options = new JsonSimple(getJsonConfig().getObject("harvester", "csv"));

        String filePath = options.getString(null, "fileLocation");
        if (filePath == null) {
            throw new HarvesterException("No data file provided!");
        }
        File csvDataFile = new File(filePath);
        if (csvDataFile == null || !csvDataFile.exists()) {
            throw new HarvesterException("Could not find CSV file '" + filePath + "'");
        }
        filename = csvDataFile.getName();

        idPrefix = options.getString("", "recordIDPrefix");
        maxRows = options.getInteger(-1, "maxRows");
        ignoredFields = getStringList(options, "ignoreFields");
        includedFields = getStringList(options, "includedFields");
        payloadId = options.getString(DEFAULT_PAYLOAD_ID, "payloadId");
        batchSize = options.getInteger(DEFAULT_BATCH_SIZE, "batchSize");
        hasMore = true;

        try {
            // open the CSV file for reading
            Reader fileReader = new InputStreamReader(new FileInputStream(csvDataFile), "UTF-8");
            char delimiter = options.getString(String.valueOf(DEFAULT_DELIMITER), "delimiter").charAt(0);
            csvReader = new CSVReader(fileReader, delimiter);

            // configure the data fields
            if (options.getBoolean(true, "headerRow")) {
                dataFields = Arrays.asList(csvReader.readNext());
            } else {
                dataFields = getStringList(options, "headerList");
            }

            // check that the specified id column is valid
            idColumn = options.getString(null, "idColumn");
            if (idColumn != null && !dataFields.contains(idColumn)) {
                throw new HarvesterException("'" + idColumn + "' is invalid!");
            }
        } catch (IOException ioe) {
            throw new HarvesterException(ioe);
        }
    }

    /**
     * Gets a string list from a JsonSimple object. Convenience method to return
     * an empty list instead of null if the node was not found.
     *
     * @param json a JsonSimple object
     * @param path path to the node
     * @return string list found at node, or empty if not found
     */
    private List<String> getStringList(JsonSimple json, Object... path) {
        List<String> list = json.getStringList(path);
        if (list == null) {
            list = Collections.emptyList();
        }
        return list;
    }

    /**
     * Shutdown the plugin.
     * 
     * @throws HarvesterException if an error occurred
     */
    @Override
    public void shutdown() throws HarvesterException {
        if (csvReader != null) {
            try {
                csvReader.close();
            } catch (IOException ioe) {
                log.warn("Failed to close CSVReader!", ioe);
            }
            csvReader = null;
        }
    }

    /**
     * Check if there are more objects to harvest.
     *
     * @return <code>true</code> if there are more, <code>false</code> otherwise
     */
    @Override
    public boolean hasMoreObjects() {
        return hasMore;
    }

    /**
     * Harvest the next batch of rows and return their object IDs.
     *
     * @return the set of object IDs just harvested
     * @throws HarvesterException if an error occurred
     */
    @Override
    public Set<String> getObjectIdList() throws HarvesterException {
        Set<String> objectIdList = new HashSet<String>();
        try {
            String[] row = null;
            int rowCount = 0;
            boolean done = false;
            while (!done && (row = csvReader.readNext()) != null) {
                rowCount++;
                currentRow++;
                objectIdList.add(createRecord(row));
                if (rowCount % batchSize == 0) {
                    log.debug("Batch size reached at row {}", currentRow);
                    break;
                }
                done = (maxRows > 0) && (currentRow < maxRows);
            }
            hasMore = (row != null);
        } catch (IOException ioe) {
            throw new HarvesterException(ioe);
        }
        if (objectIdList.size() > 0) {
            log.debug("Created {} objects", objectIdList.size());
        }
        return objectIdList;
    }

    private String createRecord(String[] columns) throws HarvesterException {
        // by default use the row number as the ID
        String recordId = Long.toString(currentRow);

        // create data
        JsonObject data = new JsonObject();
        for (int index = 0; index < columns.length; index++) {
            String field = dataFields.get(index);
            String value = columns[index];
            // respect fields to be included and ignored
            if (includedFields.contains(field) && !ignoredFields.contains(field)) {
                data.put(field, value);
            }
            if (field.equals(idColumn)) {
                recordId = value;
            }
        }

        // create metadata
        JsonObject meta = new JsonObject();
        meta.put("dc.identifier", idPrefix + recordId);

        // store JSON to the object
        JsonObject json = new JsonObject();
        json.put("recordIDPrefix", idPrefix);
        json.put("data", data);
        json.put("metadata", meta);
        try {
            // create a new object
            String oid = DigestUtils.md5Hex(filename + idPrefix + recordId);
            DigitalObject object = StorageUtils.getDigitalObject(getStorage(), oid);

            // Make our JSON human readable
            JsonSimple jsonSimple = new JsonSimple(json);
            String jsonString = jsonSimple.toString(true);

            // add the JSON payload
            InputStream jsonStream = IOUtils.toInputStream(jsonString, "UTF-8");
            Payload payload = StorageUtils.createOrUpdatePayload(
                    object, payloadId, jsonStream);
            payload.setContentType("application/json");
            payload.close();

            // set the pending flag
            object.getMetadata().setProperty("render-pending", "true");
            object.close();

            return oid;
        } catch (Exception e) {
            throw new HarvesterException("Failed to store metadata", e);
        }
    }
}
