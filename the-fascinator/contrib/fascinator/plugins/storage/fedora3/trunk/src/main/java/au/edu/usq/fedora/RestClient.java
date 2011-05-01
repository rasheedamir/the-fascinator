/*
 * The Fascinator - Fedora Commons 3.x storage plugin
 * Copyright (C) 2009-2011 University of Southern Queensland
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
package au.edu.usq.fedora;

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
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.common.BasicHttpClient;
import au.edu.usq.fedora.types.DatastreamProfile;
import au.edu.usq.fedora.types.ObjectDatastreamsType;
import au.edu.usq.fedora.types.PidListType;
import au.edu.usq.fedora.types.ResultType;

/**
 * Fedora REST API client.
 * 
 * @author Oliver Lucido
 */
public class RestClient extends BasicHttpClient {

    /** Logger **/
    private Logger log = LoggerFactory.getLogger(RestClient.class);

    /**
     * RestClient Constructor
     * 
     * @param baseUrl
     */
    public RestClient(String baseUrl) {
        super(baseUrl);
    }

    /** Access methods (Fedora 2.2/3.0 compatible) */
    public enum FindObjectsType {
        QUERY("query"), TERMS("terms"), SESSION_TOKEN("sessionToken");

        private String name;

        FindObjectsType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    };

    /**
     * Find objects method
     * 
     * @param terms
     * @param maxResults
     * @return ResultType
     * @throws IOException
     */
    public ResultType findObjects(String terms, int maxResults)
            throws IOException {
        return findObjects(terms, maxResults, null);
    }

    /**
     * Find objects methods
     * 
     * @param terms
     * @param maxResults
     * @param fields
     * @return ResultType
     * @throws IOException
     */
    public ResultType findObjects(String terms, int maxResults, String[] fields)
            throws IOException {
        return findObjects(FindObjectsType.TERMS, terms, maxResults, fields);
    }

    /**
     * Find objects methods
     * 
     * @param type
     * @param queryOrTermsOrToken
     * @param maxResults
     * @return ResultType
     * @throws IOException
     */
    public ResultType findObjects(FindObjectsType type,
            String queryOrTermsOrToken, int maxResults) throws IOException {
        return findObjects(type, queryOrTermsOrToken, maxResults, null);
    }

