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
package au.edu.usq.fascinator.api.indexer;

import java.io.OutputStream;

import au.edu.usq.fascinator.api.Plugin;

/**
 * Provides an indexing service for digital objects and payloads
 * 
 * @author Oliver Lucido
 */
public interface Indexer extends Plugin {

    /**
     * Searches the index using the specified request. The search results are
     * written into the specified output stream and should generally be in a
     * JSON or XML format
     * 
     * @param request search request
     * @param result search results
     * @throws IndexerException if an error occurred performing the search
     */
    public void search(SearchRequest request, OutputStream result)
            throws IndexerException;

    /**
     * Adds an object to the index
     * 
     * @param oid an object identifier
     * @throws IndexerException if an error occurred while indexing
     */
    public void index(String oid) throws IndexerException;

    /**
     * Adds a payload entry to the index
     * 
     * @param oid an object identifier
     * @param pid a payload identifier
     * @throws IndexerException if an error occurred while indexing
     */
    public void index(String oid, String pid) throws IndexerException;

    /**
     * Removes an objects entry from the index
     * 
     * @param oid an object identifier
     * @throws IndexerException if an error occurred while indexing
     */
    public void remove(String oid) throws IndexerException;

    /**
     * Removes a payload entry from the index
     * 
     * @param oid an object identifier
     * @param pid a payload identifier
     * @throws IndexerException if an error occurred while indexing
     */
    public void remove(String oid, String pid) throws IndexerException;
}
