/* 
 * The Fascinator - Plugin - Harvester - OAI-PMH
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
package au.edu.usq.fascinator.harvester.oaipmh;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import se.kb.oai.pmh.Record;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.common.storage.impl.GenericPayload;

/**
 * Represents an OAI-PMH metadata payload
 * 
 * @author Oliver Lucido
 */
public class OaiPmhPayload extends GenericPayload {

    /** OAI-PMH record */
    private Record record;

    /**
     * Creates an OAI-PMH metadata payload. The identifier is usually the
     * metadataPrefix used in the OAI-PMH request.
     * 
     * @param pid payload identifier
     * @param record a OAI-PMH record
     */
    public OaiPmhPayload(String pid, Record record) {
        this.record = record;
        setId(pid);
        setLabel("oai_dc".equals(pid) ? "Dublin Core Metadata" : "Metadata ("
                + pid + ")");
        setContentType("text/xml");
        setType(PayloadType.Annotation);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return IOUtils.toInputStream(record.getMetadataAsString());
    }
}
