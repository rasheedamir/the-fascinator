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
package au.edu.usq.fascinator.harvester.fedora.restclient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.UUID;
    
import javax.activation.MimetypesFileTypeMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;

import au.edu.usq.fascinator.common.BasicHttpClient;
import au.edu.usq.fascinator.harvester.fedora.utils.StreamUtils;

public class FedoraRestClient extends BasicHttpClient {

    private Logger log = Logger.getLogger(FedoraRestClient.class);

    public FedoraRestClient(String baseUrl) {
        super(baseUrl);
    }

    // Access methods (Fedora 2.2/3.0 compatible)

    public enum FindObjectsType {
        QUERY("query"), TERMS("terms"), SESSION_TOKEN("sessionToken");

        private String name;

        FindObjectsType(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    };

    public ResultType findObjects(String terms, int maxResults)
        throws IOException {
        return findObjects(terms, maxResults, null);
    }

    public ResultType findObjects(String terms, int maxResults, String[] fields)
        throws IOException {
        return findObjects(FindObjectsType.TERMS, terms, maxResults, fields);
    }

    public ResultType findObjects(FindObjectsType type,
        String queryOrTermsOrToken, int maxResults) throws IOException {
        return findObjects(type, queryOrTermsOrToken, maxResults, null);
    }

    public ResultType findObjects(FindObjectsType type,
        String queryOrTermsOrToken, int maxResults, String[] fields)
        throws IOException {
        ResultType result = null;
        try {
            StringBuilder uri = new StringBuilder(getBaseUrl());
            uri.append("/search?");
            uri.append(type);
            uri.append("=");
            uri.append(URLEncoder.encode(queryOrTermsOrToken, "UTF-8"));
            if (!type.equals(FindObjectsType.SESSION_TOKEN) && maxResults != -1) {
                uri.append("&maxResults=");
                uri.append(maxResults);
            }
            uri.append("&xml=true&pid=true");
            if (fields != null) {
                for (String field : fields) {
                    uri.append("&");
                    uri.append(field);
                    uri.append("=true");
                }
            }
            log.info("***** URI: " + uri.toString());
            GetMethod method = new GetMethod(uri.toString());
            int status = executeMethod(method);
            if (status == HttpStatus.SC_OK) {
                JAXBContext jc = JAXBContext.newInstance(ResultType.class);
                Unmarshaller um = jc.createUnmarshaller();
                InputStream in = method.getResponseBodyAsStream();
                result = (ResultType) um.unmarshal(in);
                in.close();
            }
            method.releaseConnection();
        } catch (JAXBException jaxbe) {
            log.error("Failed parsing result", jaxbe);
        }
        return result;
    }

    public ResultType resumeFindObjects(String token) throws IOException {
        return findObjects(FindObjectsType.SESSION_TOKEN, token, 0, null);
    }

    public ResultType resumeFindObjects(String token, String[] fields)
        throws IOException {
        return findObjects(FindObjectsType.SESSION_TOKEN, token, 0, fields);
    }

    public ObjectDatastreamsType listDatastreams(String pid) throws IOException {
        ObjectDatastreamsType result = null;
        try {
            StringBuilder uri = new StringBuilder(getBaseUrl());
            uri.append("/listDatastreams/");
            uri.append(pid);
            uri.append("?xml=true");
            GetMethod method = new GetMethod(uri.toString());
            int status = executeMethod(method);
            if (status == 200) {
                JAXBContext jc = JAXBContext.newInstance(ObjectDatastreamsType.class);
                Unmarshaller um = jc.createUnmarshaller();
                InputStream in = method.getResponseBodyAsStream();
                result = (ObjectDatastreamsType) um.unmarshal(in);
                in.close();
            }
            method.releaseConnection();
        } catch (JAXBException jaxbe) {
            log.error("Failed parsing result", jaxbe);
        }
        return result;
    }

