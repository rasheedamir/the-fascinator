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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
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
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.indexer.SearchRequest;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.storage.StorageUtils;

/**
 * To backup the DigitalObject indexed in Solr
 * <p>
 * Rely on configuration either set in:
 * </p>
 * <ul>
 * <li>system-config.json (Default) "email": "fascinator@usq.edu.au"</li>
 * <li>backup paths list consists of:
 * <ul>
 * <li>path: backup destination full path</li>
 * <li>active: to specify if the backup path is active</li>
 * <li>ignoreFilter: to specify directory/files to be ignored</li>
 * <li>include-portal-query: to backup the current portal view</li>
 * <li>storage:
 * <ul>
 * <li>type: storage type to backup to</li>
 * <li>and the storage information e.g. filesystem storage require home
 * directory path</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @author Linda Octalina
 * 
 */

public class BackupClient {

    /** Date format **/
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /** DateTime format **/
    public static final String DATETIME_FORMAT = DATE_FORMAT + "'T'hh:mm:ss'Z'";

    /** Default storage type will be used if none defined **/
    private static final String DEFAULT_STORAGE_TYPE = "file-system";

    /** Default indexer type if none defined **/
    private static final String DEFAULT_INDEXER_TYPE = "solr";

    private static Logger log = LoggerFactory.getLogger(BackupClient.class);

    /** Json configuration file **/
    private JsonConfig config;

    /** Json system configuration file **/
    private JsonConfig systemConfig;

    /** Email used to define user space **/
    private String email = null;

    /** Backup location list **/
    private Map<String, JsonConfigHelper> backupDirList;

    /** Storage **/
    private String realStorageType;

    /** Storage **/
    private Storage realStorage;

    /** Indexer **/
    private String indexerType;

    /** Portal query **/
    private String portalQuery = null;

    /** Portal directory **/
    private File portalDir;

    /** Backup all **/
    private Boolean backupAll = false;

    /** Indexer **/
    private Indexer indexer;

    /**
     * Backup Client Constructor
     * 
     * @throws IOException If initialisation fail
     */
    public BackupClient() throws IOException {
        setDefaultSetting(null);
    }

    /**
     * Backup Client Constructor
     * 
     * @param jsonFile Configuration file
     * @throws IOException If initialisation fail
     */
    public BackupClient(File jsonFile) throws IOException {
        setDefaultSetting(jsonFile);
        backupAll = true;
    }

    /**
     * Initialising default setting from specified configuration file
     * 
     * @param jsonFile Configuration file
     * @throws IOException If initialisation fail
     */
    public void setDefaultSetting(File jsonFile) throws IOException {
        Boolean fromPortal = true;
        if (jsonFile != null) {
            fromPortal = false;
            config = new JsonConfig(jsonFile);
        } else {
            config = new JsonConfig();
        }

        systemConfig = new JsonConfig(config.getSystemFile());
        indexerType = systemConfig.get("indexer/type", DEFAULT_INDEXER_TYPE);
        realStorageType = systemConfig
                .get("storage/type", DEFAULT_STORAGE_TYPE);
        setEmail(config.get("email"));
        if (fromPortal == false) {
            // Set default backupDirList
            backupDirList = config.getJsonMap("backup/paths");

        }
    }

    /**
     * Backup Client Constructor
     * 
     * @param email Email address of the user
     * @param backupDir Backup Directory List
     * @param portalQuery Query of the portal
     * @throws IOException If initialisation fail
     */
    public BackupClient(File portalDir,
            Map<String, JsonConfigHelper> backupDirs, String portalQuery)
            throws IOException {
        this.portalDir = portalDir;
        setDefaultSetting(null);
        backupDirList = backupDirs;
        setPortalQuery(portalQuery);
    }

    /**
     * Create the md5 of the email for the user space
     * 
     * @param email Email address of the user
     */
    public void setEmail(String email) {
        if (email != null && email != "") {
            this.email = DigestUtils.md5Hex(email);
        }
    }

    /**
     * Get the email
     * 
     * @return email Email address of the user
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set Backup location being used
     * 
     * @param backupDir Backup Directory list
     */
    public void setBackupDir(Map<String, JsonConfigHelper> backupDirList) {
        this.backupDirList = backupDirList;
    }

    /**
     * Return backup location
     * 
     * @return backupDir Backup Directory list
     */
    public Map<String, JsonConfigHelper> getBackupDir() {
        return backupDirList;
    }

    /**
     * Set the portal Query
     * 
     * @param portalQuery Query for the portal
     */
    public void setPortalQuery(String portalQuery) {
        if (portalQuery != null && portalQuery != "") {
            this.portalQuery = portalQuery;
        }
    }

