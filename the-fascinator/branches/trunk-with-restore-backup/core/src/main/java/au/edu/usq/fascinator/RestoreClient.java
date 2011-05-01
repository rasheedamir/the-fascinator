/* 
 * The Fascinator - Core
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

package au.edu.usq.fascinator;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.harvester.HarvesterException;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.storage.StorageUtils;

/**
 * To restore backup directories to the current storage from backup directory
 * and index it
 * 
 * NOTE: for now only work for restoring from filesystem storage
 * 
 * @author Linda octalina
 * 
 */

public class RestoreClient {

    /** Date format **/
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /** DateTime format **/
    public static final String DATETIME_FORMAT = DATE_FORMAT + "'T'hh:mm:ss'Z'";

    /** Default storage type will be used if none defined **/
    private static final String DEFAULT_STORAGE_TYPE = "file-system";

    /** Default indexer type if none defined **/
    private static final String DEFAULT_INDEXER_TYPE = "solr";

    /** Default ignore filter if none defined **/
    private static final String DEFAULT_IGNORE_FILTER = ".svn|.ice|.*|~*|*~";

    /** Logging */
    private static Logger log = LoggerFactory.getLogger(RestoreClient.class);

    /** Json configuration file **/
    private JsonConfig config;

    /** Email used to define user space **/
    private String email = null;

    /** Restore configuration file **/
    private File restoreJson;

    /** Restore Config Helper file **/
    private JsonConfigHelper restoreJsonConfig;

    /** Indexed storage **/
    private Storage storage;

    /** Indexer **/
    private Indexer indexer;

    /** Storage type where the file will be restored are stored **/
    String sourceStorageType;

    /** Storage where the files will be restored are stored **/
    Storage sourceStorage;

    /** Path where the files will be restored **/
    private String pathToBeRestored;

    /**
     * Backup Client Constructor
     * 
     * @throws IOException if configuration file not found
     * @throws PluginException if the plugin initialisation fail
     */
    public RestoreClient() throws IOException, PluginException {
        setDefaultSetting();
    }

    /**
     * Backup Client Constructor
     * 
     * @param pathToBeRestored
     * @throws PluginException
     * @throws IOException
     */
    public RestoreClient(String pathToBeRestored) throws IOException,
            PluginException {
        this.pathToBeRestored = pathToBeRestored;
        setDefaultSetting();
    }

    /**
     * Set the default setting
     * 
     * @param jsonFile configuration file
     * @throws IOException if configuration file not found
     * @throws PluginException if the plugin initialisation fail
     */
    public void setDefaultSetting() throws IOException, PluginException {
        File configFile = JsonConfig.getSystemFile();
        config = new JsonConfig(configFile);

        restoreJson = new File(pathToBeRestored, "restore.json");
        if (!restoreJson.exists()) {
            log
                    .info("Could not locate the restore configuration file, could not perform restore");
            return;
        }

        // If email address in restore configuration is not the same as current
        // storage email address change the email address of current storage
        restoreJsonConfig = new JsonConfigHelper(restoreJson);
        email = restoreJsonConfig.get("email");
        String systemEmail = config.get("email");

        String configStr = config.toString();
        JsonConfigHelper newConfig = new JsonConfigHelper();
        if (email != systemEmail) {
            newConfig.set("email", email);
            newConfig.setMap("storage", config.getMap("storage"));
            configStr = newConfig.toString();
        }

        indexer = PluginManager.getIndexer(config.get("indexer/type",
                DEFAULT_INDEXER_TYPE));
        String realStorageType = config.get("storage/type",
                DEFAULT_STORAGE_TYPE);

        // For now assume the source to be restored is a filesystem storage type
        if (sourceStorageType == null) {
            sourceStorageType = DEFAULT_STORAGE_TYPE;
        }
        // Set up restored-from storage
        sourceStorage = PluginManager.getStorage(sourceStorageType);
        try {
            sourceStorage.init(restoreJson);
        } catch (PluginException e1) {
            e1.printStackTrace();
        }

        if (sourceStorage.getObjectIdList().size() > 0) {
            // Set up the current system storage
            storage = PluginManager.getStorage(realStorageType);
            if (storage == null) {
                log.info("Fail to load storage plugin");
                return;
            }
            try {
                storage.init(configStr);
                log.info("Loaded {}", storage.getName());
            } catch (PluginException pe) {
                log.info("Failed to initialise storage");
                return;
            }

            try {
                indexer.init(configStr);
            } catch (PluginException e) {
                log.error("Failed to initialise indexer {}", e.getMessage());
                return;
            }
        } else {
            log
                    .info("No objects have been restored. Check if the email in restore.json "
                            + "(from the restore path) is the same as the email used during backup");
        }
    }

