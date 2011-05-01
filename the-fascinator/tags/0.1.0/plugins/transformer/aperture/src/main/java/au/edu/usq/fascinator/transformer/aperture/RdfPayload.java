/*
 * The Fascinator - Plugin - Transformer - Aperture
 * Copyright (C) 2009  University of Southern Queensland
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
package au.edu.usq.fascinator.transformer.aperture;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.ontoware.rdf2go.model.Syntax;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.common.storage.impl.GenericPayload;

/**
 * RdfPayload class to store the rdf xml returned by Aperture
 * 
 * @author Linda Octalina
 * @author Oliver Lucido
 */
public class RdfPayload extends GenericPayload {

    private static Logger log = LoggerFactory.getLogger(RdfPayload.class);

    private RDFContainer rdf;

    /**
     * RdfPayload Constructor
     * 
     * @param rdf as RDFContainer
     */
    public RdfPayload(RDFContainer rdf) {
        this.rdf = rdf;
        setId("rdf");
        setLabel("Aperture rdf");
        setContentType("application/xml+rdf");
        setType(PayloadType.Enrichment);
    }

    /**
     * getInputStream method
     * 
     * @return metadata content as InputStream
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(stripNonValidXMLCharacters().getBytes());
    }

    public String stripNonValidXMLCharacters() {
        String rdfString = rdf.getModel().serialize(Syntax.RdfXml).toString();

        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (rdfString == null || ("".equals(rdfString))) {
            return "";
        }
        for (int i = 0; i < rdfString.length(); i++) {
            current = rdfString.charAt(i);
            if ((current == 0x9) || (current == 0xA) || (current == 0xD)
                    || ((current >= 0x20) && (current <= 0xD7FF))
                    || ((current >= 0xE000) && (current <= 0xFFFD))
                    || ((current >= 0x10000) && (current <= 0x10FFFF))) {
                out.append(current);
            }
        }

        return out.toString();
    }
}
