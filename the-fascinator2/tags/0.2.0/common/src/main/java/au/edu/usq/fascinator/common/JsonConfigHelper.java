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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.jxpath.AbstractFactory;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;
import org.apache.commons.lang.text.StrSubstitutor;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Helper class for working with JSON configuration. Uses the JXPath library to
 * use XPath syntax to access JSON nodes.
 * 
 * @author Oliver Lucido
 */
@SuppressWarnings("unchecked")
public class JsonConfigHelper {

    /** JXPath factory for creating JSON nodes */
    private class JsonMapFactory extends AbstractFactory {
        @Override
        public boolean createObject(JXPathContext context, Pointer pointer,
                Object parent, String name, int index) {
            if (parent instanceof Map) {
                ((Map<String, Object>) parent).put(name,
                        new HashMap<String, Object>());
                return true;
            }
            return false;
        }

    }

    /** JSON root node */
    private Map<String, Object> rootNode;

    /** JXPath context */
    private JXPathContext jxPath;

    /**
     * Creates an empty JSON configuration
     */
    public JsonConfigHelper() {
        rootNode = new HashMap<String, Object>();
    }

    /**
     * Creates a JSON configuration from a map. This is normally used to create
     * an instance for a subNode returned from one of the get methods.
     * 
     * @param rootNode a JSON structured map
     */
    public JsonConfigHelper(Map<String, Object> rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Creates a JSON configuration from the specified string
     * 
     * @param jsonContent a JSON content string
     * @throws IOException if there was an error parsing or reading the content
     */
    public JsonConfigHelper(String jsonContent) throws IOException {
        rootNode = new ObjectMapper().readValue(jsonContent, Map.class);
    }

    /**
     * Creates a JSON configuration from the specified file
     * 
     * @param jsonFile a JSON file
     * @throws IOException if there was an error parsing or reading the file
     */
    public JsonConfigHelper(File jsonFile) throws IOException {
        rootNode = new ObjectMapper().readValue(jsonFile, Map.class);
    }

    /**
     * Creates a JSON configuration from the specified input stream
     * 
     * @param jsonIn a JSON stream
     * @throws IOException if there was an error parsing or reading the stream
     */
    public JsonConfigHelper(InputStream jsonIn) throws IOException {
        rootNode = new ObjectMapper().readValue(jsonIn, Map.class);
    }

    /**
     * Gets a JXPath context for selecting and creating JSON nodes and values
     * 
     * @return a JXPath context
     */
    private JXPathContext getJXPath() {
        if (jxPath == null) {
            jxPath = JXPathContext.newContext(rootNode);
            jxPath.setFactory(new JsonMapFactory());
        }
        return jxPath;
    }

    /**
     * Gets the value of the specified node
     * 
     * @param path XPath to node
     * @return node value or null if not found
     */
    public String get(String path) {
        return get(path, null);
    }

    /**
     * Gets the value of the specified node, with a specified default if the not
     * was not found
     * 
     * @param path XPath to node
     * @param defaultValue value to return if the node was not found
     * @return node value or defaultValue if not found
     */
    public String get(String path, String defaultValue) {
        Object valueNode = null;
        try {
            valueNode = getJXPath().getValue(path);
        } catch (Exception e) {
        }
        String value = valueNode == null ? defaultValue : valueNode.toString();
        return StrSubstitutor.replaceSystemProperties(value);
    }

    /**
     * Gets values of the specified node as a list. Use this method for JSON
     * arrays.
     * 
     * @param path XPath to node
     * @return value list, possibly empty
     */
    public List<Object> getList(String path) {
        List<Object> valueList = new ArrayList<Object>();
        Iterator valueIterator = getJXPath().iterate(path);
        while (valueIterator.hasNext()) {
            Object value = valueIterator.next();
            valueList.add(value instanceof String ? StrSubstitutor
                    .replaceSystemProperties(value) : value);
        }
        return valueList;
    }

    /**
     * Gets a map of the child nodes of the specified node
     * 
     * @param path XPath to node
     * @return node map, possibly empty
     */
    public Map<String, Object> getMap(String path) {
        Map<String, Object> valueMap = new HashMap<String, Object>();
        Object valueNode = getJXPath().getValue(path);
        if (valueNode instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) valueNode;
            for (String key : map.keySet()) {
                valueMap.put(key, StrSubstitutor.replaceSystemProperties(map
                        .get(key)));
            }
        }
        return valueMap;
    }

    /**
     * Sets the value of the specified node. If the node doesn't exist it is
     * created.
     * 
     * @param path XPath to node
     * @param value value to set
     */
    public void set(String path, String value) {
        try {
            getJXPath().setValue(path, value);
        } catch (Exception e) {
            getJXPath().createPathAndSetValue(path, value);
        }
    }

    /**
     * Serialises the current state of the JSON configuration to the specified
     * writer. By default this doesn't use a pretty printer.
     * 
     * @param writer a writer
     * @throws IOException if there was an error writing the configuration
     */
    public void store(Writer writer) throws IOException {
        store(writer, false);
    }

    /**
     * Serialises the current state of the JSON configuration to the specified
     * writer. The output can be set to be pretty printed if required.
     * 
     * @param writer a writer
     * @param pretty use pretty printer
     * @throws IOException if there was an error writing the configuration
     */
    public void store(Writer writer, boolean pretty) throws IOException {
        JsonGenerator generator = new JsonFactory().createJsonGenerator(writer);
        if (pretty) {
            generator.useDefaultPrettyPrinter();
        }
        new ObjectMapper().writeValue(generator, rootNode);
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        try {
            store(sw, true);
        } catch (IOException e) {
        }
        return sw.toString();
    }
}
