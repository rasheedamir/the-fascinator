/* 
 * The Fascinator - Common Library
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
package au.edu.usq.fascinator.common;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Commons HttpClient wrapper that makes it easier to work with proxies and
 * authentication
 * 
 * @author Oliver Lucido
 */
public class BasicHttpClient {

    /** Logging */
    private Logger log = LoggerFactory.getLogger(BasicHttpClient.class);

    /** Base URL for all requests */
    private String baseUrl;

    /** Authentication credentials */
    private UsernamePasswordCredentials credentials;

    /**
     * Creates an HTTP client for the specified base URL. The base URL is used
     * in determining proxy usage.
     * 
     * @param baseUrl the base URL
     */
    public BasicHttpClient(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.baseUrl = baseUrl;
    }

    /**
     * Sets the authentication credentials for authenticated requests
     * 
     * @param username a username
     * @param password a password
     */
    public void authenticate(String username, String password) {
        credentials = new UsernamePasswordCredentials(username, password);
    }

    /**
     * Gets an HTTP client. If authentication is required, the authenticate()
     * method must be called prior to this method.
     * 
     * @param auth set true to use authentication, false to skip authentication
     * @return an HTTP client
     */
    public HttpClient getHttpClient(boolean auth) {
        HttpClient client = new HttpClient();
        try {
            URL url = new URL(baseUrl);
            if (!isNonProxyHost(url.getHost())) {
                String proxyHost = System.getProperty("http.proxyHost");
                String proxyPort = System.getProperty("http.proxyPort");
                if (proxyHost != null && !"".equals(proxyHost)) {
                    client.getHostConfiguration().setProxy(proxyHost,
                            Integer.parseInt(proxyPort));
                    log.trace("Using proxy {}:{}", proxyHost, proxyPort);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get proxy settings: " + e.getMessage());
        }
        if (auth && credentials != null) {
            client.getParams().setAuthenticationPreemptive(true);
            client.getState().setCredentials(AuthScope.ANY, credentials);
            log.trace("Credentials: username={}", credentials.getUserName());
        }
        return client;
    }

    /**
     * Gets the base URL
     * 
     * @return the base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Sends an HTTP request. This method uses authentication for all requests
     * except GET.
     * 
     * @param method an HTTP method
     * @return HTTP status code
     * @throws IOException if an error occurred during the HTTP request
     */
    public int executeMethod(HttpMethodBase method) throws IOException {
        boolean auth = !(method instanceof GetMethod);
        return executeMethod(method, auth);
    }

    /**
     * Sends an HTTP request
     * 
     * @param method an HTTP method
     * @param auth true to request with authentication, false to request without
     * @return HTTP status code
     * @throws IOException if an error occurred during the HTTP request
     */
    public int executeMethod(HttpMethodBase method, boolean auth)
            throws IOException {
        log.trace("{} {}", method.getName(), method.getURI());
        int status = getHttpClient(auth).executeMethod(method);
        log.trace("{} {}", status, HttpStatus.getStatusText(status));
        return status;
    }

    /**
     * Tests whether a host should bypass the currently set proxy
     * 
     * @param host the host to test
     * @return true if the host should bypass the proxy, false otherwise
     */
    private boolean isNonProxyHost(String host) {
        String httpNonProxyHosts = System.getProperty("http.nonProxyHosts",
                "localhost|127.0.0.1");
        String[] nonProxyHosts = httpNonProxyHosts.split("\\|");
        for (int i = 0; i < nonProxyHosts.length; ++i) {
            if (nonProxyHosts[i].startsWith("*")) {
                if (host.endsWith(nonProxyHosts[i].substring(1))) {
                    return true;
                }
            } else if (host.equals(nonProxyHosts[i])) {
                return true;
            }
        }
        return false;
    }
}
