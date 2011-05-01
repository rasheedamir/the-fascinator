/*
 * The Fascinator - Plugin - Transformer - ICE 2
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
package au.edu.usq.fascinator.transformer.ice2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.common.MimeTypeUtil;
import au.edu.usq.fascinator.common.storage.impl.GenericPayload;

/**
 * Wraps a ZipEntry as a payload
 * 
 * @author Linda Octalina
 * @author Oliver Lucido
 */
public class IcePayload extends GenericPayload {

    private Logger log = LoggerFactory.getLogger(IcePayload.class);

    private File filePath;
    private boolean isZip = false;
    private ZipEntry zipEntry;

    public IcePayload(File filePath, ZipEntry zipEntry) {
        this.filePath = filePath;
        this.zipEntry = zipEntry;
        String name = filePath.getName();
        if (this.zipEntry != null) {
            isZip = true;
            name = zipEntry.getName();
        }
        setId(name);
        setLabel(name);
        setContentType(MimeTypeUtil.getMimeType(name));
        setType(PayloadType.Enrichment); // need to store somewhere
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (isZip) {
            ZipFile zipFile = new ZipFile(filePath);
            return zipFile.getInputStream(zipEntry);
        } else {
            return new FileInputStream(filePath);
        }
    }
}
