/* 
 * The Fascinator - File System storage plugin
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
package au.edu.usq.fascinator.storage.filesystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfig;

/**
 * File system storage plugin based on Dflat/Pairtree
 * 
 * @author Oliver Lucido
 */
public class FileSystemStorage implements Storage {

    private static final String DEFAULT_HOME_DIR = System
            .getProperty("user.home")
            + File.separator + ".fascinator" + File.separator + "storage";

    private final Logger log = LoggerFactory.getLogger(FileSystemStorage.class);

    private File homeDir;

    public String getId() {
        return "file-system";
    }

    public String getName() {
        return "File System Storage";
    }

    public void init(File jsonFile) throws StorageException {
        try {
            JsonConfig config = new JsonConfig(jsonFile);
            homeDir = new File(config.get("storage/file-system/home",
                    DEFAULT_HOME_DIR));
            if (!homeDir.exists()) {
                homeDir.mkdirs();
            }
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
    }

    public void shutdown() throws StorageException {
        // Don't need to do anything
    }

    public String addObject(DigitalObject object) throws StorageException {
        FileSystemDigitalObject fileObject = new FileSystemDigitalObject(
                homeDir, object.getId());
        log.debug("Adding object {}", fileObject);
        for (Payload payload : object.getPayloadList()) {
            addPayload(fileObject.getId(), payload);
        }
        return fileObject.getPath().getAbsolutePath();
    }

    public void removeObject(String oid) {
        log.debug("Removing object {}", oid);
        FileSystemDigitalObject fileObject = (FileSystemDigitalObject) getObject(oid);
        FileUtils.deleteQuietly(fileObject.getPath());
    }

    public void addPayload(String oid, Payload payload) {
        log.debug("Adding payload {} to {}", payload.getId(), oid);
        FileSystemDigitalObject fileObject = (FileSystemDigitalObject) getObject(oid);
        FileSystemPayload filePayload = new FileSystemPayload(fileObject
                .getPath(), payload);
        File payloadFile = filePayload.getFile();
        File parentDir = payloadFile.getParentFile();
        parentDir.mkdirs();
        try {
            FileOutputStream out = new FileOutputStream(payloadFile);
            IOUtils.copy(filePayload.getInputStream(), out);
            out.close();
        } catch (IOException ioe) {
            log.error("Failed to add payload", ioe);
        }
    }

    public void removePayload(String oid, String pid) {
        log.debug("Removing payload {} from {}", pid, oid);
    }

    public DigitalObject getObject(String oid) {
        log.debug("Getting object {}", oid);
        return new FileSystemDigitalObject(homeDir, oid);
    }

    public Payload getPayload(String oid, String pid) {
        log.debug("Getting payload {} from {}", pid, oid);
        return getObject(oid).getPayload(pid);
    }

    public File getHomeDir() {
        return homeDir;
    }
}
