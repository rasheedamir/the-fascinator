/*
 * The Fascinator - Plugin API
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
package au.edu.usq.fascinator.api;

/**
 * Plugins wishing to describe themselves to the fascinator should do so using
 * this class.
 * 
 * @author Greg Pendlebury
 */
@SuppressWarnings("serial")
public class PluginDescription {
    /** Plugin id */
    private String plugin_id;

    /** Plugin name */
    private String plugin_name;

    /** Plugin metadata */
    private String plugin_metadata;

    /**
     * Plugin Description Constructor
     * 
     * @param p Plugin object
     */
    public PluginDescription(Plugin p) {
        plugin_id = p.getId();
        plugin_name = p.getName();
        plugin_metadata = null;
    }

    /**
     * Get Plugin Id
     * 
     * @return plugin id String
     */
    public String getId() {
        return plugin_id;
    }

    /**
     * Get Plugin name
     * 
     * @return plugin name String
     */
    public String getName() {
        return plugin_name;
    }

    /**
     * Get plugin metadata
     * 
     * @return plugin metadata String
     */
    public String getMetadata() {
        return plugin_metadata;
    }

    /**
     * Set plugin metadata
     * 
     * @param newMetadata metadata String
     */
    public void setMetadata(String newMetadata) {
        plugin_metadata = newMetadata;
    }
}
