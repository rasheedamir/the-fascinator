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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.common.MimeTypeUtil;
import au.edu.usq.fascinator.common.storage.impl.GenericPayload;

public class FileSystemPayload extends GenericPayload {

    private Logger log = LoggerFactory.getLogger(FileSystemPayload.class);

    private File file;

    private File homeDir;

    private boolean linked;

    public FileSystemPayload(File homeDir, Payload payload) {
        super(payload);
        this.homeDir = homeDir;
        updateMeta(true);
    }

    public FileSystemPayload(File homeDir, File payloadFile) {
        this.homeDir = homeDir;
        file = payloadFile;
        if (file.isAbsolute()) {
            setId(payloadFile.getName());
        } else {
            setId(payloadFile.getPath());
        }
        updateMeta(true);

    }

    protected void updateMeta(boolean load) {
        // set default payload meta if not already set
        if (getType() == null) {
            setType(PayloadType.Data);
        }
        if (getLabel() == null) {
            setLabel(getFile().getAbsolutePath());
        }
        if (getContentType() == null) {
            setContentType(MimeTypeUtil.getMimeType(getFile()));
        }
        File metaFile = new File(getFile().getParentFile(), getFile().getName()
                + ".meta");
        try {
            Properties props = new Properties();
            if (load && metaFile.exists()) {
                Reader metaReader = new FileReader(metaFile);
                props.load(metaReader);
                metaReader.close();
                setId(props.getProperty("id", getId()));
                setType(PayloadType.valueOf(props.getProperty("payloadType",
                        getType().toString())));
                setLabel(props.getProperty("label", getId()));
                setContentType(props.getProperty("contentType",
                        getContentType()));
                setLinked(Boolean.parseBoolean(props.getProperty("linked",
                        String.valueOf(isLinked()))));
            }
            metaFile.getParentFile().mkdirs();
            OutputStream metaOut = new FileOutputStream(metaFile);
            props.setProperty("id", getId());
            props.setProperty("payloadType", getType().toString());
            props.setProperty("label", getLabel());
            props.setProperty("contentType", getContentType());
            props.setProperty("linked", String.valueOf(isLinked()));
            props.store(metaOut, "Payload metadata for "
                    + getFile().getAbsolutePath());
            metaOut.close();
        } catch (IOException ioe) {
            log.warn("Failed to read/write metaFile {}: {}", metaFile, ioe
                    .getMessage());
        }
    }

    public File getFile() {
        if (file == null) {
            return new File(homeDir, getId());
        } else {
            if (file.isAbsolute()) {
                return file;
            } else {
                return new File(homeDir, file.getPath());
            }
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (file == null) {
            return super.getInputStream();
        }
        if (isLinked()) {
            try {
                String realPath = FileUtils.readFileToString(getFile());
                log.debug("realPath: {}", realPath);
                file = new File(realPath);
            } catch (IOException ioe) {
                log.warn("Failed to get linked file", ioe);
            }
        }
        return new FileInputStream(getFile());
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", getLabel(), getId());
    }

    public void setLinked(boolean linked) {
        this.linked = linked;
    }

    public boolean isLinked() {
        return linked;
    }
}
