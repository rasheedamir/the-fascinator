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
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.portal.Portal;
import au.edu.usq.fascinator.portal.services.PortalManager;

public class PortalManagerImpl implements PortalManager {

    private static final String PORTAL_XML = "portal.xml";

    private Logger log = LoggerFactory.getLogger(PortalManagerImpl.class);

    private Map<String, Portal> portals;

    private Map<String, Long> lastModified;

    private Map<String, File> portalFiles;

    private File portalsDir;

    private Marshaller jaxbM;

    private Unmarshaller jaxbU;

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
            JAXBContext ctx = JAXBContext.newInstance(Portal.class);
            jaxbM = ctx.createMarshaller();
            jaxbM.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbU = ctx.createUnmarshaller();
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
        Map<String, String> facetFields = portal.getFacetFields();
        if (!portalName.equals("default") && facetFields.isEmpty()) {
            facetFields.putAll(getDefault().getFacetFields());
        }
        getPortals().put(portalName, portal);
    }

    public void remove(String name) {
        // delete the portal.xml
        File portalDir = new File(portalsDir, name);
        File portalFile = new File(portalDir, PORTAL_XML);
        portalFile.delete();
        getPortals().remove(name);
    }

    public void save(Portal portal) {
        String portalName = portal.getName();
        File portalFile = new File(new File(portalsDir, portalName), PORTAL_XML);
        portalFile.getParentFile().mkdirs();
        try {
            jaxbM.marshal(portal, portalFile);
            log.info("Saved portal: " + portal);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        // TODO copy velocity templates
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
        File portalFile = new File(new File(portalsDir, name), PORTAL_XML);
        if (portalFile.exists()) {
            lastModified.put(name, portalFile.lastModified());
            portalFiles.put(name, portalFile);
            log.info(" *** portalFile:" + portalFile);
            try {
                portal = (Portal) jaxbU.unmarshal(portalFile);
                add(portal);
                log.info("Loaded portal: " + portal);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
        return portal;
    }
}
