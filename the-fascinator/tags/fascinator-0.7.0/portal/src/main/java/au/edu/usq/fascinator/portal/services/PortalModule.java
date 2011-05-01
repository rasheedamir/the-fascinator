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
package au.edu.usq.fascinator.portal.services;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.RegistryShutdownHub;
import org.apache.tapestry5.services.AliasContribution;
import org.apache.tapestry5.services.ApplicationStateContribution;
import org.apache.tapestry5.services.ApplicationStateCreator;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.URLEncoder;
import org.apache.tapestry5.urlrewriter.RewriteRuleApplicability;
import org.apache.tapestry5.urlrewriter.SimpleRequestWrapper;
import org.apache.tapestry5.urlrewriter.URLRewriteContext;
import org.apache.tapestry5.urlrewriter.URLRewriterRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import au.edu.usq.fascinator.AccessManager;
import au.edu.usq.fascinator.AuthenticationManager;
import au.edu.usq.fascinator.RoleManager;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.access.AccessControlManager;
import au.edu.usq.fascinator.api.authentication.AuthManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.roles.RolesManager;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.portal.JsonSessionState;
import au.edu.usq.fascinator.portal.services.impl.CachingDynamicPageServiceImpl;
import au.edu.usq.fascinator.portal.services.impl.DatabaseServicesImpl;
import au.edu.usq.fascinator.portal.services.impl.HarvestManagerImpl;
import au.edu.usq.fascinator.portal.services.impl.HouseKeepingManagerImpl;
import au.edu.usq.fascinator.portal.services.impl.PortalManagerImpl;
import au.edu.usq.fascinator.portal.services.impl.PortalSecurityManagerImpl;
import au.edu.usq.fascinator.portal.services.impl.ScriptingServicesImpl;

public class PortalModule {

    private static final String DEFAULT_INDEXER_TYPE = "solr";

    private static final String DEFAULT_STORAGE_TYPE = "file-system";

    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(PortalModule.class);

    static {
        MDC.put("name", "main");
    }

    public static void bind(ServiceBinder binder) {
        binder.bind(HarvestManager.class, HarvestManagerImpl.class);
        binder.bind(DynamicPageService.class,
                CachingDynamicPageServiceImpl.class);
        binder.bind(PortalManager.class, PortalManagerImpl.class);
        binder.bind(ScriptingServices.class, ScriptingServicesImpl.class);
        binder.bind(PortalSecurityManager.class,
                PortalSecurityManagerImpl.class);
    }

    public static DatabaseServices buildDatabaseServices(RegistryShutdownHub hub) {
        DatabaseServices database = new DatabaseServicesImpl();
        hub.addRegistryShutdownListener(database);
        return database;
    }

    public static HouseKeepingManager buildHouseKeepingManager(
            RegistryShutdownHub hub) {
        HouseKeepingManager houseKeeping = new HouseKeepingManagerImpl();
        hub.addRegistryShutdownListener(houseKeeping);
        return houseKeeping;
    }

    public static AccessControlManager buildAccessManager() {
        try {
            AccessManager access = new AccessManager();
            access.init(JsonConfig.getSystemFile());
            return access;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static AuthManager buildAuthManager() {
        try {
            AuthenticationManager auth = new AuthenticationManager();
            auth.init(JsonConfig.getSystemFile());
            return auth;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Indexer buildIndexer() {
        try {
            JsonConfig config = new JsonConfig();
            Indexer indexer = PluginManager.getIndexer(config.get(
                    "indexer/type", DEFAULT_INDEXER_TYPE));
            indexer.init(JsonConfig.getSystemFile());
            return indexer;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static RolesManager buildRoleManager() {
        try {
            RoleManager roles = new RoleManager();
            roles.init(JsonConfig.getSystemFile());
            return roles;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Storage buildStorage() {
        try {
            JsonConfig config = new JsonConfig();
            Storage storage = PluginManager.getStorage(config.get(
                    "storage/type", DEFAULT_STORAGE_TYPE));
            storage.init(JsonConfig.getSystemFile());
            return storage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void contributeApplicationStateManager(
            MappedConfiguration<Class<?>, ApplicationStateContribution> configuration) {
        ApplicationStateCreator<JsonSessionState> creator = new ApplicationStateCreator<JsonSessionState>() {
            @Override
            public JsonSessionState create() {
                return new JsonSessionState();
            }
        };
        ApplicationStateContribution contribution = new ApplicationStateContribution(
                "session", creator);
        configuration.add(JsonSessionState.class, contribution);
    }

    public static void contributeAlias(
            Configuration<AliasContribution<URLEncoder>> configuration) {
        configuration.add(AliasContribution.create(URLEncoder.class,
                new NullURLEncoderImpl()));
    }

    public static void contributeURLRewriter(
            OrderedConfiguration<URLRewriterRule> configuration,
            @Inject final RequestGlobals requestGlobals,
            @Inject final URLEncoder urlEncoder) {
        URLRewriterRule rule = new URLRewriterRule() {
            @Override
            public Request process(Request request, URLRewriteContext context) {
                // set the original request uri - without context
                HttpServletRequest req = requestGlobals.getHTTPServletRequest();
                String ctxPath = request.getContextPath();
                String uri = req.getRequestURI();
                request.setAttribute("RequestURI",
                        uri.substring(ctxPath.length() + 1));
                request.setAttribute("RequestID",
                        DigestUtils.md5Hex(uri + req.getQueryString()));

                // forward all requests to the main dispatcher
                String path = request.getPath();
                String[] parts = path.substring(1).split("/");
                if (parts.length > 0) {
                    String start = parts[0];
                    if (!"assets".equals(start) && !"dispatch".equals(start)) {
                        path = "/dispatch" + path;
                    }
                } else {
                    path = "/dispatch";
                }
                return new SimpleRequestWrapper(request, path);
            }

            @Override
            public RewriteRuleApplicability applicability() {
                return RewriteRuleApplicability.INBOUND;
            }
        };
        configuration.add("dispatch", rule);
    }
}
