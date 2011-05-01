/* 
 * The Fascinator - Plugin API
 * Copyright (C) 2008-2009 University of Southern Queensland
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
package au.edu.usq.fascinator.api.storage;

import java.util.List;

/**
 * Represents an object and its related payloads (or attachments)
 * 
 * @author Oliver Lucido
 */
public interface DigitalObject {

    /**
     * Gets the unique identifier for this object
     * 
     * @return an identifier
     */
    public String getId();

    /**
     * Gets the payload which represents the metadata for this object
     * 
     * @return a payload
     */
    public Payload getMetadata();

    /**
     * Gets the payload with the specified identifier
     * 
     * @param pid payload identifier
     * @return a payload
     */
    public Payload getPayload(String pid);

    /**
     * Gets the payloads related to this object
     * 
     * @return list of payloads
     */
    public List<Payload> getPayloadList();

    /**
     * Gets the Source related to this object
     * 
     * @return a payload
     */
    public Payload getSource();

}
