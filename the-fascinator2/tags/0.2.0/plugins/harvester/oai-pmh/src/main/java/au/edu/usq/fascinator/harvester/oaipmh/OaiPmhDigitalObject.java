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

import se.kb.oai.pmh.Record;
import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject;

/**
 * Represents an OAI-PMH record with a single metadata payload
 * 
 * @author Oliver Lucido
 */
public class OaiPmhDigitalObject extends GenericDigitalObject {

    /**
     * Creates an OAI-PMH object
     * 
     * @param record the OAI-PMH record
     * @param metaId the metadata payload identifier
     */
    public OaiPmhDigitalObject(Record record, String metaId) {
        super(record.getHeader().getIdentifier(), metaId);
        addPayload(new OaiPmhPayload(metaId, record));
    }
}