    public DatastreamProfile getDatastream(String pid, String dsId)
        throws IOException {
        DatastreamProfile result = null;

        try {
            StringBuilder uri = new StringBuilder(getBaseUrl());
            uri.append("/objects/");
            uri.append(pid);
            uri.append("/datastreams/");
            uri.append(dsId);
            uri.append(".xml");

            GetMethod method = new GetMethod(uri.toString());
            int status = executeMethod(method, true);
            if (status == 200) {
                JAXBContext jc = JAXBContext.newInstance(DatastreamProfile.class);
                Unmarshaller um = jc.createUnmarshaller();
                InputStream in = method.getResponseBodyAsStream();

                result = (DatastreamProfile) um.unmarshal(in);
                in.close();
            }
            method.releaseConnection();
        } catch (JAXBException jaxbe) {
            log.error("Failed parsing result", jaxbe);
        }

        return result;
    }

    public int get(String pid, OutputStream out) throws IOException {
        return get(pid, null, out);
    }

    public int get(String pid, String dsId, OutputStream out)
        throws IOException {
        StringBuilder uri = new StringBuilder(getBaseUrl());
        uri.append("/get/");
        uri.append(pid);
        if (dsId != null) {
            uri.append("/");
            uri.append(dsId);
        }
        GetMethod method = new GetMethod(uri.toString());
        int status = executeMethod(method);
        if (status == HttpStatus.SC_OK) {
            InputStream in = method.getResponseBodyAsStream();
            StreamUtils.copyStream(in, out);
            in.close();
        }
        method.releaseConnection();
        return status;
    }

    public InputStream getStream(String pid, String dsId) throws IOException {
        StringBuilder uri = new StringBuilder(getBaseUrl());
        uri.append("/get/");
        uri.append(pid);
        if (dsId != null) {
            uri.append("/");
            uri.append(dsId);
        }
        GetMethod method = new GetMethod(uri.toString());
        int status = executeMethod(method);
        if (status == HttpStatus.SC_OK) {
            return method.getResponseBodyAsStream();
        }
        method.releaseConnection();
        return null;
    }

    // Management methods (Fedora 3.0 only)

    public String getNextPid() throws IOException {
        return getNextPid(1, null).getPids().get(0);
    }

    public PidListType getNextPid(int numPids, String namespace)
        throws IOException {
        PidListType result = null;
        try {
            StringBuilder uri = new StringBuilder(getBaseUrl());
            uri.append("/objects/nextPID?numPIDs=");
            uri.append(numPids);
            if (namespace != null) {
                uri.append("&namespace=");
                uri.append(namespace);
            }
            uri.append("&format=xml");
            GetMethod method = new GetMethod(uri.toString());
            int status = executeMethod(method);
            if (status == HttpStatus.SC_OK) {
                JAXBContext jc = JAXBContext.newInstance(PidListType.class);
                Unmarshaller um = jc.createUnmarshaller();
                InputStream in = method.getResponseBodyAsStream();
                result = (PidListType) um.unmarshal(in);
                in.close();
            }
            method.releaseConnection();
        } catch (JAXBException jaxbe) {
            log.error("Failed parsing result", jaxbe);
        }
        return result;
    }

    public String createObject(String label, String namespace)
        throws IOException {
        String pid = namespace + ":" + UUID.randomUUID().toString();
        Properties options = new Properties();
        options.setProperty("label", label);
        options.setProperty("namespace", namespace);
        ingest(pid, options, null);
        return pid;
    }

    public void ingest(String pid, String label, File content)
        throws IOException {
        Properties options = new Properties();
        options.setProperty("label", label);
        ingest(pid, options, content);
    }

    public void ingest(String pid, Properties options, File content)
        throws IOException {
        StringBuilder uri = new StringBuilder(getBaseUrl());
        uri.append("/objects/");
        uri.append(pid);
        addParam(uri, options, "label");
        addParam(uri, options, "format");
        addParam(uri, options, "encoding");
        addParam(uri, options, "namespace");
        addParam(uri, options, "ownerId");
        addParam(uri, options, "logMessage");
        PostMethod method = new PostMethod(uri.toString());
        RequestEntity request = null;
        if (content == null) {
            request = new StringRequestEntity("", "text/xml", "UTF-8");
        } else {
            String mimeType = options.getProperty("mimeType",
                getMimeType(content));
            request = new FileRequestEntity(content, mimeType);
        }
        method.setRequestEntity(request);
        executeMethod(method);
        method.releaseConnection();
    }

