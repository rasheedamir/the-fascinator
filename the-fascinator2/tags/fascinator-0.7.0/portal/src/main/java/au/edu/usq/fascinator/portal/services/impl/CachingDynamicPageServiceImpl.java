/* 
 * The Fascinator - Portal - Dynamic Page Service
 * Copyright (C) 2008-2010 University of Southern Queensland
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
package au.edu.usq.fascinator.portal.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.Response;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.python.core.Py;
import org.python.core.PyModule;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.core.imp;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.portal.FormData;
import au.edu.usq.fascinator.portal.JsonSessionState;
import au.edu.usq.fascinator.portal.guitoolkit.GUIToolkit;
import au.edu.usq.fascinator.portal.services.DynamicPageService;
import au.edu.usq.fascinator.portal.services.HouseKeepingManager;
import au.edu.usq.fascinator.portal.services.PortalManager;
import au.edu.usq.fascinator.portal.services.PortalSecurityManager;
import au.edu.usq.fascinator.portal.services.ScriptingServices;
import au.edu.usq.fascinator.portal.velocity.JythonLogger;

public class CachingDynamicPageServiceImpl implements DynamicPageService {

    private static final String CACHING_LEVEL_DATE = "dynamic";

    private static final String CACHING_LEVEL_FULL = "full";

    private static final String DEFAULT_LAYOUT_TEMPLATE = "layout";

    private static final String DEFAULT_SKIN = "default";

    private static final String DEFAULT_DISPLAY = "default";

    private static final String AJAX_EXT = ".ajax";

    private static final String SCRIPT_EXT = ".script";

    private Logger log = LoggerFactory
            .getLogger(CachingDynamicPageServiceImpl.class);

    private JsonConfig config;

    private String urlBase;

    @Inject
    private RequestGlobals requestGlobals;

    @Inject
    private Request request;

    @Inject
    private Response response;

    @Inject
    private ScriptingServices scriptingServices;

    @Inject
    private HouseKeepingManager houseKeeping;

    @Inject
    private PortalSecurityManager security;

    private String defaultPortal;

    private String defaultSkin;

    private String defaultDisplay;

    private List<String> skinPriority;

    private String layoutName;

    private String portalPath;

    private GUIToolkit toolkit;

    private HashMap<String, PyObject> scriptCache;

    private HashMap<String, Long> scriptCacheLastModified;

    private HashMap<String, String> skinCache;

    private HashMap<String, Long> skinCacheLastModified;

    private boolean cacheFull;

    private boolean cacheDate;

    public CachingDynamicPageServiceImpl() {
        try {
            config = new JsonConfig();
            urlBase = config.get("urlBase");
            layoutName = config.get("portal/layout", DEFAULT_LAYOUT_TEMPLATE);
            toolkit = new GUIToolkit();

            // Default templates
            defaultPortal = config.get("portal/defaultView",
                    PortalManager.DEFAULT_PORTAL_NAME);
            defaultSkin = config.get("portal/skins/default", DEFAULT_SKIN);
            defaultDisplay = config.get("portal/displays/default",
                    DEFAULT_DISPLAY);

            // Skin customisations - implement using resource loader logic?
            skinPriority = new ArrayList<String>();
            List<Object> skins = config.getList("portal/skins/order");
            for (Object object : skins) {
                skinPriority.add(object.toString());
            }
            if (!skinPriority.contains(defaultSkin)) {
                skinPriority.add(defaultSkin);
            }

            // Template directory
            String home = config.get("portal/home",
                    PortalManager.DEFAULT_PORTAL_HOME_DIR);
            File homePath = new File(home);
            if (!homePath.exists()) {
                home = PortalManager.DEFAULT_PORTAL_HOME_DIR_DEV;
                homePath = new File(home);
            }

            // setup velocity engine
            portalPath = homePath.getAbsolutePath();
            Properties props = new Properties();
            props.load(getClass().getResourceAsStream("/velocity.properties"));
            props.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, portalPath);
            Velocity.init(props);

            // Caching
            scriptCache = new HashMap<String, PyObject>();
            scriptCacheLastModified = new HashMap<String, Long>();
            skinCache = new HashMap<String, String>();
            skinCacheLastModified = new HashMap<String, Long>();
            String cacheLevel = config.get("portal/cachingLevel",
                    CACHING_LEVEL_DATE);
            cacheDate = false;
            cacheFull = false;
            if (cacheLevel.equals(CACHING_LEVEL_FULL)) {
                cacheDate = true;
                cacheFull = true;
                log.info("Full caching active...");
            } else {
                if (cacheLevel.equals(CACHING_LEVEL_DATE)) {
                    cacheDate = true;
                    cacheFull = false;
                    log.info("Dynamic caching active...");
                } else {
                    log.info("Caching disabled or invalid configuration! '{}'",
                            cacheLevel);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String resourceExists(String portalId, String resourceName) {
        return resourceExists(portalId, resourceName, true);
    }

    @Override
    public String resourceExists(String portalId, String resourceName,
            boolean fallback) {
        // Try the cache
        String path = getSkinPath(portalId, resourceName);
        if (path != null) {
            return path;
        }

        // Look through the skins of the specified portal
        path = testSkins(portalId, resourceName);
        if (path != null) {
            cacheSkinPath(portalId, resourceName, path);
            return path;
        }
        // Check if it's a display skin
        Pattern p = Pattern
                .compile("^(?:(.*)/)?display/(?:([a-zA-Z][^/]*))?/(.*)$");
        Matcher m = p.matcher(resourceName);
        if (m.matches()) {
            String displayType = m.group(2);
            if (!defaultDisplay.equals(displayType)) {
                String relPath = m.group(3);
                String fallbackResourceName = "display/" + defaultDisplay + "/"
                        + relPath;
                if (m.group(1) != null) {
                    fallbackResourceName = "scripts/" + fallbackResourceName;
                }
                path = testSkins(portalId, fallbackResourceName);
                if (path != null) {
                    cacheSkinPath(portalId, resourceName, path);
                    return path;
                }
            }
        }

        // Check if we can fall back to default portal
        if (fallback && !defaultPortal.equals(portalId)) {
            return resourceExists(defaultPortal, resourceName, false);
        }

        return null;
    }

    private String testSkins(String portalId, String resourceName) {
        String path = null;
        boolean noExt = resourceName.indexOf('.') == -1;
        // Loop through our skins
        for (String skin : skinPriority) {
            path = portalId + "/" + skin + "/" + resourceName;

            // Check raw resource
            if (Velocity.resourceExists(path)) {
                // But make sure it's not a directory, resourceExists()
                ///   will return directories as valid resources.
                File file = new File(portalPath, path);
                if (!file.isDirectory()) {
                    return path;
                }
            }
            // Look for templates and scripts if it had no extension
            if (noExt) {
                path = path + ".vm";
                if (Velocity.resourceExists(path)) {
                    return path;
                }
                path = portalId + "/" + skin + "/scripts/" + resourceName
                        + ".py";
                if (Velocity.resourceExists(path)) {
                    return path;
                }
            }
        }
        // We didn't find it
        return null;
    }

    @Override
    public InputStream getResource(String portalId, String resourceName) {
        return getResource(resourceExists(portalId, resourceName));
    }

    @Override
    public InputStream getResource(String resourcePath) {
        if (!Velocity.resourceExists(resourcePath)) {
            return null;
        }
        try {
            return RuntimeSingleton.getContent(resourcePath)
                    .getResourceLoader().getResourceStream(resourcePath);
        } catch (Exception e) {
            log.error("Failed to get resource: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String render(String portalId, String pageName, OutputStream out,
            FormData formData, JsonSessionState sessionState) {

        String mimeType = "text/html";
        boolean isAjax = pageName.endsWith(AJAX_EXT);
        if (isAjax) {
            pageName = pageName.substring(0, pageName.lastIndexOf(AJAX_EXT));
        }
        boolean isScript = pageName.endsWith(SCRIPT_EXT);
        if (isScript) {
            pageName = pageName.substring(0, pageName.lastIndexOf(SCRIPT_EXT));
        }
        Set<String> renderMessages = new HashSet<String>();

        // setup script and velocity context
        String contextPath = request.getContextPath();

        // TODO remove request/session based bindings once all scripts/templates
        // are using the cacheable format
        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("systemConfig", config);
        bindings.put("Services", scriptingServices);
        bindings.put("systemProperties", System.getProperties());
        bindings.put("request", request);
        bindings.put("response", response);
        bindings.put("formData", formData);
        bindings.put("sessionState", sessionState);
        bindings.put("security", security);
        bindings.put("contextPath", contextPath);
        bindings.put("scriptsPath", portalPath + "/" + portalId + "/scripts");
        bindings.put("portalDir", portalPath + "/" + portalId);
        bindings.put("portalId", portalId);
        bindings.put("portalPath", contextPath + "/" + portalId);
        bindings.put("defaultPortal", defaultPortal);
        bindings.put("pageName", pageName);
        bindings.put("responseOutput", out);
        bindings.put("urlBase", urlBase);
        bindings.put("serverPort", requestGlobals.getHTTPServletRequest()
                .getServerPort());
        bindings.put("toolkit", toolkit);
        bindings.put("log", log);
        bindings.put("notifications", houseKeeping.getUserMessages());
        bindings.put("bindings", bindings);

        // run page and template scripts
        PyObject layoutObject = new PyObject();
        String scriptName = "scripts/" + layoutName + ".py";
        try {
            layoutObject = evalScript(portalId, scriptName, bindings);
        } catch (Exception e) {
            ByteArrayOutputStream eOut = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(eOut));
            String eMsg = eOut.toString();
            log.warn("Failed to run layout script!\n=====\n{}\n=====", eMsg);
            renderMessages.add("Layout script error: " + scriptName + "\n"
                    + eMsg);
        }
        bindings.put("page", layoutObject);

        PyObject pageObject = new PyObject();
        try {
            scriptName = "scripts/" + pageName + ".py";
            pageObject = evalScript(portalId, scriptName, bindings);
        } catch (Exception e) {
            ByteArrayOutputStream eOut = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(eOut));
            String eMsg = eOut.toString();
            log.warn("Failed to run page script!\n=====\n{}\n=====", eMsg);
            renderMessages
                    .add("Page script error: " + scriptName + "\n" + eMsg);
        }
        bindings.put("self", pageObject);
        Object mimeTypeAttr = request.getAttribute("Content-Type");
        if (mimeTypeAttr != null) {
            mimeType = mimeTypeAttr.toString();
        }

        boolean committed = response.isCommitted();
        // log.debug("Response has been sent or redirected");

        if (!committed && resourceExists(portalId, pageName + ".vm") != null) {
            // set up the velocity context
            VelocityContext vc = new VelocityContext();
            for (String key : bindings.keySet()) {
                vc.put(key, bindings.get(key));
            }
            vc.put("velocityContext", vc);
            if (!renderMessages.isEmpty()) {
                vc.put("renderMessages", renderMessages);
            }

            try {
                // render the page content
                log.debug("Rendering page {}/{}.vm...", portalId, pageName);
                StringWriter pageContentWriter = new StringWriter();
                Template pageContent = getTemplate(portalId, pageName);
                pageContent.merge(vc, pageContentWriter);
                if (isAjax || isScript) {
                    out.write(pageContentWriter.toString().getBytes());
                } else {
                    vc.put("pageContent", pageContentWriter.toString());
                }
            } catch (Exception e) {
                ByteArrayOutputStream eOut = new ByteArrayOutputStream();
                e.printStackTrace(new PrintStream(eOut));
                String eMsg = eOut.toString();
                log.error("Failed to run page script ({})!\n=====\n{}\n=====",
                        isAjax ? "ajax" : isScript ? "script" : "html",
                        eMsg);
                String errorMsg = "<pre>Page content template error: "
                        + pageName + "\n" + eMsg + "</pre>";
                if (isAjax || isScript) {
                    try {
                        out.write(errorMsg.getBytes());
                    } catch (Exception e2) {
                        log.error("Failed to output error message!");
                    }
                } else {
                    vc.put("pageContent", errorMsg);
                }
            }

            if (!(isAjax || isScript)) {
                try {
                    // render the page using the layout template
                    log.debug("Rendering layout {}/{}.vm for page {}.vm...",
                            new Object[] { portalId, layoutName, pageName });
                    Template page = getTemplate(portalId, layoutName);
                    Writer pageWriter = new OutputStreamWriter(out, "UTF-8");
                    page.merge(vc, pageWriter);
                    pageWriter.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return mimeType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String renderObject(Context context, String template,
            JsonConfigHelper metadata) {
        //log.debug("========== START renderObject ==========");

        // setup script and velocity context
        String portalId = context.get("portalId").toString();
        String displayType = metadata.get("display_type", defaultDisplay);
        if ("".equals(displayType)) {
            displayType = defaultDisplay;
        }
        // On the detail page, check for a preview template too
        if (template.startsWith("detail")) {
            String previewType = metadata.get("preview_type");
            if (previewType != null && !"".equals(previewType)) {
                log.debug("Preview template found: '{}'", previewType);
                displayType = previewType;
            }
        }
        String templateName = "display/" + displayType + "/" + template;

        //log.debug("displayType: '{}'", displayType);
        //log.debug("templateName: '{}'", templateName);

        Object parentPageObject = null;
        Context objectContext = new VelocityContext(context);
        if (objectContext.containsKey("parent")) {
            parentPageObject = objectContext.get("parent");
        } else {
            parentPageObject = objectContext.get("self");
        }
        //log.debug("parentPageObject: '{}'", parentPageObject);

        objectContext.put("pageName", template);
        objectContext.put("displayType", displayType);
        objectContext.put("parent", parentPageObject);
        objectContext.put("metadata", metadata);

        // evaluate the context script if exists
        Object pageObject = new Object();
        String scriptName = "scripts/" + templateName + ".py";
        Set<String> renderMessages = null;
        if (objectContext.containsKey("renderMessages")) {
            renderMessages = (Set<String>) objectContext.get("renderMessages");
        } else {
            renderMessages = new HashSet<String>();
            context.put("renderMessages", renderMessages);
        }
        try {
            Map<String, Object> bindings = (Map<String, Object>) objectContext
                    .get("bindings");
            bindings.put("metadata", metadata);
            pageObject = evalScript(portalId, scriptName, bindings);
        } catch (Exception e) {
            ByteArrayOutputStream eOut = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(eOut));
            String eMsg = eOut.toString();
            log.warn("Failed to run display script!\n=====\n{}\n=====", eMsg);
            renderMessages
                    .add("Page script error: " + scriptName + "\n" + eMsg);
        }
        objectContext.put("self", pageObject);

        String content = "";
        try {
            // render the page content
            log.debug("Rendering display page {}/{}.vm...", portalId,
                    templateName);
            StringWriter pageContentWriter = new StringWriter();
            Template pageContent = getTemplate(portalId, templateName);
            pageContent.merge(objectContext, pageContentWriter);
            content = pageContentWriter.toString();
        } catch (Exception e) {
            log.error("Failed rendering display page: {}", templateName);
            ByteArrayOutputStream eOut = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(eOut));
            String eMsg = eOut.toString();
            renderMessages.add("Page content template error: " + templateName
                    + "\n" + eMsg);
        }

        //log.debug("========== END renderObject ==========");
        return content;
    }

    private PyObject evalScript(String portalId, String scriptName,
            Map<String, Object> bindings) {
        String path = resourceExists(portalId, scriptName);
        if (path == null) {
            log.debug("No script for portalId:'{}' scriptName:'{}'", portalId,
                    scriptName);
            return null;
        }
        PyObject scriptObject = null;
        if (cacheDate) {
            scriptObject = getPythonObject(path);
        }
        boolean useCache = scriptObject != null;
        if (scriptObject == null) {
            log.debug("Loading script '{}'", path);
            InputStream in = getResource(path);
            if (in != null) {
                // add current and default portal directories to python sys.path
                PySystemState sys = new PySystemState();
                addClassPaths(portalId, sys);
                Py.setSystemState(sys);
                PythonInterpreter python = new PythonInterpreter();
                // add virtual portal namespace - support context passing
                // between imported modules
                // need to add from __main__ import * to jython modules to
                // access the context
                PyModule mod = imp.addModule("__main__");
                python.setLocals(mod.__dict__);
                for (String key : bindings.keySet()) {
                    python.set(key, bindings.get(key));
                }
                JythonLogger jythonLogger = new JythonLogger(log, scriptName);
                python.setOut(jythonLogger);
                python.setErr(jythonLogger);
                python.execfile(in);
                scriptObject = python.get("scriptObject");
                if (scriptObject == null) {
                    useCache = true;
                    log.debug("isXHR:{}", request.isXHR());
                    String scriptClassName = StringUtils
                            .capitalize(FilenameUtils.getBaseName(scriptName))
                            + "Data";
                    PyObject scriptClass = python.get(scriptClassName);
                    log.debug("Instantiating object from class '{}'...",
                            scriptClassName);
                    if (scriptClass != null) {
                        scriptObject = scriptClass.__call__();
                        if (cacheDate) {
                            cachePythonObject(path, scriptObject);
                        }
                    }
                } else {
                    log.debug("DEPRECATED: script:'{}'", path);
                    // scriptObject = new PyObject();
                }
                python.cleanup();
            } else {
                log.debug("Failed to load script: '{}'", path);
            }
        }
        if (useCache && scriptObject != null) {
            // try {
            if (scriptObject.__findattr__("__activate__") != null) {
                //log.debug("Activating cached script:'{}'", path);
                scriptObject.invoke("__activate__", Py.java2py(bindings));
            } else {
                log.warn("__activate__ not found in '{}'", path);
            }
            // } catch (Exception e) {
            // ByteArrayOutputStream eOut = new ByteArrayOutputStream();
            // e.printStackTrace(new PrintStream(eOut));
            // String eMsg = eOut.toString();
            // log.warn("Failed to activate page!\n=====\n{}\n=====", eMsg);
            // }
        }
        return scriptObject;
    }

    private void addClassPaths(String portalId, PySystemState sys) {
        for (String skin : skinPriority) {
            sys.path.append(Py.newString(portalPath + "/" + portalId + "/"
                    + skin + "/scripts"));
        }
        if (!defaultPortal.equals(portalId)) {
            addClassPaths(defaultPortal, sys);
        }
    }

    private Template getTemplate(String portalId, String templateName)
            throws Exception {
        String path = resourceExists(portalId, templateName + ".vm");
        return Velocity.getTemplate(path);
    }

    private void cachePythonObject(String path, PyObject pyObject) {
        scriptCache.put(path, pyObject);
        // Only cache by modification date if full caching is not active
        if (!cacheFull) {
            scriptCacheLastModified.put(path, getLastModified(path));
        }
    }

    private void cacheSkinPath(String portalId, String resource, String path) {
        String index = portalId + "/" + resource;
        skinCache.put(index, path);
        //log.debug("Caching '{}' : '{}'", index, path);
        // Only cache by modification date if full caching is not active
        if (!cacheFull) {
            skinCacheLastModified.put(index, getLastModified(path));
        }
    }

    private PyObject getPythonObject(String path) {
        if (scriptCache.containsKey(path)) {
            // If full caching is not active
            if (!cacheFull) {
                // Also check the modification date
                Long lastCached = scriptCacheLastModified.get(path);
                if (lastCached != null && lastCached < getLastModified(path)) {
                    // expire the object
                    return null;
                }
            }
            return scriptCache.get(path);
        }
        return null;
    }

    private String getSkinPath(String portalId, String resource) {
        String index = portalId + "/" + resource;
        if (skinCache.containsKey(index)) {
            String path = skinCache.get(index);
            // If full caching is not active
            if (!cacheFull) {
                // Also check the modification date
                Long lastCached = skinCacheLastModified.get(index);
                if (lastCached != null && lastCached < getLastModified(path)) {
                    // expire the object
                    //log.debug("Expired '{}'", index);
                    return null;
                }
            }
            //log.debug("Cached '{}' : '{}'", index, path);
            return path;
        }
        return null;
    }

    private long getLastModified(String path) {
        File file = new File(portalPath, path);
        if (file.exists()) {
            //log.debug("Last modified '{}' : {}", file.getAbsolutePath(), file.lastModified());
            return file.lastModified();
        }
        return -1;
    }
}
