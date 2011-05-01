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

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents content for a digital object
 * 
 * @author Oliver Lucido
 */
public interface Payload {

    /**
     * Gets the type of this payload
     * 
     * @return payload type
     */
    public PayloadType getType();

    /**
     * Gets the identifier for this payload
     * 
     * @return an identifier
     */
    public String getId();

    /**
     * Gets the descriptive label for this payload
     * 
     * @return a label
     */
    public String getLabel();

    /**
     * Gets the content (MIME) type for this payload
     * 
     * @return a MIME type
     */
    public String getContentType();

    /**
     * Gets the input stream to access the content for this payload
     * 
     * @return an input stream
     * @throws IOException if there was an error reading the stream
     */
    public InputStream getInputStream() throws IOException;

}
