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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject;

public class FileSystemDigitalObject extends GenericDigitalObject {

    private Logger log = LoggerFactory.getLogger(FileSystemDigitalObject.class);

    private File homeDir;

    private File path;

    private String hashId;

    public FileSystemDigitalObject(File homeDir, String oid) {
        super(oid);
        this.homeDir = homeDir;
    }

    public String getHashId() {
        if (hashId == null) {
            hashId = DigestUtils.md5Hex(getId());
        }
        return hashId;
    }

    public File getPath() {
        if (path == null) {
            String dir = getHashId().substring(0, 2) + File.separator
                    + getHashId().substring(2, 4);
            File parentDir = new File(homeDir, dir);
            path = new File(parentDir, getHashId());
        }
        return path;
    }

    @Override
    public List<Payload> getPayloadList() {
        List<Payload> payloadList = new ArrayList<Payload>();
        addPayloadDir(payloadList, getPath(), 0);
        return payloadList;
    }

    private void addPayloadDir(List<Payload> payloadList, File dir, int depth) {
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.endsWith(".meta");
            }
        });
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    File payloadFile = file;
                    if (depth > 0) {
                        File parentFile = file.getParentFile();
                        String relPath = "";
                        for (int i = 0; i < depth; i++) {
                            relPath = parentFile.getName() + File.separator
                                    + relPath;
                            parentFile = parentFile.getParentFile();
                        }
                        payloadFile = new File(relPath, file.getName());
                    }
                    Payload payload = new FileSystemPayload(getPath(),
                            payloadFile);
                    if (payload.getType().equals(PayloadType.Data)) {
                        setSourceId(payload.getId());
                    }
                    payloadList.add(payload);
                } else if (file.isDirectory()) {
                    addPayloadDir(payloadList, file, depth + 1);
                }
            }
        }
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", getId(), getPath());
    }

    @Override
    public Payload getSource() {
        getPayloadList();
        return super.getSource();
    }
}
