/* 
 * The Fascinator - Common Library
 * Copyright (C) 2008 University of Southern Queensland
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
package au.edu.usq.fascinator.common.storage.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.common.MimeTypeUtil;

/**
 * A payload that represents a file on the local file system
 * 
 * @author Oliver Lucido
 */
public class FilePayload extends GenericPayload {

    /** the file this payload represents */
    private File payloadFile;

    /**
     * Creates a payload for the specified file
     * 
     * @param payloadFile a file
     */
    public FilePayload(File payloadFile) {
        this.payloadFile = payloadFile;
        setId(payloadFile.getName());
        setLabel(payloadFile.getPath());
        if (payloadFile.exists()) {
            setContentType(MimeTypeUtil.getMimeType(payloadFile));
        } else {
            setContentType(MimeTypeUtil.DEFAULT_MIME_TYPE);
        }
        setType(PayloadType.Data);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(payloadFile);
    }
}
