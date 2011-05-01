/* 
 * The Fascinator - Solr Indexer Plugin
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
package au.edu.usq.fascinator.indexer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.common.BasicHttpClient;

public class SolrSearcher {

    public static String QUERY_ALL = "*:*";

    private Logger log = LoggerFactory.getLogger(SolrSearcher.class);

    private BasicHttpClient client;

    private String baseUrl;

    public SolrSearcher(String solrBaseUrl) {
        this.baseUrl = solrBaseUrl;
        client = new BasicHttpClient(solrBaseUrl);
    }

    public void authenticate(String username, String password) {
        client.authenticate(username, password);
    }

    public InputStream get() throws IOException {
        return get(null);
    }

    public InputStream get(String query) throws IOException {
        return get(query, true);
    }

    public InputStream get(String query, boolean escape) throws IOException {
        return get(query, null, escape);
    }

    public InputStream get(String query, String extras, boolean escape)
            throws IOException {
        if (query == null) {
            query = "*:*";
        } else if (!QUERY_ALL.equals(query) && escape) {
            query = query.replaceAll(":", "\\\\:");
        }
        String queryUrl = getUrl(query, extras);
        log.debug("Query URL: " + queryUrl);
        GetMethod method = new GetMethod(queryUrl);
        int status = client.executeMethod(method, true);
        if (status == HttpStatus.SC_OK) {
            return method.getResponseBodyAsStream();
        }
        return null;
    }

    private String getUrl(String query) throws UnsupportedEncodingException {
        StringBuilder url = new StringBuilder(baseUrl);
        url.append("/select?q=");
        url.append(URLEncoder.encode(query, "UTF-8"));
        return url.toString();
    }

    private String getUrl(String query, String extras)
            throws UnsupportedEncodingException {
        String url = getUrl(query);
        if (!"".equals(extras) && extras != null) {
            int qmark = url.indexOf('?') + 1;
            url = url.substring(0, qmark) + extras + "&" + url.substring(qmark);
        }
        return url;
    }
}
