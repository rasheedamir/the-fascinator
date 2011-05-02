/* 
 * The Fascinator - Plugin - Harvester - SKOS
 * Copyright (C) 2010-2011 University of Southern Queensland
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
package au.edu.usq.fascinator.harvester.skos;

import java.io.File;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.harvester.Harvester;
import au.edu.usq.fascinator.api.storage.Storage;

/**
 * Unit tests for the SKOS harvester plugin
 * 
 * @author Linda Octalina
 */
public class SkosHarvesterTest {

    /** logging */
    private Logger log = LoggerFactory.getLogger(SkosHarvesterTest.class);

    private Storage ram;

    /**
     * Sets the "test.dir" and "test.cache.dir" system property for use in the
     * JSON configuration
     * 
     * @throws Exception if any error occurs
     */
    @Before
    public void setup() throws Exception {
        File baseDir = new File(SkosHarvesterTest.class.getResource("/")
                .toURI());
        System.setProperty("test.dir", baseDir.getAbsolutePath());
        
        ram = PluginManager.getStorage("ram");
        ram.init("{}");
    }

    /**
     * Test on getting concept scheme uri
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void getConceptScheme() throws Exception {
        SkosHarvester skosHarvester = getHarvester("/for-subset.json");
        Assert.assertEquals(skosHarvester.getConceptScheme().toString(),
                "http://purl.org/anzsrc/for/#for");
    }

    @Test
    public void getConceptScheme2() throws Exception {
        SkosHarvester skosHarvester = getHarvester("/for08-subset.json");
        Assert.assertEquals(skosHarvester.getConceptScheme().toString(),
                "http://purl.org/asc/1297.0/2008/for/");
    }

    /**
     * Test on getting object list
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void getObjectIdList() throws Exception {
        SkosHarvester skosHarvester = getHarvester("/for-subset.json");
        Set<String> concepts = skosHarvester.getObjectIdList();
        Assert.assertEquals(15, concepts.size());
    }

    @Test
    public void getObjectIdList2() throws Exception {
        SkosHarvester skosHarvester = getHarvester("/for08-subset.json");
        Set<String> concepts = skosHarvester.getObjectIdList();
        Assert.assertEquals(99, concepts.size());
    }

    private SkosHarvester getHarvester(String filename) throws Exception {
        Harvester skos = PluginManager.getHarvester("skos", ram);
        File f = new File(getClass().getResource(filename).toURI());
        skos.init(f);
        return (SkosHarvester) skos;
    }
}