    /**
     * Find objects methods
     * 
     * @param type
     * @param queryOrTermsOrToken
     * @param maxResults
     * @param fields
     * @return ResultType
     * @throws IOException
     */
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
            if (!type.equals(FindObjectsType.SESSION_TOKEN)) {
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

    /**
     * Resume find object
     * 
     * @param token
     * @return found token object
     * @throws IOException
     */
    public ResultType resumeFindObjects(String token) throws IOException {
        return findObjects(FindObjectsType.SESSION_TOKEN, token, 0, null);
    }

    /**
     * Resume find object
     * 
     * @param token
     * @param fields
     * @return found token object
     * @throws IOException
     */
    public ResultType resumeFindObjects(String token, String[] fields)
            throws IOException {
        return findObjects(FindObjectsType.SESSION_TOKEN, token, 0, fields);
    }

    /**
     * Get a list of datastream belong to the fedora id
     * 
     * @param pid
     * @return ObjectDatastreamsType
     * @throws IOException
     */
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
                JAXBContext jc = JAXBContext
                        .newInstance(ObjectDatastreamsType.class);
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

    /**
     * Get datastream from fedora
     * 
     * @param pid
     * @param dsId
     * @return DatastreamProfile
     * @throws IOException
     */
    public DatastreamProfile getDatastream(String pid, String dsId)
            throws IOException {
        DatastreamProfile result = null;
        // dsId = URLEncoder.encode(dsId, "UTF-8");
        try {
            StringBuilder uri = new StringBuilder(getBaseUrl());
            uri.append("/objects/");
            uri.append(pid);
            uri.append("/datastreams/");
            uri.append(dsId);
            uri.append("?format=xml");

            GetMethod method = new GetMethod(uri.toString());
            int status = executeMethod(method, true);
            if (status == 200) {
                JAXBContext jc = JAXBContext
                        .newInstance(DatastreamProfile.class);
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

    /**
     * Get the status if object exist in Fedora
     * 
     * @param pid
     * @param out
     * @return status of the get
     * @throws IOException
     */
    public int get(String pid, OutputStream out) throws IOException {
        return get(pid, null, out);
    }

    /**
     * Get the status if object exist in Fedora
     * 
     * @param pid
     * @param dsId
     * @param out
     * @return status of the get
     * @throws IOException
     */
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
            IOUtils.copy(in, out);
            in.close();
        }
        method.releaseConnection();
        return status;
    }

    /**
     * Get InputStream of a Datastream
     * 
     * @param pid
     * @param dsId
     * @return stream of the datastream
     * @throws IOException
     */
    public InputStream getStream(String pid, String dsId) throws IOException {
        StringBuilder uri = new StringBuilder(getBaseUrl());
        uri.append("/get/");
        uri.append(pid);
        if (dsId != null) {
            // dsId = URLEncoder.encode(dsId, "UTF-8");
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

    /**
     * Management methods (Fedora 3.0 only)
     * 
     * @return result
     * @throws IOException
     */
    public String getNextPid() throws IOException {
        return getNextPid(1, null).getPids().get(0);
    }

    /**
     * Get next fedora id
     * 
     * @param numPids
     * @param namespace
     * @return PidListType
     * @throws IOException
     */
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

    /**
     * Create new object in Fedora
     * 
     * @param label
     * @param namespace
     * @return created fedoraid
     * @throws IOException
     */
    public String createObject(String pid, String label, String namespace)
            throws IOException {
        Properties options = new Properties();
        options.setProperty("label", label);
        options.setProperty("namespace", namespace);
        ingest(pid, options, null);
        return pid;
    }

    public String createObject(String label, String namespace)
            throws IOException {
        String pid = namespace + ":" + UUID.randomUUID().toString();
        return createObject(pid, label, namespace);
    }

    /**
     * Ingest file to fedora repository
     * 
     * @param pid
     * @param label
     * @param content
     * @throws IOException
     */
    public void ingest(String pid, String label, File content)
            throws IOException {
        Properties options = new Properties();
        options.setProperty("label", label);
        ingest(pid, options, content);
    }

    /**
     * Ingest file to fedora repository
     * 
     * @param pid
     * @param options
     * @param content
     * @throws IOException
     */
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

    /**
     * Add new datastream to current fedora object
     * 
     * @param pid
     * @param dsId
     * @param dsLabel
     * @param contentType
     * @param altIds
     * @param content
     * @throws IOException
     */
    public void addDatastream(String pid, String dsId, String dsLabel,
            String contentType, String altIds, String content)
            throws IOException {
        Properties options = new Properties();
        options.setProperty("dsLabel", dsLabel);
        options.setProperty("altIDs", altIds);
        options.setProperty("controlGroup",
                "text/xml".equals(contentType) ? "X" : "M");
        RequestEntity request = new StringRequestEntity(content, contentType,
                "UTF-8");
        addDatastream(pid, dsId, options, contentType, request);
    }

    /**
     * Add new datastream to current fedora object
     * 
     * @param pid
     * @param dsId
     * @param dsLabel
     * @param contentType
     * @param altIds
     * @param content
     * @throws IOException
     */
    public void addDatastream(String pid, String dsId, String dsLabel,
            String contentType, String altIds, File content) throws IOException {
        Properties options = new Properties();
        options.setProperty("dsLabel", dsLabel);
        options.setProperty("altIDs", altIds);
        options.setProperty("controlGroup", "M");

        RequestEntity request = new FileRequestEntity(content, contentType);
        addDatastream(pid, dsId, options, contentType, request);
    }

    public void addDatastream(String pid, String dsId, String dsLabel,
            String contentType, String altIds, InputStream content)
            throws IOException {
        Properties options = new Properties();
        options.setProperty("dsLabel", dsLabel);
        options.setProperty("altIDs", altIds);
        options.setProperty("controlGroup", "M");
        RequestEntity request = new InputStreamRequestEntity(content,
                contentType);
        addDatastream(pid, dsId, options, contentType, request);
    }

    /**
     * Add new External datastream to current fedora object
     * 
     * @param pid
     * @param dsId
     * @param dsLabel
     * @param contentType
     * @param altIds
     * @param dsLocation
     * @throws IOException
     */
    public void addExternalDatastream(String pid, String dsId, String dsLabel,
            String contentType, String altIds, String dsLocation)
            throws IOException {
        Properties options = new Properties();
        options.setProperty("dsLabel", dsLabel);
        options.setProperty("altIDs", altIds);
        options.setProperty("controlGroup", "E");
        options.setProperty("dsLocation", dsLocation);
        RequestEntity request = new StringRequestEntity("", contentType,
                "UTF-8");
        addDatastream(pid, dsId, options, contentType, request);
    }

    /**
     * Add new datastream to current fedora object
     * 
     * @param pid
     * @param dsId
     * @param options
     * @param contentType
     * @param request
     * @throws IOException
     */
    private void addDatastream(String pid, String dsId, Properties options,
            String contentType, RequestEntity request) throws IOException {
        // dsId = URLEncoder.encode(dsId, "UTF-8");
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

    /**
     * Modify current datastream
     * 
     * @param pid
     * @param dsId
     * @param content
     * @throws IOException
     */
    public void modifyDatastream(String pid, String dsId, String content)
            throws IOException {
        // dsId = URLEncoder.encode(dsId, "UTF-8");
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

    public void modifyDatastream(String pid, String dsId, String dsLabel,
            String altIds, String contentType, File content) throws IOException {
        // dsId = URLEncoder.encode(dsId, "UTF-8");
        Properties options = new Properties();
        options.setProperty("dsLabel", dsLabel);
        options.setProperty("altIDs", altIds);
        StringBuilder uri = new StringBuilder(getBaseUrl());
        uri.append("/objects/");
        uri.append(pid);
        uri.append("/datastreams/");
        uri.append(dsId);
        addParam(uri, options, "dsLabel");
        addParam(uri, options, "altIDs");
        PutMethod method = new PutMethod(uri.toString());
        method.setRequestEntity(new FileRequestEntity(content, contentType));
        executeMethod(method);
        method.releaseConnection();
    }

    public void modifyDatastream(String pid, String dsId, Properties options)
            throws IOException {
        // dsId = URLEncoder.encode(dsId, "UTF-8");
        StringBuilder uri = new StringBuilder(getBaseUrl());
        uri.append("/objects/");
        uri.append(pid);
        uri.append("/datastreams/");
        uri.append(dsId);
        addParam(uri, options, "dsLocation");
        addParam(uri, options, "altIDs");
        addParam(uri, options, "dsLabel");
        addParam(uri, options, "versionable");
        addParam(uri, options, "dsState");
        addParam(uri, options, "formatURI");
        addParam(uri, options, "checksumType");
        addParam(uri, options, "checksum");
        addParam(uri, options, "mimeType");
        addParam(uri, options, "logMessage");
        addParam(uri, options, "ignoreContent");
        addParam(uri, options, "lastModifiedDate");
        PutMethod method = new PutMethod(uri.toString());
        executeMethod(method);
        method.releaseConnection();
    }

    /**
     * Purge datastream method
     * 
     * @param pid
     * @param dsId
     * @throws IOException
     */
    public void purgeDatastream(String pid, String dsId) throws IOException {
        StringBuilder uri = new StringBuilder(getBaseUrl());
        uri.append("/objects/");
        uri.append(pid);
        uri.append("/datastreams/");
        uri.append(dsId);
        DeleteMethod method = new DeleteMethod(uri.toString());
        executeMethod(method);
        method.releaseConnection();
    }

    /**
     * Purge fedora Digital object
     * 
     * @param pid
     * @throws IOException
     */
    public void purgeObject(String pid) throws IOException {
        purgeObject(pid, false);
    }

    /**
     * Purge fedora Digital object
     * 
     * @param pid
     * @param force
     * @throws IOException
     */
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

    /**
     * Export fedora Digital Object
     * 
     * @param pid
     * @param out
     * @throws IOException
     */
    public void export(String pid, OutputStream out) throws IOException {
        StringBuilder uri = new StringBuilder(getBaseUrl());
        uri.append("/objects/");
        uri.append(pid);
        uri.append("/export");
        GetMethod method = new GetMethod(uri.toString());
        int status = executeMethod(method);
        if (status == HttpStatus.SC_OK) {
            InputStream in = method.getResponseBodyAsStream();
            IOUtils.copy(in, out);
            in.close();
        } else {
            log.warn("GET {} returned {}", uri, status);
        }
        method.releaseConnection();
    }

    /**
     * Helper methods
     * 
     * @param uri
     * @param options
     * @param name
     * @throws IOException
     */
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

    /**
     * Get mimetype
     * 
     * @param file
     * @return the mimetype of the file
     */
    private String getMimeType(File file) {
        return MimetypesFileTypeMap.getDefaultFileTypeMap()
                .getContentType(file);
    }
}
