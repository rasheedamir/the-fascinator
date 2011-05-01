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

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.annotations.Service;

import au.edu.usq.fascinator.api.authentication.AuthManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.roles.RolesManager;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.portal.services.DynamicPageService;
import au.edu.usq.fascinator.portal.services.HarvestManager;
import au.edu.usq.fascinator.portal.services.PortalManager;
import au.edu.usq.fascinator.portal.services.ScriptingServices;

public class ScriptingServicesImpl implements ScriptingServices {

    @Inject
    private DynamicPageService pageService;

    @Inject
    private AuthManager authManager;

    @Inject
    private Indexer indexerService;

    @Inject
    private RolesManager roleManager;

    @Inject
    private Storage storageService;

    @Inject
    private HarvestManager harvestManager;

    @Inject
    private PortalManager portalManager;

    @Override
    public DynamicPageService getPageService() {
        return pageService;
    }

    @Override
    public AuthManager getAuthManager() {
        return authManager;
    }

    @Override
    public Indexer getIndexer() {
        return indexerService;
    }

    @Override
    public RolesManager getRoleManager() {
        return roleManager;
    }

    @Override
    public Storage getStorage() {
        return storageService;
    }

    @Override
    public HarvestManager getHarvestManager() {
        return harvestManager;
    }

    @Override
    public PortalManager getPortalManager() {
        return portalManager;
    }
}
