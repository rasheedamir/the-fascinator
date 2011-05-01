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
package au.edu.usq.fascinator.portal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.common.JsonConfigHelper;

/**
 * Portal configuration
 * 
 * @author Linda Octalina
 */
public class Portal extends JsonConfigHelper {

    public static final String PORTAL_JSON = "portal.json";

    private Logger log = LoggerFactory.getLogger(Portal.class);

    /**
     * Construct a portal instance with the specified name
     */
    public Portal(String portalName) {
        setName(portalName);
    }

    /**
     * Construct a portal instance for the specified JSON file
     * 
     * @throws IOException if there was an error reading the JSON file
     */
    public Portal(File portalConfig) throws IOException {
        super(portalConfig);
    }

    public String getName() {
        return get("portal/name", "undefined");
    }

    public void setName(String name) {
        set("portal/name", name.replace(' ', '_'));
    }

    public String getDescription() {
        return get("portal/description", "Undefined");
    }

    public void setDescription(String description) {
        set("portal/description", description);
    }

    public String getQuery() {
        return get("portal/query", "");
    }

    public void setQuery(String query) {
        set("portal/query", query);
    }

    public String getSearchQuery() {
        return get("portal/searchQuery", "");
    }

    public void setSearchQuery(String query) {
        set("portal/searchQuery", query);
    }

    public int getRecordsPerPage() {
        return Integer.parseInt(get("portal/records-per-page", "10"));
    }

    public void setRecordsPerPage(int recordsPerPage) {
        set("portal/records-per-page", Integer.toString(recordsPerPage));
    }

    public int getFacetCount() {
        return Integer.parseInt(get("portal/facet-count", "25"));
    }

    public void setFacetCount(int facetCount) {
        set("portal/facet-count", Integer.toString(facetCount));
    }

    public boolean getFacetSort() {
        return Boolean.parseBoolean(get("portal/facet-sort-by-count", "false"));
    }

    public void setFacetSort(boolean facetSort) {
        set("portal/facet-sort-by-count", Boolean.toString(facetSort));
    }

    public Map<String, JsonConfigHelper> getFacetFields() {
        return getJsonMap("portal/facet-fields");
    }

    public void setFacetFields(Map<String, JsonConfigHelper> map) {
        setJsonMap("portal/facet-fields", map);
    }

    public List<String> getFacetFieldList() {
        return new ArrayList<String>(getFacetFields().keySet());
    }

    public int getFacetDisplay() {
        return Integer.parseInt(get("portal/facet-display", "10"));
    }

    public void setFacetDisplay(int facetDisplay) {
        set("portal/facet-display", Integer.toString(facetDisplay));
    }

    public Map<String, String> getSortFields() {
        Map<String, Object> objectFields = getMap("portal/sort-fields");
        Map<String, String> sortFields = new LinkedHashMap<String, String>();
        for (String key : objectFields.keySet()) {
            sortFields.put(key, objectFields.get(key).toString());
        }
        return sortFields;
    }

    public void setSortFields(Map<String, String> sortFields) {
        Map<String, Object> objectFields = new LinkedHashMap<String, Object>();
        objectFields.putAll(sortFields);
        setMap("portal/sort-fields", objectFields);
    }

    public List<String> getSortFieldList() {
        return new ArrayList<String>(getSortFields().keySet());
    }
}