    /**
     * Get the portal Query
     * 
     * @return portalQuery Query of the portal
     */
    public String getPortalQuery() {
        return portalQuery;
    }

    /**
     * Run the backup code
     * 
     */
    public void run() {
        DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
        String now = df.format(new Date());
        long start = System.currentTimeMillis();
        log.info("Backup Started at " + now);

        try {
            File configFile = JsonConfig.getSystemFile();
            realStorage = PluginManager.getStorage(realStorageType);
            realStorage.init(configFile);
            indexer = PluginManager.getIndexer(indexerType);
            indexer.init(configFile);
            log.info("Loaded {} and {}", realStorage.getName(), indexer
                    .getName());
        } catch (Exception e) {
            log.error("Failed to initialise storage", e);
            return;
        }

        // Get all the records from solr
        int startRow = 0;
        int numPerPage = 5;
        int numFound = 0;
        do {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            SearchRequest request = new SearchRequest("*:*");
            request.addParam("rows", String.valueOf(numPerPage));
            request.addParam("fq", "item_type:\"object\"");
            request.setParam("start", String.valueOf(startRow));

            // Check if the portal has it's own query
            if (portalQuery != "" && portalQuery != null) {
                request.addParam("fq", portalQuery);
            }

            try {
                indexer.search(request, result);
                JsonConfigHelper js;
                js = new JsonConfigHelper(result.toString());
                startBackup(js);

                startRow += numPerPage;
                numFound = Integer.parseInt(js.get("response/numFound"));

            } catch (IndexerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (startRow < numFound);

        log.info("Backup Completed in "
                + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
    }

    /**
     * Start backup files from from the result returned by solr
     * 
     * @param js JSON Configuration
     * @throws IOException
     * @throws IOException
     */
    public void startBackup(JsonConfigHelper js) throws IOException {
        // Backup to active backup Directory
        log.debug("backupDir: " + backupDirList.toString());
        for (String backupName : backupDirList.keySet()) {
            // Map<String, Object> backupProps = backupDirList.get(backupPath);
            JsonConfigHelper backupProps = backupDirList.get(backupName);

            String backupPath = String.valueOf(backupProps.get("path"));

            boolean active = Boolean.parseBoolean(backupProps.get("active")
                    .toString());
            boolean includeQuery = Boolean.parseBoolean(backupProps
                    .get("include-portal-query"));

            String destinationStorageType = String.valueOf(backupProps
                    .get("storage/type"));
            if (destinationStorageType == null) {
                destinationStorageType = DEFAULT_STORAGE_TYPE;
            }

            String storageConfig = backupProps.toString();
            Storage destinationStorage = PluginManager
                    .getStorage(destinationStorageType);
            try {
                log.debug("backupProps: " + backupProps.toString());
                destinationStorage.init(storageConfig);
            } catch (PluginException e1) {
                log.error("Fail to initalise plugin");
                e1.printStackTrace();
            }

            // Only using active backup path
            if (active && destinationStorage != null) {
                try {
                    log.info("Backup destionation folder: '{}'", backupPath);
                    Set<String> jsonConfigOidList = new HashSet<String>();
                    Set<String> ruleOidList = new HashSet<String>();
                    // List all the files to be backup-ed
                    for (JsonConfigHelper docConfig : js
                            .getJsonList("response/docs")) {
                        String objectId = docConfig.get("id");

                        // Retrieve object from source
                        DigitalObject digitalObject = realStorage
                                .getObject(objectId);

                        String originalFilePath = digitalObject.getMetadata()
                                .getProperty("file.path");
                        log.info("Backing up '{}'", originalFilePath);
                        File originalFile = new File(originalFilePath);
                        // Backup Original File instead of using link
                        DigitalObject newObject = StorageUtils.storeFile(
                                destinationStorage, originalFile, false);

                        // Backup all the payloads
                        Set<String> payloadIdList = digitalObject
                                .getPayloadIdList();
                        for (String payloadId : payloadIdList) {
                            Payload payload = digitalObject
                                    .getPayload(payloadId);

                            // Check if payload already exist
                            if (!newObject.getPayloadIdList().contains(
                                    payloadId)) {
                                Payload newPayload = newObject
                                        .createStoredPayload(payloadId, payload
                                                .open());
                                // Set the appropriate Payload Type
                                newPayload.setType(payload.getType());
                                newPayload.close();
                            }
                        }

                        // Backup config file
                        String jsonConfigOid = docConfig.get("harvest_config");
                        if (!jsonConfigOidList.contains(jsonConfigOid)) {

                            DigitalObject jsonConfigObject = realStorage
                                    .getObject(jsonConfigOid);
                            Payload jsonConfigPayload = jsonConfigObject
                                    .getPayload(jsonConfigObject.getSourceId());

                            DigitalObject newJsonConfigObject = StorageUtils
                                    .getDigitalObject(destinationStorage,
                                            jsonConfigOid);
                            StorageUtils.createOrUpdatePayload(
                                    newJsonConfigObject, jsonConfigPayload
                                            .getId(), jsonConfigPayload.open());
                            jsonConfigOidList.add(jsonConfigOid);
                        }

                        // Backup rule files
                        String ruleFileOid = docConfig.get("harvest_rules");
                        if (!ruleOidList.contains(ruleFileOid)) {
                            DigitalObject ruleObject = realStorage
                                    .getObject(ruleFileOid);
                            Payload rulePayload = ruleObject
                                    .getPayload(ruleObject.getSourceId());

                            DigitalObject newRuleObject = StorageUtils
                                    .getDigitalObject(destinationStorage,
                                            ruleFileOid);
                            StorageUtils.createOrUpdatePayload(newRuleObject,
                                    rulePayload.getId(), rulePayload.open());
                            ruleOidList.add(ruleFileOid);
                        }
                        newObject.close();
                    }
                    createRestoreConfig(backupProps.getMap("storage"),
                            backupPath, includeQuery);
                } catch (PluginException e) {
                    log.error("Fail to load plugin");
                    e.printStackTrace();
                }

                // backup all the portal if in "default" portal
                if (portalDir.getName().equals("default")) {
                    backupAll = true;
                }
                if (includeQuery) {
                    backupQuery(backupPath);
                }
            }
        }
    }

    /**
     * Create restore configuration file
     * 
     * @param storage storage configuration
     * @param backupPath full backup path
     * @param includeQuery status to include query
     * @throws IOException if can't be created
     */
    private void createRestoreConfig(Map<String, Object> storage,
            String backupPath, Boolean includeQuery) throws IOException {
        // Creating restore config
        JsonConfigHelper restoreConfig = new JsonConfigHelper();
        restoreConfig.set("email", config.get("email"));
        restoreConfig.setMap("storage", storage);
        restoreConfig.set("include-portal-query", Boolean
                .toString(includeQuery));
        FileWriter restoreConfigFile = new FileWriter(backupPath
                + "/restore.json");
        restoreConfig.store(restoreConfigFile, true);
        restoreConfigFile.close();
    }

    /**
     * Copy backup query
     * 
     * @param backupPath Full backup path
     * @throws IOException if file not exist
     */
    private void backupQuery(String backupPath) throws IOException {
        String home = config.get("portal/home",
                "/opt/the-fascinator/config/portal");
        File localPortalDir = new File(home);
        if (!localPortalDir.exists()) {
            home = "portal/src/main/config/portal";
            localPortalDir = new File("../", home);
        }

        if (backupAll == true) {
            log.info("Backing up all the portal view");
            log.info("file: {}", localPortalDir.listFiles().length);
            for (File file : localPortalDir.listFiles()) {
                log.info("file: {}", file.getAbsolutePath());
                File portalJsonFile = new File(file.getAbsolutePath(),
                        "portal.json");
                if (portalJsonFile.exists()) {
                    File destinationFile = new File(FilenameUtils
                            .separatorsToSystem(backupPath.toString()
                                    + File.separator + email
                                    + "/config/portal/" + file.getName()
                                    + "/portal.json"));
                    copyFile(portalJsonFile, destinationFile);
                }
            }
        } else {
            // Just copy portal.json of current view
            log.info("Backing up {} view", portalDir.getName());
            File sourceFile = new File(localPortalDir.getAbsolutePath(),
                    portalDir.getName() + "/portal.json");
            File destinationFile = new File(FilenameUtils
                    .separatorsToSystem(backupPath.toString() + File.separator
                            + email + "/config/portal/" + portalDir.getName()
                            + "/portal.json"));
            copyFile(sourceFile, destinationFile);
        }
    }

    /**
     * Copying file
     * 
     * @param sourceFile Source file to be copied
     * @param destinationFile destination file
     * @throws IOException
     */
    private void copyFile(File sourceFile, File destinationFile)
            throws IOException {
        destinationFile.getParentFile().mkdirs();
        InputStream in = new FileInputStream(sourceFile);
        OutputStream out = new FileOutputStream(destinationFile);
        IOUtils.copy(in, out);
    }

    /**
     * Main method for Backup client
     * 
     * @param args list of arguments
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            log.info("Usage: backup <json-config>");
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

            File jsonFile = new File(filePath);
            try {
                log.info("jsonFile: " + jsonFile.getAbsolutePath());
                BackupClient backup = new BackupClient(jsonFile);
                backup.run();
            } catch (IOException ioe) {
                log.error("Failed to initialise client: {}", ioe.getMessage());
            }
        }
    }
}
