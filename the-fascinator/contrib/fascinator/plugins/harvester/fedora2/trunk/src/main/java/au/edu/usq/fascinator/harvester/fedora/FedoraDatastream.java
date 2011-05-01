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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import au.edu.usq.fascinator.harvester.fedora.restclient.DatastreamType;
import au.edu.usq.fascinator.harvester.fedora.restclient.FedoraRestClient;

public class FedoraDatastream {

    private String pid;

    private FedoraRestClient client;

    private DatastreamType dsType;

    public FedoraDatastream(FedoraRestClient client, String pid,
        DatastreamType dsType) {
        this.client = client;
        this.pid = pid;
        this.dsType = dsType;
    }

    public String getId() {
        return dsType.getDsid();
    }

    public String getLabel() {
        return dsType.getLabel();
    }

    public String getMimeType() {
        return dsType.getMimeType();
    }

    public String getLocation() {
        return null;
    }

    public byte[] getContent() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        client.get(pid, getId(), out);
        return out.toByteArray();
    }

    public String getContentAsString() throws IOException {
        return new String(getContent());
    }

    public InputStream getContentAsStream() throws IOException {
        return new ByteArrayInputStream(getContent());
    }

    public Element getContentAsXml() throws IOException {
        Element elem = null;
        SAXReader reader = new SAXReader();
        try {
            Document doc = reader.read(getContentAsStream());
            elem = doc.getRootElement();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return elem;
    }

    public void getContent(File file) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        client.get(pid, getId(), out);
        out.close();
    }

    @Override
    public String toString() {
        return pid + "/" + getId() + " [" + getLabel() + "] (" + getMimeType()
            + ")";
    }
}