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
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject;
import au.edu.usq.fedora.RestClient;
import au.edu.usq.fedora.types.DatastreamProfile;
import au.edu.usq.fedora.types.DatastreamType;
import au.edu.usq.fedora.types.ObjectDatastreamsType;

/**
 * Represent the DigitalObject in the Fedora repository
 * 
 * @author Oliver Lucido & Linda Octalina
 * 
 */
public class Fedora3DigitalObject extends GenericDigitalObject {

    /** Logger **/
    private Logger log = LoggerFactory.getLogger(Fedora3DigitalObject.class);

    /** API to talk to Fedora **/
    private RestClient client;

    /** fedoraId represent the DigitalObject stored in the Fedora Repository **/
    private String fedoraId;

    /**
     * Constructor method for Fedora3DigitalObject
     * 
     * @param client
     * @param fedoraId
     * @param oid
     */
    public Fedora3DigitalObject(RestClient client, String fedoraId, String oid) {
        super(oid);
        this.fedoraId = fedoraId;
        this.client = client;
    }

    /**
     * Return the payload list for the FedoraDigitaObject
     * 
     * @return List<Payload> of payload
     */
    @Override
    public List<Payload> getPayloadList() {
        List<Payload> dsList = new ArrayList<Payload>();
        try {
            ObjectDatastreamsType odt = client.listDatastreams(fedoraId);
            for (DatastreamType dst : odt.getDatastreams()) {
                Fedora3Payload ds = new Fedora3Payload(client, fedoraId, dst
                        .getDsid());

                DatastreamProfile dsProfile = client.getDatastream(fedoraId,
                        dst.getDsid());
                if (dsProfile.getDsAltID() != null) {
                    // AltID in Fedora store PayloadType and dsId in
                    // PayloadType:dsId format
                    // The dsId will be used to search the data stream in Fedora
                    String[] altId = dsProfile.getDsAltID().split(":");
                    String altId1 = URLDecoder.decode(altId[1], "UTF-8");
                    ds.setId(altId1);
                    ds.setLabel(dst.getLabel());
                    ds.setType(PayloadType.valueOf(altId[0]));
                    if (ds.getType().equals(PayloadType.Data)) {
                        // If it's a source, set the source id
                        setSourceId(altId1);
                    }
                    ds.setContentType(dst.getMimeType());
                    dsList.add(ds);
                }
            }
        } catch (IOException ioe) {
            log.error("Failed to list datastreams for {}", fedoraId);
        }
        return dsList;
    }
}
