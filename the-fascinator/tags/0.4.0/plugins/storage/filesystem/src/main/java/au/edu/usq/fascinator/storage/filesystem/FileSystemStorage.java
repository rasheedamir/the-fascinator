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

import static au.edu.usq.fascinator.api.storage.PayloadType.Data;
import static au.edu.usq.fascinator.api.storage.PayloadType.External;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
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

    private String email;

    private boolean useLink;

    public String getId() {
        return "file-system";
    }

    public String getName() {
        return "File System Storage";
    }

    public void init(String jsonString) throws StorageException {
        try {
            JsonConfig config = new JsonConfig(new ByteArrayInputStream(
                    jsonString.getBytes("UTF-8")));
            setVariable(config);
        } catch (UnsupportedEncodingException e) {
            throw new StorageException(e);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    private void setVariable(JsonConfig config) {
        useLink = Boolean.parseBoolean(String.valueOf(config.get(
                "storage/file-system/use-link", "false")));
        String email = config.get("email");

        if (!email.equals("")) {
            email = DigestUtils.md5Hex(config.get("email"));
        }
        homeDir = new File(config.get("storage/file-system/home",
                DEFAULT_HOME_DIR), email);
        if (!homeDir.exists()) {
            homeDir.mkdirs();
        }
    }

    public void init(File jsonFile) throws StorageException {
        try {
            JsonConfig config = new JsonConfig(jsonFile);
            setVariable(config);
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
    }

    public void shutdown() throws StorageException {
        // Don't need to do anything
    }

    public String addObject(DigitalObject object) throws StorageException {
        FileSystemDigitalObject fileObject = new FileSystemDigitalObject(
                homeDir, object.getId(), useLink);
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
                .getPath(), payload, useLink);
        File payloadFile = filePayload.getFile();
        File parentDir = payloadFile.getParentFile();
        parentDir.mkdirs();
        try {
            // FileOutputStream out = new FileOutputStream(payloadFile);
            // File realFile = new File(oid);
            PayloadType type = payload.getType();
            // if (realFile.isFile()
            // && (Data.equals(type) || External.equals(type))) {
            if (useLink && (Data.equals(type) || External.equals(type))) {
                FileOutputStream out = new FileOutputStream(payloadFile);
                filePayload.setLinked(true);
                filePayload.updateMeta(false);
                IOUtils.write(oid, out);
                out.close();
            } else {
                File tempFile = File.createTempFile("payload", ".temp");
                FileOutputStream tempOut = new FileOutputStream(tempFile);
                IOUtils.copy(filePayload.getInputStream(), tempOut);
                tempOut.close();
                FileUtils.copyFile(tempFile, payloadFile);

                // IOUtils.copy(tempOut, out);
                // log.info("sofmeta: " +
                // FileUtils.readFileToString(payloadFile));
            }
            // out.close();
        } catch (IOException ioe) {
            log.error("Failed to add payload", ioe);
        }
    }

    public void removePayload(String oid, String pid) {
        log.debug("Removing payload {} from {}", pid, oid);
        FileSystemPayload payload = (FileSystemPayload) getPayload(oid, pid);
        File realFile = payload.getFile();

        if (realFile.exists()) {
            FileUtils.deleteQuietly(realFile);
        }

        // Need to remove the .meta file as well
        File metaFile = new File(realFile.getAbsoluteFile(), ".meta");
        if (metaFile.exists()) {
            FileUtils.deleteQuietly(realFile);
        }
    }

    public DigitalObject getObject(String oid) {
        log.debug("Getting object {}", oid);
        return new FileSystemDigitalObject(homeDir, oid, useLink);
    }

    public Payload getPayload(String oid, String pid) {
        log.debug("Getting payload {} from {}", pid, oid);
        return getObject(oid).getPayload(pid);
    }

    public File getHomeDir() {
        return homeDir;
    }

    /**
     * Get the List of Digital Object found in the storage
     * 
     * @return List of Digital Object
     */
    public List<DigitalObject> getObjectList() {
        List<DigitalObject> objectList = new ArrayList<DigitalObject>();

        List<File> files = new ArrayList<File>();
        listFileRecur(files, homeDir);

        for (File file : files) {
            log.info("file: " + file.getAbsolutePath());
            Properties sofMeta = new Properties();
            InputStream is;
            try {
                is = new FileInputStream(file);
                sofMeta.load(is);
                String objectId = sofMeta.getProperty("objectId");
                log.info("objectId:" + objectId);
                FileSystemDigitalObject digitalObject = (FileSystemDigitalObject) getObject(objectId);
                if (digitalObject != null) {
                    objectList.add(digitalObject);
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return objectList;
    }

    private void listFileRecur(List<File> files, File path) {
        if (path.isDirectory()) {
            // for (File file : path.listFiles(new FilenameFilter() {
            // @Override
            // public boolean accept(File dir, String name) {
            // return name.equals("SOF-META");
            // }
            // })) {
            for (File file : path.listFiles()) {
                if (path.isDirectory()) {
                    listFileRecur(files, file);
                } else {
                    if (file.getName().equals("SOF-META")) {
                        files.add(file);
                    }
                }
            }
        } else {
            if (path.getName().equals("SOF-META")) {
                files.add(path);
            }
        }
    }
}
