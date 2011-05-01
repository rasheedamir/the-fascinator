/* 
 * The Fascinator - Plugin - Harvester - File System
 * Copyright (C) 2009 University of Southern Queensland
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
package au.edu.usq.fascinator.harvester.filesystem;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.harvester.filesystem.FileSystemHarvester;

/**
 * Unit tests for the file system harvester plugin
 * 
 * @author Oliver Lucido
 */
public class FileSystemHarvesterTest {

    /**
     * Sets the "test.dir" system property for use in the JSON configuration
     * 
     * @throws Exception if any error occurs
     */
    @Before
    public void setup() throws Exception {
        File testDir = new File(getClass().getResource("/fs-harvest-root")
                .toURI());
        System.setProperty("test.dir", testDir.getAbsolutePath());
    }

    /**
     * Tests a non recurisve harvest
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void getObjects() throws Exception {
        FileSystemHarvester fsh = new FileSystemHarvester();
        fsh.init(getConfig("/fsh-config.json"));
        List<DigitalObject> items = fsh.getObjects();
        Assert.assertEquals(1, items.size());
        Assert.assertFalse(fsh.hasMoreObjects());
    }

    /**
     * Tests a recursive harvest
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void getObjectsRecursive() throws Exception {
        FileSystemHarvester fsh = new FileSystemHarvester();
        fsh.init(getConfig("/fsh-config-recursive.json"));
        List<DigitalObject> items = fsh.getObjects();
        Assert.assertTrue(fsh.hasMoreObjects());
        while (fsh.hasMoreObjects()) {
            items = fsh.getObjects();
            DigitalObject item = items.get(0);
            String id = item.getId();
            int expectedSize = 0;
            if (id.contains(FilenameUtils.separatorsToSystem("/books/"))) {
                expectedSize = 3;
            } else if (id.contains(FilenameUtils.separatorsToSystem("/music/"))) {
                expectedSize = 2;
            } else if (id.contains(FilenameUtils
                    .separatorsToSystem("/pictures/"))) {
                expectedSize = 4;
            }
            Assert.assertEquals(expectedSize, items.size());
        }
    }

    /**
     * Tests a recursive harvest while ignoring *.gif and *.mp3 files
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void getObjectsRecursiveIgnoreJpg() throws Exception {
        FileSystemHarvester fsh = new FileSystemHarvester();
        fsh.init(getConfig("/fsh-config-recursive-ignore.json"));
        List<DigitalObject> items = fsh.getObjects();
        Assert.assertTrue(fsh.hasMoreObjects());
        while (fsh.hasMoreObjects()) {
            items = fsh.getObjects();
            if (!items.isEmpty()) {
                DigitalObject item = items.get(0);
                String id = item.getId();
                int expectedSize = 0;
                if (id.contains(FilenameUtils.separatorsToSystem("/books/"))) {
                    expectedSize = 3;
                } else if (id.contains(FilenameUtils
                        .separatorsToSystem("/pictures/"))) {
                    // 1 gif file should be ignored
                    expectedSize = 3;
                }
                Assert.assertEquals(expectedSize, items.size());
            }
        }
    }

    private File getConfig(String filename) throws Exception {
        return new File(getClass().getResource(filename).toURI());
    }
}
