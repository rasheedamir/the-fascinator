/* 
 * The Fascinator - File System Harvester Plugin
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
package au.edu.usq.fascinator.harvester.filesystem;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.harvester.HarvesterException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.harvester.impl.GenericHarvester;
import au.edu.usq.fascinator.common.storage.StorageUtils;

/**
 * Harvests files in a specified directory on the local file system
 * <p>
 * Configuration options:
 * <ul>
 * <li>baseDir: directory to harvest</li>
 * <li>recursive: set true to recurse into sub-directories (default: false)</li>
 * <li>ignoreFilter: wildcard patterns of files to ignore separated by '|'
 * (default: .svn)</li>
 * <li>cacheDir: location for cache files</li>
 * <li>force: set true to force harvest of all files (ignore cache)</li>
 * <li>link: set true to link to original files, false to create copies</li>
 * </ul>
 * 
 * @author Oliver Lucido
 */
public class FileSystemHarvester extends GenericHarvester {

    /** file containing the full path for caching purposes */
    private static final String CACHE_ID_FILE = "id.txt";

    /** default ignore list */
    private static final String DEFAULT_IGNORE_PATTERNS = ".svn";

    /** logging */
    private Logger log = LoggerFactory.getLogger(FileSystemHarvester.class);

    /** Harvesting targets */
    private JsonConfigHelper[] targets;

    /** Target index */
    private Integer targetIndex;

    /** Target index */
    private File nextFile;

    /** Stack of queued files to harvest */
    private Stack<File> fileStack;

    /** Path data for facet */
    private String facetBase;

    /** whether or not there are more files to harvest */
    private boolean hasMore;

    /** whether or not there are more files to harvest */
    private boolean hasMoreDeleted;

    /** current directory while harvesting */
    private File cacheCurrentDir;

    /** stack of sub-directories while harvesting */
    private Stack<File> cacheSubDirs;

    /** filter used to ignore files matching specified patterns */
    private IgnoreFilter ignoreFilter;

    /** cache directory */
    private File cacheDir;

    /** whether or not to recursively harvest */
    private boolean recursive;

    /** force harvesting all files */
    private boolean force;

    /** use links instead of copying */
    private boolean link;

    /** filter used when detecting deleted files */
    private FileFilter cacheIdFilter;

    /** Render chains */
    private Map<String, Map<String, List<String>>> renderChains;

    /**
     * File filter used to ignore specified files
     */
    private class IgnoreFilter implements FileFilter {

        /** wildcard patterns of files to ignore */
        private String[] patterns;

        public IgnoreFilter(String[] patterns) {
            this.patterns = patterns;
        }

        @Override
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
     * File System Harvester Constructor
     */
    public FileSystemHarvester() {
        super("file-system", "File System Harvester");
    }

    /**
     * Initialisation of File system harvester plugin
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
        // Check for valid targest
        List<JsonConfigHelper> list = config
                .getJsonList("harvester/file-system/targets");
        if (list.isEmpty()) {
            throw new HarvesterException("No targets specified");
        }

        // Loop processing variables
        fileStack = new Stack<File>();
        targets = list.toArray(new JsonConfigHelper[list.size()]);
        targetIndex = null;
        hasMore = true;
        hasMoreDeleted = true;

        // Caching variables
        cacheDir = null;
        String cachePath = config.get("harvester/file-system/cacheDir");
        if (cachePath != null) {
            cacheDir = new File(cachePath);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            if (cacheDir.exists() && cacheDir.isDirectory()) {
                log.info("File system state will be cached in {}", cacheDir);
            } else {
                cacheDir = null;
                log.warn("Cache location '{}' is invalid or not a directory,"
                        + " caching disabled", cacheDir);
            }
        }
        cacheCurrentDir = cacheDir;
        cacheSubDirs = new Stack<File>();

        // Order is significant
        renderChains = new LinkedHashMap();
        Map<String, JsonConfigHelper> renderTypes = config
                .getJsonMap("renderTypes");
        for (String name : renderTypes.keySet()) {
            Map<String, List<String>> details = new HashMap();
            JsonConfigHelper chain = renderTypes.get(name);
            details.put("fileTypes", getList(chain, "fileTypes"));
            details.put("harvestQueue", getList(chain, "harvestQueue"));
            details.put("indexOnHarvest", getList(chain, "indexOnHarvest"));
            details.put("renderQueue", getList(chain, "renderQueue"));
            renderChains.put(name, details);
        }

        cacheIdFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory()
                        || CACHE_ID_FILE.equals(file.getName());
            }
        };

        // Prep the first file
        nextFile = getNextFile();
    }

    /**
     * Get the next file due to be harvested
     * 
     * @return The next file to harvest, null if none
     */
    private File getNextFile() {
        File next = null;
        if (fileStack.empty()) {
            next = getNextTarget();
        } else {
            next = fileStack.pop();
        }
        if (next == null) {
            hasMore = false;
        }
        return next;
    }

