/* 
 * The Fascinator - Portal
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
package au.edu.usq.fascinator.portal.pages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;
import org.slf4j.Logger;

import au.edu.usq.fascinator.common.MimeTypeUtil;
import au.edu.usq.fascinator.portal.FormData;
import au.edu.usq.fascinator.portal.JsonSessionState;
import au.edu.usq.fascinator.portal.services.DynamicPageService;
import au.edu.usq.fascinator.portal.services.GenericStreamResponse;
import au.edu.usq.fascinator.portal.services.HttpStatusCodeResponse;

public class Dispatch {

    private static final String AJAX_EXT = ".ajax";

    private static final String DEFAULT_PORTAL_ID = "default";

    private static final String DEFAULT_RESOURCE = "home";

    @Inject
    private Logger log;

    @SessionState
    private JsonSessionState sessionState;

    @Inject
    private DynamicPageService pageService;

    @Inject
    private Request request;

    @Inject
    private Response response;

    @Persist
    private Map<String, FormData> formDataMap;

    public StreamResponse onActivate(Object... params) {
        log.trace("{} {}", request.getMethod(), request.getPath());

        // determine resource
        String portalId = (String) sessionState.get("portalId",
                DEFAULT_PORTAL_ID);
        String resourceName = DEFAULT_RESOURCE;

        String requestUri = request.getAttribute("RequestURI").toString();
        String[] path = requestUri.split("/");
        if (path.length > 1) {
            portalId = path[0].toString();
            resourceName = StringUtils.join(path, "/", 1, path.length);
        }
        String match = getBestMatchResource(portalId, resourceName);
        log.trace("resourceName = {}, match = {}", resourceName, match);
        if (match == null) {
            return new HttpStatusCodeResponse(404, "Page not found: "
                    + resourceName);
        }
        resourceName = match;
        boolean isAjax = resourceName.endsWith(AJAX_EXT);

        if (formDataMap == null) {
            formDataMap = Collections
                    .synchronizedMap(new HashMap<String, FormData>());
        }

        // save form data for POST requests, since we redirect after POSTs
        String requestId = request.getAttribute("RequestID").toString();
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            try {
                FormData formData = new FormData(request);
                formDataMap.put(requestId, formData);
                if (isAjax) {
                    response.setHeader("Cache-Control", "no-cache");
                    response.setDateHeader("Expires", 0);
                } else {
                    String redirectUri = resourceName;
                    if (path.length > 2) {
                        redirectUri += StringUtils.join(path, "/", 2,
                                path.length);
                    }
                    log.info("Redirecting to {}...", redirectUri);
                    response.sendRedirect(redirectUri);
                    return GenericStreamResponse.noResponse();
                }
            } catch (IOException ioe) {
                log.warn("Failed to redirect after POST", ioe);
            }
        }

        // render the page or retrieve the resource
        String mimeType;
        InputStream stream;

        if ((resourceName.indexOf(".") == -1) || isAjax) {
            FormData formData = formDataMap.get(requestId);
            if (formData == null) {
                formData = new FormData(request);
            }
            formDataMap.put(requestId, formData);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mimeType = pageService.render(portalId, resourceName, out,
                    formData, sessionState);
            stream = new ByteArrayInputStream(out.toByteArray());
        } else {
            mimeType = MimeTypeUtil.getMimeType(resourceName);
            stream = pageService.getResource(portalId, resourceName);
        }

        // clear formData
        formDataMap.remove(requestId);

        return new GenericStreamResponse(mimeType, stream);
    }

    public String getBestMatchResource(String portalId, String resourceName) {
        boolean isAjax = resourceName.endsWith(AJAX_EXT);
        if (isAjax) {
            resourceName = resourceName.substring(0, resourceName
                    .lastIndexOf(AJAX_EXT));
        }
        if (pageService.resourceExists(portalId, resourceName)) {
            return resourceName + (isAjax ? AJAX_EXT : "");
        }
        int slash = resourceName.lastIndexOf('/');
        if (slash != -1) {
            return getBestMatchResource(portalId, resourceName.substring(0,
                    slash));
        }
        return null;
    }
}
