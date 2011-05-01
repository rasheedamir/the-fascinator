/* 
 * The Fascinator - Plugin - Harvester - JSON Queue
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
package au.edu.usq.fascinator.harvester.jsonq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.common.storage.impl.GenericPayload;

/**
 * 
 * @author Oliver Lucido
 */
public class JsonQMetadataPayload extends GenericPayload {

    /** File state information */
    private Map<String, String> info;

    /**
     * Creates a payload for a file with state changed information from the
     * Watcher service
     * 
     * @param file file content
     * @param info state information
     */
    public JsonQMetadataPayload(File file, Map<String, String> info) {
        this.info = info;
        info.put("uri", file.getAbsolutePath());
        setId(file.getName() + ".properties");
        setLabel("File State Metadata");
        setContentType("text/plain");
        setType(PayloadType.Annotation);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Properties props = new Properties();
        props.setProperty("uri", info.get("uri"));
        props.setProperty("state", info.get("state"));
        props.setProperty("time", info.get("time"));
        props.store(out, "File Metadata");
        return new ByteArrayInputStream(out.toByteArray());
    }
}