    /**
     * Set Backup location being used
     * 
     * @param backupDir Backup Directory list
     */
    public void setBackupDir(Map<String, JsonConfigHelper> backupDirList) {
        // this.backupDirList = backupDirList;
    }

    /**
     * Run the restore code
     * 
     * @throws HarvesterException
     * @throws IOException if the config file not found
     * 
     */
    public void run() throws HarvesterException, IOException {
        DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
        String now = df.format(new Date());
        long start = System.currentTimeMillis();
        log.info("Started at " + now);

        startRestore();

        log.info("Completed in "
                + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
    }

    /**
     * Start restore the objects from backup directory to the storage
     * 
     * TODO: fix the restore
     */
    public void startRestore() {
        for (String id : sourceStorage.getObjectIdList()) {
            Set<String> jsonConfigOidList = new HashSet<String>();
            Set<String> ruleOidList = new HashSet<String>();
            log.info("Restoring: {}", id);
            try {
                // Retrieving object from source
                DigitalObject sourceObject = sourceStorage.getObject(id);

                DigitalObject destinationObject = StorageUtils
                        .getDigitalObject(storage, id);

                // Create the source paylaod first
                String sourceId = sourceObject.getSourceId();
                Payload sourcePayload = sourceObject.getPayload(sourceId);
                StorageUtils.createOrUpdatePayload(destinationObject,
                        sourcePayload.getId(), sourcePayload.open());

                // Creating payloads in the destination
                for (String payloadId : sourceObject.getPayloadIdList()) {
                    if (!payloadId.equals(sourceId)) {
                        Payload payload = sourceObject.getPayload(payloadId);
                        Payload newPayload = StorageUtils
                                .createOrUpdatePayload(destinationObject,
                                        payload.getId(), payload.open());
                        newPayload.setType(payload.getType());
                    }
                }

                // Set the file.path to nothing
                destinationObject.getMetadata().setProperty("file.path",
                        sourceId);

                // Restore config file
                String jsonConfigOid = sourceObject.getMetadata().getProperty(
                        "jsonConfigOid");
                if (!jsonConfigOidList.contains(jsonConfigOid)) {
                    DigitalObject jsonConfigObject = sourceStorage
                            .getObject(jsonConfigOid);
                    if (jsonConfigObject != null) {
                        Payload jsonConfigPayload = jsonConfigObject
                                .getPayload(jsonConfigObject.getSourceId());

                        // Creating new Object and payload in the destination
                        DigitalObject newJsonConfigObject = StorageUtils
                                .getDigitalObject(storage, jsonConfigOid);
                        StorageUtils.createOrUpdatePayload(newJsonConfigObject,
                                jsonConfigPayload.getId(), jsonConfigPayload
                                        .open());
                    }
                    jsonConfigOidList.add(jsonConfigOid);
                }

                // Restore rules file
                String ruleFileOid = sourceObject.getMetadata().getProperty(
                        "rulesOid");
                if (!ruleOidList.contains(ruleFileOid)) {
                    DigitalObject ruleObject = sourceStorage
                            .getObject(ruleFileOid);
                    if (ruleObject != null) {
                        Payload rulePayload = ruleObject.getPayload(ruleObject
                                .getSourceId());

                        // Creating new Object and payload in the destination
                        DigitalObject newRuleObject = StorageUtils
                                .getDigitalObject(storage, ruleFileOid);
                        StorageUtils.createOrUpdatePayload(newRuleObject,
                                rulePayload.getId(), rulePayload.open());
                    }
                    ruleOidList.add(jsonConfigOid);
                }

                // Indexing...
                try {
                    log.info("indexing: {}", id);
                    indexer.index(id);
                } catch (IndexerException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                destinationObject.close();
            } catch (StorageException e) {
                log.info("Object {} not found", id);
                e.printStackTrace();
            }
        }

        Boolean includeQuery = Boolean.parseBoolean(restoreJsonConfig
                .get("include-portal-query"));
        if (includeQuery) {
            restorePortalDir();
        }
    }

    private void restorePortalDir() {
        // Restore portal directory
        String emailMd5 = DigestUtils.md5Hex(config.get("email"));
        File portalDir = new File(pathToBeRestored, emailMd5 + "/config/portal");

        // If use installer, the portal/home value will be set
        if (portalDir.exists()) {
            String home = config.get("portal/home",
                    "/opt/the-fascinator/config");
            File homePath = new File(home);
            if (!homePath.exists()) {
                home = "portal/src/main/config";
                homePath = new File("../", home);
            }
            /** Default ignore filter if none defined **/

            IgnoreFilter ignore = new IgnoreFilter(".svn|.ice|.*|~*|*~"
                    .split("\\|"));
            try {
                log.info("Restoring portal view: {} to {}", portalDir
                        .getAbsolutePath(), homePath.getAbsolutePath());
                includePortalDir(portalDir, homePath, ignore);
            } catch (IOException e) {
                log.info("Error Restore file, file not found");
                e.printStackTrace();
            }
        }

    }

    /**
     * Copy portal config directory
     * 
     * @param portalSrc Portal config source directory
     * @param portalDest Portal config destination directory
     * @param ignoreFilter to filter out .svn directory
     * @throws IOException If portal source/destination directory not exist
     */
    private void includePortalDir(File portalSrc, File portalDest,
            IgnoreFilter ignoreFilter) throws IOException {
        if (portalSrc.isDirectory()) {
            if (!portalDest.exists()) {
                portalDest.mkdir();
            }
            for (File file : portalSrc.listFiles(ignoreFilter)) {
                includePortalDir(new File(portalSrc, file.getName()), new File(
                        portalDest, file.getName()), ignoreFilter);
            }
        } else {
            InputStream in = new FileInputStream(portalSrc);
            OutputStream out = new FileOutputStream(portalDest);
            IOUtils.copy(in, out);
            in.close();
            out.close();
        }
    }

    /**
     * File filter used to ignore specified files
     */
    private class IgnoreFilter implements FileFilter {

        /** wildcard patterns of files to ignore */
        private String[] patterns;

        public IgnoreFilter(String[] patterns) {
            this.patterns = patterns;
        }

        public boolean accept(File path) {
            for (String pattern : patterns) {
                if (FilenameUtils.wildcardMatch(path.getName(), pattern)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Main class for Backup Client
     * 
     * @param args Argument list
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            log.info("Usage: restore <path-to-be-restored>");
        } else {
            // TODO - http://jira.codehaus.org/browse/MEXEC-37
            // Because of the bug in maven exec spaces in the
            // path will result in incorrect arguements.
            String filePath;
            if (args.length > 1) {
                filePath = StringUtils.join(args, " ");
            } else {
                filePath = args[0];
            }
            RestoreClient restore;
            try {
                restore = new RestoreClient(filePath);
                restore.run();
            } catch (IOException ioe) {
                log.error("Failed to initialise client: {}", ioe.getMessage());
            } catch (PluginException e) {
                log.error("Failed to run Restore client: {}", e.getMessage());
            }

        }

    }
}