    /**
     * Retrieve the next file specified as a target in configuration
     * 
     * @return The next target file, null if none
     */
    private File getNextTarget() {
        // First execution
        if (targetIndex == null) {
            targetIndex = new Integer(0);
        } else {
            targetIndex++;
        }

        // We're finished
        if (targetIndex >= targets.length) {
            return null;
        }

        // Get the next target
        JsonConfigHelper target = targets[targetIndex];
        String path = target.get("baseDir");
        if (path == null) {
            log.warn("No path provided for target, skipping!");
            return getNextTarget();

        } else {
            File file = new File(path);
            if (!file.exists()) {
                log.warn("Path '{}' does not exist, skipping!", path);
                return getNextTarget();

            } else {
                log.info("Target file/directory found: '{}'", path);
                updateConfig(target, path);
                return file;
            }
        }
    }

    /**
     * Update harvest configuration when switching target path
     * 
     * @param tConfig The target configuration
     * @param path The path to the target (used as default facet)
     */
    private void updateConfig(JsonConfigHelper tConfig, String path) {
        recursive = Boolean.parseBoolean(tConfig.get("recursive", "false"));
        ignoreFilter = new IgnoreFilter(tConfig.get("ignoreFilter",
                DEFAULT_IGNORE_PATTERNS).split("\\|"));
        force = Boolean.parseBoolean(tConfig.get("force", "false"));
        link = Boolean.parseBoolean(tConfig.get("link", "false"));
        facetBase = tConfig.get("facetDir", path);
    }

    /**
     * Shutdown the plugin
     * 
     * @throws HarvesterException is there are errors
     */
    @Override
    public void shutdown() throws HarvesterException {
    }

    /**
     * Harvest the next set of files, and return their Object IDs
     * 
     * @return Set<String> The set of object IDs just harvested
     * @throws HarvesterException is there are errors
     */
    @Override
    public Set<String> getObjectIdList() throws HarvesterException {
        Set<String> fileObjectIdList = new HashSet<String>();

        // We had no valid targets
        if (nextFile == null) {
            hasMore = false;
            return fileObjectIdList;
        }

        // Normal logic
        if (nextFile.isDirectory()) {
            File[] children = nextFile.listFiles(ignoreFilter);
            for (File child : children) {
                if (child.isDirectory()) {
                    if (recursive) {
                        fileStack.push(child);
                    }
                } else {
                    harvestFile(fileObjectIdList, child);
                }
            }

        } else {
            harvestFile(fileObjectIdList, nextFile);
        }

        // Progess the stack and return
        nextFile = getNextFile();
        return fileObjectIdList;
    }

    /**
     * Harvest a file based on configuration
     * 
     * @param list The set of harvested IDs to add to
     * @param file The file to harvest
     * @throws HarvesterException is there are errors
     */
    private void harvestFile(Set<String> list, File file)
            throws HarvesterException {
        if (force || hasFileChanged(file)) {
            try {
                list.add(createDigitalObject(file));
            } catch (StorageException se) {
                log.warn("File not harvested {}: {}", file, se.getMessage());
            }
        }
    }

    /**
     * Check if file has changed
     * 
     * @param file File to be checked
     * @return <code>true</code> if changed, <code>false</code> otherwise
     */
    private boolean hasFileChanged(File file) {
        boolean changed = true;
        if (cacheDir != null) {
            try {
                File parentDir = getCacheDirForFile(file);
                File cacheFile = new File(parentDir, "checksum.txt");
                File idFile = new File(parentDir, CACHE_ID_FILE);
                if (cacheFile != null) {
                    InputStream fis = new FileInputStream(file);
                    String sha1 = DigestUtils.shaHex(fis);
                    fis.close();
                    if (cacheFile.exists()) {
                        String cachedSha1 = FileUtils
                                .readFileToString(cacheFile);
                        log.trace("Comparing {} with {}", sha1, cachedSha1);
                        if (sha1.equals(cachedSha1)) {
                            log.debug("{} has not changed", file);
                            changed = false;
                        } else {
                            log.debug("{} has changed", file);
                            changed = true;
                            FileUtils.writeStringToFile(cacheFile, sha1);
                            FileUtils.writeStringToFile(idFile, file
                                    .getAbsolutePath());
                        }
                    } else {
                        log.debug("Caching checksum for {}", file);
                        FileUtils.writeStringToFile(cacheFile, sha1);
                        FileUtils.writeStringToFile(idFile, file
                                .getAbsolutePath());
                    }
                }
            } catch (IOException ioe) {
                log.error("Failed to cache " + file, ioe);
            }
        }
        return changed;
    }

    /**
     * Get the cache directory for the file
     * 
     * @param file File to be checked
     * @return the cache directory if found, <code>null</code> otherwise
     */
    private File getCacheDirForFile(File file) {
        if (cacheDir != null) {
            String hash = DigestUtils.shaHex(file.getAbsolutePath());
            File shaDir = new File(cacheDir, hash.substring(0, 2));
            shaDir = new File(shaDir, hash.substring(2, 4));
            shaDir = new File(shaDir, hash);
            shaDir.mkdirs();
            return shaDir;
        }
        return null;
    }

