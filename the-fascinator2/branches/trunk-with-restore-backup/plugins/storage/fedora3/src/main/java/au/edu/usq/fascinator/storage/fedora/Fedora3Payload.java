/* 
 * The Fascinator - Fedora Commons 3.x storage plugin
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
package au.edu.usq.fascinator.storage.fedora;

import java.io.IOException;
import java.io.InputStream;

import au.edu.usq.fascinator.common.storage.impl.GenericPayload;
import au.edu.usq.fedora.RestClient;

/**
 * Represent each Datastream of Fedora object
 * 
 * @author Linda Octalina & Oliver Lucido
 * 
 */
public class Fedora3Payload extends GenericPayload {

    /** API to talk to Fedora **/
    private RestClient client;

    /** object id **/
    private String oid;

    /** payload id **/
    private String pid;

    /**
     * Constructor method for Fedora3Payload
     * 
     * @param client
     * @param oid
     * @param pid
     */
    public Fedora3Payload(RestClient client, String oid, String pid) {
        this.client = client;
        this.oid = oid;
        this.pid = pid;
    }

    /**
     * Get the inputstream of the payload
     * 
     * @return InputStream of the payload
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return client.getStream(oid, pid);
    }

}
