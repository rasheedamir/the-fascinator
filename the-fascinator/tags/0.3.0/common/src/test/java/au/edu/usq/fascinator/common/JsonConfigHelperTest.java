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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for JsonConfigHelper
 * 
 * @author Oliver Lucido
 */
public class JsonConfigHelperTest {

    private JsonConfigHelper config;

    @Before
    public void setup() throws Exception {
        config = new JsonConfigHelper(getClass().getResourceAsStream(
                "/test-config.json"));
    }

    /**
     * Tests the get method
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void get() throws Exception {
        Assert.assertEquals("testing", config.get("test"));
        Assert.assertEquals("fedora3", config.get("storage/type"));
        Assert.assertEquals("http://localhost:8080/fedora", config
                .get("storage/config/uri"));
        Assert.assertEquals("http://localhost:8080/solr", config
                .get("indexer/config/uri"));
        Assert.assertEquals("true", config.get("indexer/config/autocommit"));
    }

    /**
     * Tests the get method with use of system properties
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void getWithSystemProperties() throws Exception {
        System.setProperty("sample.property", "Sample Value");
        Assert.assertEquals("Sample Value", config.get("sample/property"));
    }

    /**
     * Tests the getList method
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void getList() throws Exception {
        List expected1 = new ArrayList();
        expected1.add("one");
        expected1.add("two");
        expected1.add("three");

        Assert.assertEquals(expected1, config.getList("numbers"));
        List expected2 = new ArrayList();
        expected2.add("aperture");
        expected2.add("ice2");
        Assert.assertEquals(expected2, config.getList("transformer/conveyer"));

        System.setProperty("one", "1");
        List expected3 = new ArrayList();
        expected3.add("1");
        expected3.add(2);
        expected3.add(3);
        Assert.assertEquals(expected3, config.getList("transformer/ints"));
    }

    /**
     * Tests the getMap method
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void getMap() throws Exception {
        Map<String, String> expected1 = new HashMap<String, String>();
        expected1.put("uri", "http://localhost:8080/fedora");
        expected1.put("username", "fedoraAdmin");
        expected1.put("password", "fedoraAdmin");
        Assert.assertEquals(expected1, config.getMap("storage/config"));

        System.setProperty("solr.password", "solrAdmin");
        Map<String, String> expected2 = new HashMap<String, String>();
        expected2.put("uri", "http://localhost:8080/solr");
        expected2.put("username", "solrAdmin");
        expected2.put("password", "solrAdmin");
        expected2.put("autocommit", "true");
        Assert.assertEquals(expected2, config.getMap("indexer/config"));
    }

    /**
     * Tests the set method
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void set() throws Exception {
        config.set("new", "value");
        config.set("testing", "new value");
        config.set("hello/world", "!!!");
        config.set("storage/config/uri", "http://localhost:9000/fedora3");

        Assert.assertEquals("value", config.get("new"));
        Assert.assertEquals("new value", config.get("testing"));
        Assert.assertEquals("!!!", config.get("hello/world"));
        Assert.assertEquals("http://localhost:9000/fedora3", config
                .get("storage/config/uri"));
    }

    @Test
    public void getJson() throws Exception {
        Map<String, JsonConfigHelper> fields = config
                .getJsonMap("portal/facet-fields");
        config.setJsonMap("portal/facet-fields-clone", fields);
        // System.out.println(config.toString());
        // for (String key : fields.keySet()) {
        // System.out.println(key);
        // System.out.println(fields.get(key).get("label"));
        // }
    }

    @Test
    public void setMultiMap() throws Exception {
        Map<String, Object> storageMap = new HashMap<String, Object>();
        storageMap.put("type", "file-system");

        Map<String, Object> filesystem = new HashMap<String, Object>();
        filesystem.put("home", "/user");
        filesystem.put("use-link", "false");
        storageMap.put("file-system", filesystem);

        Map<String, Object> filesystem1 = new HashMap<String, Object>();
        filesystem1.put("more", "more");
        filesystem.put("filesystem1", filesystem1);

        Map<String, Object> filesystem2 = new HashMap<String, Object>();
        filesystem2.put("more2", "more2");
        filesystem1.put("filesystem2", filesystem2);

        config.setMultiMap("storage2", storageMap);

        // System.out.println("config:  " + config);
    }
}
