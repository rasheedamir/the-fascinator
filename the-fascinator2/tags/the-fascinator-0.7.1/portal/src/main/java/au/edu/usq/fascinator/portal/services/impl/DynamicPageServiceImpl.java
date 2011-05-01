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
package au.edu.usq.fascinator.portal.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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
import org.python.core.PySystemState;
import org.python.core.imp;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class DynamicPageServiceImpl implements DynamicPageService {

    private static final String DEFAULT_LAYOUT_TEMPLATE = "layout";

    private static final String DEFAULT_SCRIPT_ENGINE = "python";

    private static final String DEFAULT_SKIN = "default";

    private static final String AJAX_EXT = ".ajax";

    private static final String SCRIPT_EXT = ".script";

    private Logger log = LoggerFactory.getLogger(DynamicPageServiceImpl.class);

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

    private List<String> skinPriority;

    private String layoutName;

    private boolean nativeJython;

    private String engineName;

    private String scriptsPath;

    private GUIToolkit toolkit;

    public DynamicPageServiceImpl() {
        try {
            JsonConfig config = new JsonConfig();
            nativeJython = Boolean.parseBoolean(config.get(
                    "portal/nativeJython", "true"));
            layoutName = config.get("portal/layout", DEFAULT_LAYOUT_TEMPLATE);
            engineName = config.get("portal/scriptEngine",
                    DEFAULT_SCRIPT_ENGINE);
            toolkit = new GUIToolkit();

            // Default templates
            defaultPortal = config.get("portal/defaultView",
                    PortalManager.DEFAULT_PORTAL_NAME);
            defaultSkin = config.get("portal/skins/default", DEFAULT_SKIN);

            // Skin customisations
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
            scriptsPath = homePath.getAbsolutePath();
            Properties props = new Properties();
            props.load(getClass().getResourceAsStream("/velocity.properties"));
            props.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, scriptsPath);
            Velocity.init(props);
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
        // log.debug(" * resourceExists:'{},{}'", portalId, resourceName);
        // Look through the skins of the specified portal
        String path = testSkins(portalId, resourceName);
        // log.debug(" ** testSkins:'{}'", path);
        if (path != null) {
            return path;
        }
        // Check if it's a display skin
        Pattern p = Pattern
                .compile("^(?:(.*)/)?display/(?:([a-z][^/]*))?/(.*)$");
        Matcher m = p.matcher(resourceName);
        // log.debug(" *** matches:'{}'", m.matches());
        if (m.matches()) {
            // log.debug(" **** m[1]:'{}'", m.group(1));
            // log.debug(" **** m[2]:'{}'", m.group(2));
            // log.debug(" **** m[3]:'{}'", m.group(3));

            String displayType = m.group(2);
            if (!"default".equals(displayType)) {
                String relPath = m.group(3);
                String fallbackResourceName = "display/default/" + relPath;
                if (m.group(1) != null) {
                    fallbackResourceName = "scripts/" + fallbackResourceName;
                }
                // log.debug(" **** fallbackResourceName:'{}'",
                // fallbackResourceName);
                path = testSkins(portalId, fallbackResourceName);
                // log.debug(" **** testSkins:'{}'", path);
                if (path != null) {
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
                return path;
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
        StringBuilder renderMessages = new StringBuilder();

        // setup script and velocity context
        String contextPath = request.getContextPath();

        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("Services", scriptingServices);
        bindings.put("systemProperties", System.getProperties());
        bindings.put("request", request);
        bindings.put("response", response);
        bindings.put("formData", formData);
        bindings.put("sessionState", sessionState);
        bindings.put("security", security);
        bindings.put("contextPath", contextPath);
        bindings.put("scriptsPath", scriptsPath + "/" + portalId + "/scripts");
        bindings.put("portalDir", scriptsPath + "/" + portalId);
        bindings.put("portalId", portalId);
        bindings.put("portalPath", contextPath + "/" + portalId);
        bindings.put("defaultPortal", defaultPortal);
        bindings.put("pageName", pageName);
        bindings.put("responseOutput", out);
        bindings.put("serverPort", requestGlobals.getHTTPServletRequest()
                .getServerPort());
        bindings.put("toolkit", toolkit);
        bindings.put("log", log);
        bindings.put("notifications", houseKeeping.getUserMessages());
        bindings.put("bindings", bindings);

        // run page and template scripts
        Object layoutObject = new Object();
        try {
            String scriptName = "scripts/" + layoutName + ".py";
            layoutObject = evalScript(portalId, scriptName, bindings);
        } catch (Exception e) {
            ByteArrayOutputStream eOut = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(eOut));
            String eMsg = eOut.toString();
            log.warn("Failed to run page script!\n=====\n{}\n=====", eMsg);
            renderMessages.append("Layout script error:\n");
            renderMessages.append(eMsg);
        }
        bindings.put("page", layoutObject);

        Object pageObject = new Object();
        try {
            String scriptName = "scripts/" + pageName + ".py";
            pageObject = evalScript(portalId, scriptName, bindings);
        } catch (Exception e) {
            ByteArrayOutputStream eOut = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(eOut));
            String eMsg = eOut.toString();
            log.warn("Failed to run page script!\n=====\n{}\n=====", eMsg);
            renderMessages.append("Page script error:\n");
            renderMessages.append(eMsg);
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
            if (renderMessages.length() > 0) {
                vc.put("renderMessages", renderMessages.toString());
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
                renderMessages.append("Page content template error:\n");
                renderMessages.append(e.getMessage());
                vc.put("renderMessages", renderMessages.toString());
                log.error("Failed rendering page: {}, {} ({})", new String[] {
                        pageName, e.getMessage(),
                        isAjax ? "ajax" : isScript ? "script" : "html" });
                log.error("Stack Track:\n", e);
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

    public String renderObject(Context context, String template,
            JsonConfigHelper metadata) {
        log.debug("========== START renderObject ==========");

        // setup script and velocity context
        String portalId = context.get("portalId").toString();

        String displayType = metadata.get("display_type", "default");
        if ("".equals(displayType)) {
            displayType = "default"; // TODO configurable
        }
        String templateName = "display/" + displayType + "/" + template;

        log.debug("displayType: '{}'", displayType);
        log.debug("templateName: '{}'", templateName);

        Object parentPageObject = null;
        if (context.containsKey("parent")) {
            parentPageObject = context.get("parent");
        } else {
            parentPageObject = context.get("self");
        }
        log.debug("parentPageObject: '{}'", parentPageObject);

        context.put("pageName", template);
        context.put("displayType", displayType);
        context.put("parent", parentPageObject);
        context.put("metadata", metadata);

        // evaluate the context script if exists
        Object pageObject = new Object();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> bindings = (Map<String, Object>) context
                    .get("bindings");
            bindings.put("metadata", metadata);
            String scriptName = "scripts/" + templateName + ".py";
            pageObject = evalScript(portalId, scriptName, bindings);
        } catch (Exception e) {
            ByteArrayOutputStream eOut = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(eOut));
            String eMsg = eOut.toString();
            log.warn("Failed to run display script!\n=====\n{}\n=====", eMsg);
        }
        context.put("self", pageObject);

        String content = "";
        try {
            // render the page content
            log.debug("Rendering display page {}/{}.vm...", portalId,
                    templateName);
            StringWriter pageContentWriter = new StringWriter();
            Template pageContent = getTemplate(portalId, templateName);
            pageContent.merge(context, pageContentWriter);
            content = pageContentWriter.toString();
        } catch (Exception e) {
            log.error("Failed rendering display page: {}", templateName);
            e.printStackTrace();
        }

        log.debug("========== END renderObject ==========");
        return content;
    }

    private Object evalScript(String portalId, String scriptName,
            Map<String, Object> bindings) throws ScriptException {
        Object scriptObject = new Object();
        log.debug("Running page script {}/{}...", portalId, scriptName);
        String path = resourceExists(portalId, scriptName);
        InputStream in = getResource(path);
        if (in != null) {
            if (nativeJython) {
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
                JythonLogger jythonLogger = new JythonLogger(
                        (Logger) bindings.get("log"), scriptName);
                python.setOut(jythonLogger);
                python.setErr(jythonLogger);
                python.execfile(in);
                scriptObject = python.get("scriptObject");
                python.cleanup();
            } else {
                ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
                ScriptEngine scriptEngine = scriptEngineManager
                        .getEngineByName(engineName);
                Bindings b = scriptEngine.createBindings();
                b.putAll(bindings);
                scriptEngine.setBindings(b, ScriptContext.ENGINE_SCOPE);
                scriptEngine.eval(new InputStreamReader(in));
                scriptObject = scriptEngine.get("scriptObject");
            }
        } else {
            log.warn("No script found for {}", scriptName);
        }
        return scriptObject;
    }

    private void addClassPaths(String portalId, PySystemState sys) {
        for (String skin : skinPriority) {
            sys.path.append(Py.newString(scriptsPath + "/" + portalId + "/"
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
}
