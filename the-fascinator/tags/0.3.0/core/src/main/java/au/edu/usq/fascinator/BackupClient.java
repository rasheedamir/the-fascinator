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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.indexer.SearchRequest;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;

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
 * <li>include-portal-view: to backup the current portal view</li>
 * <li>storage:
 * <ul>
 * <li>type: storage type to backup to</li>
 * <li>and the storage information e.g. filesystem storage require home
 * directory path</li>
 * </ul>
 * </li>
 * <ul></li>
 * </ul>
 * 
 * @author Linda octalina
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

    /** Default ignore filter if none defined **/
    private static final String DEFAULT_IGNORE_FILTER = ".svn|.ice|.*|~*|*~";

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

    private Storage realStorage;

    /** Indexer **/
    private String indexerType;

    /** Portal query **/
    private String portalQuery = null;

    /** Portal directory **/
    private File portalDir;

    /** Backup all **/
    private Boolean backupAll = false;

    /**
     * Backup Client Constructor
     * 
     * @throws IOException
     * 
     * @throws IOException
     */
    public BackupClient() throws IOException {
        setDefaultSetting(null);
    }

    /**
     * Backup Client Constructor
     * 
     * @param jsonFile
     * @throws IOException
     * @throws IOException
     */
    public BackupClient(File jsonFile) throws IOException {
        setDefaultSetting(jsonFile);
        backupAll = true;
    }

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
     * @param email
     * @param backupDir
     * @param portalQuery
     * @throws IOException
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
     * @param email
     */
    public void setEmail(String email) {
        if (email != null && email != "") {
            this.email = DigestUtils.md5Hex(email);
        }
    }

    /**
     * Get the email
     * 
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set Backup location being used
     * 
     * @param backupDir
     */
    public void setBackupDir(Map<String, JsonConfigHelper> backupDirList) {
        this.backupDirList = backupDirList;
    }

    /**
     * Return backup location
     * 
     * @return backupDir
     */
    public Map<String, JsonConfigHelper> getBackupDir() {
        return backupDirList;
    }

    /**
     * Set the portal Query
     * 
     * @param portalQuery
     */
    public void setPortalQuery(String portalQuery) {
        if (portalQuery != null && portalQuery != "") {
            this.portalQuery = portalQuery;
        }
    }

    /**
     * Get the portal Query
     * 
     * @return portalQuery
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
        log.info("Started at " + now);

        Indexer indexer;
        try {
            realStorage = PluginManager.getStorage(realStorageType);
            indexer = PluginManager.getIndexer(indexerType);
            realStorage.init(config.getSystemFile());
            indexer.init(config.getSystemFile());
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

        log.info("Completed in "
                + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
    }

    /**
     * Start backup files from from the result returned by solr
     * 
     * @param js
     * @throws IOException
     */
    public void startBackup(JsonConfigHelper js) throws IOException {
        // Backup to active backup Directory
        log.debug("backupDir: " + backupDirList.toString());
        for (String backupName : backupDirList.keySet()) {
            // Map<String, Object> backupProps = backupDirList.get(backupPath);
            JsonConfigHelper backupProps = backupDirList.get(backupName);

            String backupPath = String.valueOf(backupProps.get("path"));
            String filterString = String.valueOf(backupProps
                    .get("ignoreFilter"));

            if (filterString == null) {
                filterString = DEFAULT_IGNORE_FILTER;
            }

            IgnoreFilter ignoreFilter = new IgnoreFilter(filterString
                    .split("\\|"));
            // boolean includeMeta = Boolean.parseBoolean(backupProps.get(
            // "include-rendition-meta").toString());
            boolean active = Boolean.parseBoolean(backupProps.get("active")
                    .toString());
            boolean includePortal = Boolean.parseBoolean(backupProps
                    .get("include-portal-view"));

            String destinationStorageType = String.valueOf(backupProps
                    .get("storage/type"));
            if (destinationStorageType == null) {
                destinationStorageType = DEFAULT_STORAGE_TYPE;
            }

            String storageConfig = backupProps.toString();
            Storage destinationStorage = PluginManager
                    .getStorage(destinationStorageType);
            try {
                log.info("backupProps: " + backupProps.toString());
                destinationStorage.init(storageConfig);
            } catch (PluginException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            // Only using active backup path
            if (active && destinationStorage != null) {
                try {

                    // List all the files to be backup-ed
                    // TODO: should the rules be backuped as well?
                    for (Object oid : js.getList("response/docs/id")) {
                        String objectId = oid.toString();
                        DigitalObject digitalObject = realStorage
                                .getObject(objectId);
                        log
                                .info("Backing up object: {}", digitalObject
                                        .getId());
                        // Original File
                        destinationStorage.addObject(digitalObject);

                        // List all the payloads
                        // NOTE: currently if the above object added, the
                        // payload will be added automatically
                        // List<Payload> payloadList = digitalObject
                        // .getPayloadList();
                        // if (includeMeta && payloadList.isEmpty() == false) {
                        // for (Payload payload : payloadList) {
                        // log.info("Backing up payload: {}", payload
                        // .getId());
                        // destinationStorage
                        // .addPayload(objectId, payload);
                        // }
                        // }

                    }

                } catch (PluginException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                // backup all the portal
                if (backupAll == true && includePortal) {
                    File portalFolder;
                    portalDir = new File(systemConfig.get("fascinator-home")
                            + "/portal/" + systemConfig.get("portal/home"));
                    portalFolder = new File(backupPath.toString()
                            + File.separator + email + File.separator
                            + "config");
                    portalFolder.getParentFile().mkdirs();
                    includePortalDir(portalDir, portalFolder, ignoreFilter);
                }

                // backup only current portal
                if (includePortal && backupAll == false) {
                    File portalFolder;
                    if (portalDir == null) {
                        portalDir = new File(systemConfig
                                .get("fascinator-home")
                                + "/portal/" + systemConfig.get("portal/home"));
                        portalFolder = new File(backupPath.toString()
                                + File.separator + email + File.separator
                                + "config");
                    } else {
                        portalFolder = new File(backupPath.toString()
                                + File.separator + email + File.separator
                                + "config" + File.separator
                                + portalDir.getName());
                    }
                    portalFolder.getParentFile().mkdirs();
                    includePortalDir(portalDir, portalFolder, ignoreFilter);
                }
            }
        }
    }

    /**
     * Copy portal config directory
     * 
     * @param portalSrc
     * @param portalDest
     * @param ignoreFilter to filter out .svn directory
     * @throws IOException
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
     * Change the name if it's a window file
     * 
     * @param fileName
     * @return
     */
    // private String isWindowFile(String fileName) {
    // // For window C: will cause error. so fix it as C_
    // // NOTE: remember to change it back when doing restore
    // if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1)
    // {
    // fileName = fileName.replace("C:", File.separator + "C_");
    // }
    // return fileName;
    // }

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

    public static void main(String[] args) {
        if (args.length < 1) {
            log.info("Usage: backup <json-config>");
        } else {
            File jsonFile = new File(args[0]);
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