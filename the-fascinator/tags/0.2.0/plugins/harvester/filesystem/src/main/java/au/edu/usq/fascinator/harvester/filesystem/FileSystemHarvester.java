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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.Configurable;
import au.edu.usq.fascinator.api.harvester.Harvester;
import au.edu.usq.fascinator.api.harvester.HarvesterException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.common.JsonConfig;

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
 * </ul>
 * 
 * @author Oliver Lucido
 */
public class FileSystemHarvester implements Harvester, Configurable {

    /** file containing the full path for caching purposes */
    private static final String ID_FILE = "id.txt";

    /** logging */
    private Logger log = LoggerFactory.getLogger(FileSystemHarvester.class);

    /** default ignore list */
    private static final String DEFAULT_IGNORE_PATTERNS = ".svn";

    /** configuration */
    private JsonConfig config;

    /** directory to harvest */
    private File baseDir;

    /** whether or not to recursively harvest */
    private boolean recursive;

    /** current directory while harvesting */
    private File currentDir;

    /** stack of sub-directories while harvesting */
    private Stack<File> subDirs;

    /** whether or not there are more files to harvest */
    private boolean hasMore;

    /** current directory while harvesting */
    private File cacheCurrentDir;

    /** stack of sub-directories while harvesting */
    private Stack<File> cacheSubDirs;

    /** whether or not there are more files to harvest */
    private boolean hasMoreDeleted;

    /** filter used to ignore files matching specified patterns */
    private IgnoreFilter ignoreFilter;

    /** cache directory */
    private File cacheDir;

    /** force harvesting all files */
    private boolean force;

    /** filter used when detecting deleted files */
    private FileFilter idTxtFilter;

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

    @Override
    public String getId() {
        return "file-system";
    }

    @Override
    public String getName() {
        return "File System Harvester";
    }

    @Override
    public void init(File jsonFile) throws HarvesterException {
        try {
            config = new JsonConfig(jsonFile);
            baseDir = new File(config.get("harvester/file-system/baseDir", "."));
            log.info("Harvesting directory: {}", baseDir);
            recursive = Boolean.parseBoolean(config.get(
                    "harvester/file-system/recursive", "false"));
            ignoreFilter = new IgnoreFilter(config.get(
                    "harvester/file-system/ignoreFilter",
                    DEFAULT_IGNORE_PATTERNS).split("\\|"));
            cacheDir = null;
            String cacheDirValue = config.get("harvester/file-system/cacheDir");
            if (cacheDirValue != null) {
                cacheDir = new File(cacheDirValue);
                cacheDir.mkdirs();
                log.info("File system state will be cached in {}", cacheDir);
            }
            force = Boolean.parseBoolean(config.get(
                    "harvester/file-system/force", "false"));
            if (force) {
                log.info("Forcing harvest of all files...");
            }
            currentDir = baseDir;
            subDirs = new Stack<File>();
            hasMore = true;

            cacheCurrentDir = cacheDir;
            cacheSubDirs = new Stack<File>();
            hasMoreDeleted = true;
            idTxtFilter = new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory() || ID_FILE.equals(file.getName());
                }
            };
        } catch (IOException ioe) {
            throw new HarvesterException(ioe);
        }
    }

    @Override
    public void shutdown() throws HarvesterException {
    }

    @Override
    public List<DigitalObject> getObjects() throws HarvesterException {
        List<DigitalObject> fileObjects = new ArrayList<DigitalObject>();
        if (currentDir.isDirectory()) {
            File[] files = currentDir.listFiles(ignoreFilter);
            for (File file : files) {
                if (file.isDirectory()) {
                    if (recursive) {
                        subDirs.push(file);
                    }
                } else {
                    if (force || hasFileChanged(file)) {
                        fileObjects.add(new FileSystemDigitalObject(file));
                    }
                }
            }
            hasMore = !subDirs.isEmpty();
            if (hasMore) {
                currentDir = subDirs.pop();
            }
        } else {
            if (force || hasFileChanged(currentDir)) {
                fileObjects.add(new FileSystemDigitalObject(currentDir));
            }
            hasMore = false;
        }
        return fileObjects;
    }

    private boolean hasFileChanged(File file) {
        boolean changed = true;
        if (cacheDir != null) {
            try {
                File parentDir = getCacheDirForFile(file);
                File cacheFile = new File(parentDir, "checksum.txt");
                File idFile = new File(parentDir, ID_FILE);
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

    @Override
    public boolean hasMoreObjects() {
        return hasMore;
    }

    @Override
    public List<DigitalObject> getDeletedObjects() {
        List<DigitalObject> fileObjects = new ArrayList<DigitalObject>();
        if (cacheCurrentDir == null) {
            hasMoreDeleted = false;
        } else {
            if (cacheCurrentDir.isDirectory()) {
                File[] files = cacheCurrentDir.listFiles(idTxtFilter);
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

    private void addDeletedObject(List<DigitalObject> fileObjects, File idFile) {
        if (ID_FILE.equals(idFile.getName())) {
            try {
                String id = FileUtils.readFileToString(idFile);
                File realFile = new File(id);
                if (!realFile.exists()) {
                    FileUtils.deleteQuietly(idFile.getParentFile());
                    fileObjects.add(new FileSystemDigitalObject(realFile));
                }
            } catch (IOException ioe) {
                log.warn("Failed to read {}", idFile);
            }
        }
    }

    @Override
    public boolean hasMoreDeletedObjects() {
        return hasMoreDeleted;
    }

    @Override
    public String getConfig() {
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(getClass().getResourceAsStream(
                    "/" + getId() + "-config.html"), writer);
        } catch (IOException ioe) {
            writer.write("<span class=\"error\">" + ioe.getMessage()
                    + "</span>");
        }
        return writer.toString();
    }
}
