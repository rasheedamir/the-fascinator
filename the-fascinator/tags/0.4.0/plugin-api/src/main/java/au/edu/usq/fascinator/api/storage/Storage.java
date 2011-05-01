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

import au.edu.usq.fascinator.api.Plugin;

/**
 * Provides storage, retrieval and management of digital objects and payloads
 * 
 * @author Oliver Lucido
 */
public interface Storage extends Plugin {

    /**
     * Adds a digital object to the store
     * 
     * @param object the object to add
     * @return an internal identifier used by the storage implementation
     * @throws StorageException if there was an error adding the object
     */
    public String addObject(DigitalObject object) throws StorageException;

    /**
     * Removes the specified object from the store
     * 
     * @param oid an object identifier
     */
    public void removeObject(String oid);

    /**
     * Adds a payload to the specified object
     * 
     * @param oid an object identifier
     * @param payload the payload to add
     */
    public void addPayload(String oid, Payload payload);

    /**
     * Removes a payload from the specified object
     * 
     * @param oid an object identifier
     * @param pid a payload identifier
     */
    public void removePayload(String oid, String pid);

    /**
     * Gets the object with the specified identifier
     * 
     * @param oid an object identifier
     * @return a digital object or null if not found
     */
    public DigitalObject getObject(String oid);

    /**
     * Gets the payload from the specified object
     * 
     * @param oid an object identifier
     * @param pid a payload identifier
     * @return a payload or null if not found
     */
    public Payload getPayload(String oid, String pid);

    /**
     * Gets all the objects from the storage
     * 
     * @return List of Digital Object
     */
    public List<DigitalObject> getObjectList();
}
