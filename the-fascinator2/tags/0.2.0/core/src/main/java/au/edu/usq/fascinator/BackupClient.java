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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.indexer.SearchRequest;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;

/**
 * To backup the DigitalObject indexed in Solr
 * <p>
 * Rely on configuration either set in:
 * </p>
 * <ul>
 * <li>system-config.json (Default) "email": "email@usq.edu.au", "backupDir":
 * "${user.home}/.fascinator-backup"</li>
 * <li>portal.xml <backup-email>email@usq.edu.au</backup-email> <backup-paths>
 * <field name="default">/home/octalina/.fascinator-backup</field>
 * </backup-paths></li>
 * </ul>
 * 
 * @author Linda octalina
 * 
 */

public class BackupClient {

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    public static final String DATETIME_FORMAT = DATE_FORMAT + "'T'hh:mm:ss'Z'";

    /** Default storage type will be used if none defined **/
    private static final String DEFAULT_STORAGE_TYPE = "file-system";

    /** Default indexer type if none defined **/
    private static final String DEFAULT_INDEXER_TYPE = "solr";

    private static Logger log = LoggerFactory.getLogger(BackupClient.class);

    /** Json configuration file **/
    private JsonConfig config;

    /** Email used to define user space **/
    private String email = null;

    /** Backup location **/
    private File backupDir = null;

    /** Portal query **/
    private String portalQuery = null;

    /**
     * Backup Client Constructor
     * 
     * @throws IOException
     */
    public BackupClient() throws IOException {
        config = new JsonConfig();
    }

    /**
     * Backup Client Constructor
     * 
     * @param jsonFile
     * @throws IOException
     */
    public BackupClient(File jsonFile) throws IOException {
        config = new JsonConfig(jsonFile);
    }

    /**
     * Backup Client Constructor
     * 
     * @param email
     * @param backupDir
     * @param portalQuery
     * @throws IOException
     */
    public BackupClient(String email, String backupDir, String portalQuery)
            throws IOException {
        config = new JsonConfig();
        setEmail(email);
        setBackupDir(backupDir);
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
    public void setBackupDir(String backupDir) {
        if (backupDir != null && backupDir != "") {
            this.backupDir = new File(backupDir);
            if (this.backupDir.exists() == false) {
                this.backupDir.getParentFile().mkdirs();
            }
        }
    }

    /**
     * Return backup location
     * 
     * @return backupDir
     */
    public File getBackupDir() {
        return backupDir;
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

        Storage realStorage;
        Indexer indexer;
        try {
            realStorage = PluginManager.getStorage(config.get("storage/type",
                    DEFAULT_STORAGE_TYPE));
            indexer = PluginManager.getIndexer(config.get("indexer/type",
                    DEFAULT_INDEXER_TYPE));
            realStorage.init(config.getSystemFile());
            indexer.init(config.getSystemFile());
            log.info("Loaded {} and {}", realStorage.getName(), indexer
                    .getName());

            // Set default email and backupDir if they are null
            String defaultEmail = config.get("email", null);
            String defaultBackupDir = config.get("backupDir", null);
            if (email == null && defaultEmail == null) {
                log.error("No email address provided in system-config.json");
                return;
            } else if (email == null) {
                setEmail(defaultEmail);
            }
            if (backupDir == null && defaultBackupDir == null) {
                log.error("No backup Directory provided in system-config.json");
                return;
            } else if (backupDir == null) {
                setBackupDir(defaultBackupDir);
            }
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
                JsonConfigHelper js = new JsonConfigHelper(result.toString());

                // Backup all the digital object returned by indexer
                for (Object oid : js.getList("response/docs/id")) {
                    String fileName = oid.toString();
                    DigitalObject digitalObject = realStorage
                            .getObject(fileName);
                    log.info("Backing up: " + fileName);
                    // File oidFile = new File(oid.toString());

                    // For window C: will cause error. so fix it as C_
                    // NOTE: remember to change it back when doing restore
                    if (System.getProperty("os.name").toLowerCase().indexOf(
                            "windows") > -1) {
                        fileName = fileName
                                .replace("C:", File.separator + "C_");
                    }
                    String outputFileName = backupDir.getAbsolutePath()
                            + File.separator + email + fileName;
                    File outputFile = new File(outputFileName);
                    outputFile.getParentFile().mkdirs();
                    OutputStream output = new FileOutputStream(outputFile);

                    // getSource still can't work. the sourceid is null
                    Payload payload = digitalObject.getSource();

                    // Payload payload = digitalObject.getPayload(oidFile
                    // .getName());
                    if (payload != null) {
                        IOUtils.copy(payload.getInputStream(), output);
                    }
                }

                startRow += numPerPage;
                numFound = Integer.parseInt(js.get("response/numFound"));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IndexerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //
        } while (startRow < numFound);

        log.info("Completed in "
                + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
    }

    public static void main(String[] args) {
        // if (args.length < 1 || (args[1] == "-h")) {
        // log.info("Usage: backup [<portal-name>]");
        // } else {

        // If without args, it will just backup everything
        try {
            BackupClient backup = new BackupClient();
            backup.run();
        } catch (IOException ioe) {
            log.error("Failed to initialise client: {}", ioe.getMessage());
        }
    }
}
