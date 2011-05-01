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
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

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
 * </ul>
 * 
 * @author Oliver Lucido
 */
public class FileSystemHarvester implements Harvester, Configurable {

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

    /** filter used to ignore files matching specified patterns */
    private IgnoreFilter ignoreFilter;

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
            recursive = Boolean.parseBoolean(config.get(
                    "harvester/file-system/recursive", "false"));
            ignoreFilter = new IgnoreFilter(config.get(
                    "harvester/file-system/ignoreFilter",
                    DEFAULT_IGNORE_PATTERNS).split("\\|"));
            currentDir = baseDir;
            hasMore = true;
            subDirs = new Stack<File>();
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
                    fileObjects.add(new FileSystemDigitalObject(file));
                }
            }
            hasMore = !subDirs.isEmpty();
            if (hasMore) {
                currentDir = subDirs.pop();
            }
        } else {
            fileObjects.add(new FileSystemDigitalObject(currentDir));
            hasMore = false;
        }
        return fileObjects;
    }

    @Override
    public boolean hasMoreObjects() {
        return hasMore;
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
