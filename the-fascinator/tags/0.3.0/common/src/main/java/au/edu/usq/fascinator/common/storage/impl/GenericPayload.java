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

import java.io.IOException;
import java.io.InputStream;

import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;

/**
 * Generic Payload implementation
 * 
 * @author Oliver Lucido
 */
public class GenericPayload implements Payload {

    /** Payload type */
    private PayloadType type;

    /** Identifier */
    private String id;

    /** Descriptive label */
    private String label;

    /** Content (MIME) type */
    private String contentType;

    /** Input stream to read content data from */
    private InputStream inputStream;

    /**
     * Creates an empty payload
     */
    public GenericPayload() {
    }

    /**
     * Creates a data payload with the specified identifier, label and content
     * type, but no content stream
     * 
     * @param id an identifier
     * @param label a descriptive label
     * @param contentType the content type
     */
    public GenericPayload(String id, String label, String contentType) {
        setId(id);
        setLabel(label);
        setContentType(contentType);
        setType(PayloadType.Data);
    }

    /**
     * Creates a copy of the specified payload
     * 
     * @param payload payload to copy
     */
    public GenericPayload(Payload payload) {
        if (payload != null) {
            setId(payload.getId());
            setLabel(payload.getLabel());
            setContentType(payload.getContentType());
            setType(payload.getType());
            try {
                setInputStream(payload.getInputStream());
            } catch (IOException e) {
            }
        }
    }

    @Override
    public PayloadType getType() {
        return type;
    }

    /**
     * Sets the payload type for this payload
     * 
     * @param type payload type
     */
    public void setType(PayloadType type) {
        this.type = type;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets the identifier for this payload
     * 
     * @param id an identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    /**
     * Sets a descriptive label for this payload
     * 
     * @param label a descriptive label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content (MIME) type for this payload
     * 
     * @param contentType a content type
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    /**
     * Sets the content input stream for this payload
     * 
     * @param inputStream an input stream
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public String toString() {
        return getId();
    }
}
