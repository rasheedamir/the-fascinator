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

import java.util.ArrayList;
import java.util.List;

import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;

/**
 * Generic DigitalObject implementation
 * 
 * @author Oliver Lucido
 */
public class GenericDigitalObject implements DigitalObject {

    /** Unique identifier */
    private String id;

    /** Identifier for the metadata payload */
    private String metadataId;

    /** List of Payloads attached to this object */
    private List<Payload> payloadList;

    /**
     * Creates a DigitalObject with the specified identifier and no metadata
     * 
     * @param id unique identifier
     */
    public GenericDigitalObject(String id) {
        this(id, null);
    }

    /**
     * Creates a DigitalObject with the specified identifier and metadata
     * 
     * @param id unique identifier
     * @param metaId identifier for the payload representing this object's
     *        metadata
     */
    public GenericDigitalObject(String id, String metaId) {
        setId(id);
        setMetadataId(metaId);
    }

    /**
     * Creates a copy of the specified DigitalObject
     * 
     * @param object object to copy
     */
    public GenericDigitalObject(DigitalObject object) {
        setId(object.getId());
        Payload metadata = object.getMetadata();
        if (metadata == null) {
            setMetadataId(object.getId());
        } else {
            setMetadataId(metadata.getId());
        }
        for (Payload payload : object.getPayloadList()) {
            addPayload(payload);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets the identifier for this object
     * 
     * @param id unique identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Payload getMetadata() {
        return getPayload(metadataId);
    }

    /**
     * Sets the identifier for the payload which represents this object's
     * metadata
     * 
     * @param metadataId payload identifier to use as metadata
     */
    public void setMetadataId(String metadataId) {
        this.metadataId = metadataId;
    }

    @Override
    public Payload getPayload(String pid) {
        if (pid != null) {
            for (Payload payload : getPayloadList()) {
                if (pid.equals(payload.getId())) {
                    return payload;
                }
            }
        }
        return null;
    }

    /**
     * Adds a payload to this object
     * 
     * @param payload the payload to add
     */
    public void addPayload(Payload payload) {
        getPayloadList().add(payload);
    }

    @Override
    public List<Payload> getPayloadList() {
        if (payloadList == null) {
            payloadList = new ArrayList<Payload>();
        }
        return payloadList;
    }

    @Override
    public String toString() {
        return getId();
    }
}
