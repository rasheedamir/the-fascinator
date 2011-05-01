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

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.BackupClient;
import au.edu.usq.fascinator.HarvestClient;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.portal.Portal;
import au.edu.usq.fascinator.portal.services.PortalManager;
import au.edu.usq.fascinator.portal.services.ScriptingServices;

public class PortalManagerImpl implements PortalManager {

    private static final String PORTAL_JSON = "portal.json";

    private Logger log = LoggerFactory.getLogger(PortalManagerImpl.class);

    private Map<String, Portal> portals;

    private Map<String, Long> lastModified;

    private Map<String, File> portalFiles;

    private File portalsDir;

    @Inject
    private ScriptingServices services;

    public PortalManagerImpl() {
        try {
            JsonConfig config = new JsonConfig();
            String home = config.get("portal/home", DEFAULT_PORTAL_HOME_DIR);
            File homeDir = new File(home);
            if (!homeDir.exists()) {
                home = DEFAULT_PORTAL_HOME_DIR_DEV;
            }
            init(home);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void init(String portalsDir) {
        try {
            this.portalsDir = new File(portalsDir);
            lastModified = new HashMap<String, Long>();
            portalFiles = new HashMap<String, File>();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Portal> getPortals() {
        if (portals == null) {
            portals = new HashMap<String, Portal>();
            loadPortals();
        }
        return portals;
    }

    public Portal getDefault() {
        return get("default");
    }

    public Portal get(String name) {
        Portal portal = null;
        if (getPortals().containsKey(name)) {
            if (lastModified.containsKey(name)
                    && lastModified.get(name) < portalFiles.get(name)
                            .lastModified()) {
                loadPortal(name);
            }
            portal = getPortals().get(name);
        } else {
            portal = loadPortal(name);
        }
        return portal;
    }

    public void add(Portal portal) {
        String portalName = portal.getName();
        // log.info("PORTAL name: " + portalName);
        getPortals().put(portalName, portal);
    }

    public void remove(String name) {
        File portalDir = new File(portalsDir, name);
        File portalFile = new File(portalDir, PORTAL_JSON);
        portalFile.delete();
        getPortals().remove(name);
    }

    public void save(Portal portal) {
        String portalName = portal.getName();
        File portalFile = new File(new File(portalsDir, portalName),
                PORTAL_JSON);
        portalFile.getParentFile().mkdirs();
        try {
            FileWriter writer = new FileWriter(portalFile);
            portal.store(writer, true);
            writer.close();
        } catch (IOException ioe) {

        }
    }

    private void loadPortals() {
        File[] portalDirs = portalsDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                String name = file.getName();
                return file.isDirectory() && !name.equals(".svn");
            }
        });
        for (File dir : portalDirs) {
            loadPortal(dir.getName());
        }
    }

    private Portal loadPortal(String name) {
        Portal portal = null;
        File portalFile = new File(new File(portalsDir, name), PORTAL_JSON);
        if (portalFile.exists()) {
            lastModified.put(name, portalFile.lastModified());
            portalFiles.put(name, portalFile);
            try {
                portal = new Portal(portalFile);
                add(portal);
                // log.info("Loaded portal: " + portal);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return portal;
    }

    @Override
    public void backup(Portal portal) {
        BackupClient backupClient;
        try {
            File portalDir = new File(portalsDir, portal.getName());
            // log.info("****** " + portalDir);
            backupClient = new BackupClient(portalDir, portal.getBackupPaths(),
                    portal.getQuery());
            backupClient.run();
        } catch (IOException e) {
            log.error("Portal backup failed", e);
        }
    }

    @Override
    public void reharvest(String objectId) {
        try {
            HarvestClient client = new HarvestClient();
            client.reharvest(objectId);
        } catch (Exception e) {
            log.error("Object reharvest failed", e);
        }
    }

    @Override
    public void reharvest(Set<String> objectIds) {
        try {
            HarvestClient client = new HarvestClient();
            for (String oid : objectIds) {
                client.reharvest(oid);
            }
        } catch (Exception e) {
            log.error("Portal reharvest failed", e);
        }
    }
}