    public void addDatastream(String pid, String dsId, String dsLabel,
        String contentType, String content) throws IOException {
        Properties options = new Properties();
        options.setProperty("dsLabel", dsLabel);
        options.setProperty("controlGroup",
            "text/xml".equals(contentType) ? "X" : "M");
        RequestEntity request = new StringRequestEntity(content, contentType,
            "UTF-8");
        addDatastream(pid, dsId, options, contentType, request);
    }

    public void addDatastream(String pid, String dsId, String dsLabel,
        String contentType, File content) throws IOException {
        Properties options = new Properties();
        options.setProperty("dsLabel", dsLabel);
        options.setProperty("controlGroup", "M");
        RequestEntity request = new FileRequestEntity(content, contentType);
        addDatastream(pid, dsId, options, contentType, request);
    }

    public void addExternalDatastream(String pid, String dsId, String dsLabel,
        String contentType, String dsLocation) throws IOException {
        Properties options = new Properties();
        options.setProperty("dsLabel", dsLabel);
        options.setProperty("controlGroup", "E");
        options.setProperty("dsLocation", dsLocation);
        RequestEntity request = new StringRequestEntity("", contentType,
            "UTF-8");
        addDatastream(pid, dsId, options, contentType, request);
    }

    private void addDatastream(String pid, String dsId, Properties options,
        String contentType, RequestEntity request) throws IOException {
        StringBuilder uri = new StringBuilder(getBaseUrl());
        uri.append("/objects/");
        uri.append(pid);
        uri.append("/datastreams/");
        uri.append(dsId);
        addParam(uri, options, "controlGroup");
        addParam(uri, options, "dsLocation");
        addParam(uri, options, "altIDs");
        addParam(uri, options, "dsLabel");
        addParam(uri, options, "versionable");
        addParam(uri, options, "dsState");
        addParam(uri, options, "formatURI");
        addParam(uri, options, "checksumType");
        addParam(uri, options, "checksum");
        addParam(uri, options, "logMessage");
        PostMethod method = new PostMethod(uri.toString());
        method.setRequestEntity(request);
        method.setRequestHeader("Content-Type", contentType);
        executeMethod(method);
        method.releaseConnection();
    }

    public void modifyDatastream(String pid, String dsId, String content)
        throws IOException {
        StringBuilder uri = new StringBuilder(getBaseUrl());
        uri.append("/objects/");
        uri.append(pid);
        uri.append("/datastreams/");
        uri.append(dsId);
        PutMethod method = new PutMethod(uri.toString());
        method.setRequestEntity(new StringRequestEntity(content, "text/xml",
            "UTF-8"));
        executeMethod(method);
        method.releaseConnection();
    }

    public void purgeObject(String pid) throws IOException {
        purgeObject(pid, false);
    }

    public void purgeObject(String pid, boolean force) throws IOException {
        StringBuilder uri = new StringBuilder(getBaseUrl());
        uri.append("/objects/");
        uri.append(pid);
        uri.append("?force=");
        uri.append(force);
        DeleteMethod method = new DeleteMethod(uri.toString());
        executeMethod(method);
        method.releaseConnection();
    }

    public void export(String pid, OutputStream out) throws IOException {
        StringBuilder uri = new StringBuilder(getBaseUrl());
        uri.append("/objects/");
        uri.append(pid);
        uri.append("/export");
        GetMethod method = new GetMethod(uri.toString());
        int status = executeMethod(method);
        if (status == HttpStatus.SC_OK) {
            InputStream in = method.getResponseBodyAsStream();
            StreamUtils.copyStream(in, out);
            in.close();
        } else {
            log.warn("GET " + uri + " returned " + status);
        }
        method.releaseConnection();
    }

    // Helper methods

    private void addParam(StringBuilder uri, Properties options, String name)
        throws IOException {
        String value = options.getProperty(name);
        if (value != null) {
            char sep = '?';
            if (uri.lastIndexOf("?") > -1) {
                sep = '&';
            }
            uri.append(sep);
            uri.append(URLEncoder.encode(name, "UTF-8"));
            uri.append("=");
            uri.append(URLEncoder.encode(value, "UTF-8"));
        }
    }

    private String getMimeType(File file) {
        return MimetypesFileTypeMap.getDefaultFileTypeMap()
            .getContentType(file);
    }
}