    /**
     * Check if there are more objects to harvest
     * 
     * @return <code>true</code> if there are more, <code>false</code> otherwise
     */
    @Override
    public boolean hasMoreObjects() {
        return hasMore;
    }

    /**
     * Delete cached references to files which no longer exist and return the
     * set of IDs to delete from the system.
     * 
     * @return Set<String> The set of object IDs deleted
     * @throws HarvesterException is there are errors
     */
    @Override
    public Set<String> getDeletedObjectIdList() throws HarvesterException {
        Set<String> fileObjects = new HashSet<String>();
        if (cacheCurrentDir == null) {
            hasMoreDeleted = false;
        } else {
            if (cacheCurrentDir.isDirectory()) {
                File[] files = cacheCurrentDir.listFiles(cacheIdFilter);
                if (files.length == 0) {
                    // remove it
                    FileUtils.deleteQuietly(cacheCurrentDir);
                } else {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            cacheSubDirs.push(file);
                        } else {
                            addDeletedObject(fileObjects, file);
                        }
                    }
                }
                hasMoreDeleted = !cacheSubDirs.isEmpty();
                if (hasMoreDeleted) {
                    cacheCurrentDir = cacheSubDirs.pop();
                }
            } else {
                addDeletedObject(fileObjects, cacheCurrentDir);
                hasMoreDeleted = false;
            }
        }
        return fileObjects;
    }

    /**
     * Add the deleted object to the file object list and delete the file
     * quietly
     * 
     * @param fileObjectIdList list of files to be deleted
     * @param idFile file to be deleted
     * @throws HarvesterException if fail to read the file
     */
    private void addDeletedObject(Set<String> fileObjectIdList, File idFile)
            throws HarvesterException {
        if (CACHE_ID_FILE.equals(idFile.getName())) {
            try {
                String id = FileUtils.readFileToString(idFile);
                File realFile = new File(id);
                if (!realFile.exists()) {
                    FileUtils.deleteQuietly(idFile.getParentFile());
                    fileObjectIdList.add(StorageUtils.generateOid(realFile));
                }
            } catch (IOException ioe) {
                log.warn("Failed to read {}", idFile);
            }
        }
    }

    /**
     * Check if there are more objects to delete
     * 
     * @return <code>true</code> if there are more, <code>false</code> otherwise
     */
    @Override
    public boolean hasMoreDeletedObjects() {
        return hasMoreDeleted;
    }

    /**
     * Create digital object
     * 
     * @param file File to be transformed to be digital object
     * @return object id of created digital object
     * @throws HarvesterException if fail to create the object
     * @throws StorageException if fail to save the file to the storage
     */
    private String createDigitalObject(File file) throws HarvesterException,
            StorageException {
        DigitalObject object = StorageUtils.storeFile(getStorage(), file, link);

        // update object metadata
        Properties props = object.getMetadata();
        props.setProperty("render-pending", "true");
        props.setProperty("file.path", FilenameUtils.separatorsToUnix(file
                .getAbsolutePath()));
        props.setProperty("base.file.path", FilenameUtils
                .separatorsToUnix(facetBase));

        // Store rendition information if we have it
        String ext = FilenameUtils.getExtension(file.getName());
        for (String chain : renderChains.keySet()) {
            Map<String, List<String>> details = renderChains.get(chain);
            if (details.get("fileTypes").contains(ext)) {
                storeList(props, details, "harvestQueue");
                storeList(props, details, "indexOnHarvest");
                storeList(props, details, "renderQueue");
            }
        }

        object.close();
        return object.getId();
    }

    /**
     * Get a list of strings from configuration
     * 
     * @param json Configuration object to retrieve from
     * @param field The path to the list
     * @return List<String> The resulting list
     */
    private List<String> getList(JsonConfigHelper json, String field) {
        List<String> result = new ArrayList();
        List<Object> list = json.getList(field);
        for (Object object : list) {
            result.add((String) object);
        }
        return result;
    }

    /**
     * Take a list of strings from a Java Map, concatenate the values together
     * and store them in a Properties object using the Map's original key.
     * 
     * @param props Properties object to store into
     * @param details The full Java Map
     * @param field The key to use in both objects
     */
    private void storeList(Properties props, Map<String, List<String>> details,
            String field) {
        Set<String> valueSet = new LinkedHashSet<String>();
        // merge with original property value if exists
        String currentValue = props.getProperty(field, "");
        if (!"".equals(currentValue)) {
            String[] currentList = currentValue.split(",");
            valueSet.addAll(Arrays.asList(currentList));
        }
        valueSet.addAll(details.get(field));
        String joinedList = StringUtils.join(valueSet, ",");
        props.setProperty(field, joinedList);
    }
}
