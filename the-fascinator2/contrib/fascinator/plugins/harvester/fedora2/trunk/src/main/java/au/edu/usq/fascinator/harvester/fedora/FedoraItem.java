/* 
 * The Fascinator - Solr Portal
 * Copyright (C) 2008  University of Southern Queensland
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
package au.edu.usq.fascinator.harvester.fedora;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import au.edu.usq.fascinator.harvester.fedora.restclient.DatastreamType;
import au.edu.usq.fascinator.harvester.fedora.restclient.FedoraRestClient;
import au.edu.usq.fascinator.harvester.fedora.restclient.ObjectDatastreamsType;

public class FedoraItem {

    private Logger log = Logger.getLogger(FedoraItem.class);

    private FedoraRestClient client;

    private String pid;

    private Map<String, String> nsMap;

    public FedoraItem(FedoraRestClient client, String pid) {
        this.client = client;
        this.pid = pid;
        nsMap = new HashMap<String, String>();
        nsMap.put("dc", "http://purl.org/dc/elements/1.1/");
        nsMap.put("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        nsMap.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        DocumentFactory.getInstance().setXPathNamespaceURIs(nsMap);
    }

    public String getId() {
        return pid;
    }

    public Element getMetadata() {
        Element elem = null;
        SAXReader reader = new SAXReader();
        try {
            String metadata = getMetadataAsString();
            Document doc = reader.read(new ByteArrayInputStream(
                metadata.getBytes("UTF-8")));
            elem = doc.getRootElement();
        } catch (UnsupportedEncodingException uee) {
            log.warn(uee);
        } catch (DocumentException de) {
            log.warn(de);
        }
        return elem;
    }

    public String getMetadataAsString() {
        String metadata = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            client.get(getPid(), getMetaId(), out);
            metadata = out.toString("UTF-8");
        } catch (IOException ioe) {
            log.warn(ioe);
        }
        return metadata;
    }

    public boolean hasDatastreams() {
        return true;
    }

    public List<FedoraDatastream> getDatastreams() {
        List<FedoraDatastream> streams = new ArrayList<FedoraDatastream>();
        try {
            ObjectDatastreamsType objectStreams = client.listDatastreams(getPid());
            for (DatastreamType ds : objectStreams.getDatastreams()) {
                streams.add(new FedoraDatastream(client, getPid(), ds));
            }
        } catch (IOException ioe) {
            log.warn(ioe);
        }
        return streams;
    }

    public FedoraDatastream getDatastream(String dsId) {
        List<FedoraDatastream> datastreams = getDatastreams();
        for (FedoraDatastream ds : datastreams) {
            if (dsId.equals(ds.getId())) {
                return ds;
            }
        }
        return null;
    }

    public void setNamespaceUri(String prefix, String uri) {
        nsMap.put(prefix, uri);
    }

    // Non-interface methods

    public String getPid() {
        return getId();
    }

    public String getMetaId() {
        return "DC";
    }
}