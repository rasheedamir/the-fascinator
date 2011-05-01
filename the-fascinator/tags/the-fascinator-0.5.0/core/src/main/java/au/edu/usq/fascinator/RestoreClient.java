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
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;

/**
 * To restore backup directories to the current storage and index it
 * <p>
 * Rely on configuration either set in:
 * </p>
 * <ul>
 * <li>system-config.json (Default) "email": "fascinator@usq.edu.au"</li>
 * <li>backup paths list consists of:
 * <ul>
 * <li>path: backup full path where need to be restored</li>
 * <li>active: to specify if the backup path is active</li>
 * <li>ignoreFilter: to specify directory/files to be ignored</li>
 * <li>include-portal-view: to backup the current portal view</li>
 * <li>storage:
 * <ul>
 * <li>type: storage type in which the backup directory rely on</li>
 * <li>and the storage information e.g. filesystem storage require home
 * directory path</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * 
 * 
 * TODO: In the future when there's option to restore only the original files,
 * conveyerbelt need to be included to run the rendition of the files
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

    /** Json system configuration file **/
    private JsonConfig systemConfig;

    /** Email used to define user space **/
    private String email = null;

    /** Backup location list from where the restore need to be performed **/
    private Map<String, JsonConfigHelper> backupDirList;

    /** Real storage Type **/
    private String realStorageType;

    /** Real Storage **/
    private Storage realStorage;

    /** configuration file **/
    private File jsonFile;

    /** Indexed storage **/
    private Storage storage;

    /** Indexer **/
    private Indexer indexer;

    /**
     * Backup Client Constructor
     * 
     * @throws IOException if configuration file not found
     * @throws PluginException if the plugin initialisation fail
     */
    public RestoreClient() throws IOException, PluginException {
        setDefaultSetting(null);
    }

    /**
     * Backup Client Constructor
     * 
     * @param jsonFile configuration file
     * @throws IOException if configuration file not found
     * @throws PluginException if the plugin initialisation fail
     */
    public RestoreClient(File jsonFile) throws IOException, PluginException {
        setDefaultSetting(jsonFile);
    }

    /**
     * Set the default setting
     * 
     * @param jsonFile configuration file
     * @throws IOException if configuration file not found
     * @throws PluginException if the plugin initialisation fail
     */
    public void setDefaultSetting(File jsonFile) throws IOException,
            PluginException {

        if (jsonFile != null) {
            config = new JsonConfig(jsonFile);
            this.jsonFile = jsonFile;
        } else {
            config = new JsonConfig();
            this.jsonFile = config.getSystemFile();
        }

        systemConfig = new JsonConfig(config.getSystemFile());
        indexer = PluginManager.getIndexer(config.get("indexer/type",
                DEFAULT_INDEXER_TYPE));
        realStorageType = systemConfig
                .get("storage/type", DEFAULT_STORAGE_TYPE);

        setEmail(config.get("email"));

        // Set backupDirList to be restored
        backupDirList = config.getJsonMap("restore/paths");
    }

    /**
     * Create the md5 of the email for the user space
     * 
     * TODO: Should be removed, can get the email from system-config.json
     * 
     * @param email of the user
     */
    public void setEmail(String email) {
        if (email != null && email != "") {
            this.email = DigestUtils.md5Hex(email);
        }
    }

    /**
     * Get the email
     * 
     * TODO: Should be removed, can get the email from system-config.json
     * 
     * @return email of the user
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
     * Run the restore code
     * 
     */
    public void run() {
        DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
        String now = df.format(new Date());
        long start = System.currentTimeMillis();
        log.info("Started at " + now);

        try {
            storage = PluginManager.getStorage(realStorageType);
            storage.init(config.getSystemFile());
            log.info("Loaded {} and {}", realStorage.getName(), indexer
                    .getName());
            indexer.init(jsonFile);
        } catch (Exception e) {
            log.error("Failed to initialise storage {}", e.getMessage());
            return;
        }

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

            String sourceStorageType = String.valueOf(backupProps
                    .get("storage/type"));
            if (sourceStorageType == null) {
                sourceStorageType = DEFAULT_STORAGE_TYPE;
            }

            String storageConfig = backupProps.toString();
            Storage sourceStorage = PluginManager.getStorage(sourceStorageType);
            try {
                sourceStorage.init(storageConfig);
            } catch (PluginException e1) {
                e1.printStackTrace();
            }
            // Only using active backup path
            // FIXME update to API
            /*
             * if (active && sourceStorage != null) { for (DigitalObject object
             * : sourceStorage.getObjectList()) { try { // Add the rules first:
             * Properties sofMetaProps = new Properties(); Payload
             * sofMetaPayload = object.getPayload("SOF-META");
             * sofMetaProps.load(sofMetaPayload.getInputStream());
             * 
             * String sofMetaRulesOid = sofMetaProps .getProperty("rulesOid");
             * File rulesFile = new File(sofMetaRulesOid);
             * 
             * try { log.debug("Caching rules file " + rulesFile); DigitalObject
             * rulesObject = new RulesDigitalObject( rulesFile);
             * realStorage.addObject(rulesObject); } catch (StorageException se)
             * { log.error( "Failed to cache indexing rules, stopping", se);
             * return; } storage.addObject(object);
             * 
             * } catch (StorageException e) { e.printStackTrace(); } catch
             * (IOException e) { e.printStackTrace(); } } }
             */
            if (includePortal) {
                File portalDir = new File(systemConfig.get("fascinator-home")
                        + "/portal/" + systemConfig.get("portal/home"));
                File restorePortalFolder = new File(backupPath + File.separator
                        + email, "config");
                if (restorePortalFolder.exists()) {
                    try {
                        includePortalDir(restorePortalFolder, portalDir,
                                ignoreFilter);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
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
            log.info("Usage: restore <json-config>");
        } else {
            File jsonFile = new File(args[0]);
            try {
                log.info("jsonFile: " + jsonFile.getAbsolutePath());
                RestoreClient restore;
                try {
                    restore = new RestoreClient(jsonFile);
                    restore.run();
                } catch (PluginException e) {
                    log.error("Failed to run Restore client: {}", e
                            .getMessage());
                }
            } catch (IOException ioe) {
                log.error("Failed to initialise client: {}", ioe.getMessage());
            }
        }
    }
}
