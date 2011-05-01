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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import au.edu.usq.fascinator.api.storage.DigitalObject;

/**
 * Unit tests for the file system harvester plugin
 * 
 * @author Oliver Lucido
 */
public class FileSystemHarvesterTest {

    private File testDir;

    private File cacheDir;

    private File testFile;

    /**
     * Sets the "test.dir" and "test.cache.dir" system property for use in the
     * JSON configuration
     * 
     * @throws Exception if any error occurs
     */
    @Before
    public void setup() throws Exception {
        File baseDir = new File(getClass().getResource("/").toURI());
        testDir = new File(baseDir, "fs-harvest-root");
        cacheDir = new File(baseDir, "fs-harvest-cache");
        cacheDir.mkdirs();
        System.setProperty("test.dir", testDir.getAbsolutePath());
        System.setProperty("test.cache.dir", cacheDir.getAbsolutePath());
        testFile = new File(testDir, "test-gen.txt");
        FileUtils.writeStringToFile(testFile, "testing");
    }

    @After
    public void cleanup() throws Exception {
        FileUtils.deleteQuietly(testFile);
        FileUtils.deleteQuietly(cacheDir);
    }

    /**
     * Tests a non recursive harvest
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void getObjects() throws Exception {
        FileSystemHarvester fsh = new FileSystemHarvester();
        fsh.init(getConfig("/fsh-config.json"));
        List<DigitalObject> items = fsh.getObjects();
        Assert.assertEquals(2, items.size());
        Assert.assertFalse(fsh.hasMoreObjects());
    }

    /**
     * Tests a non recursive harvest with checksums with no file changes on 2nd
     * harvest, and a forced harvest on 3rd
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void getObjectsWithCachingNoChanges() throws Exception {
        // the initial harvest will pick up the file
        FileSystemHarvester fsh = new FileSystemHarvester();
        fsh.init(getConfig("/fsh-config-caching.json"));
        List<DigitalObject> items = fsh.getObjects();
        Assert.assertEquals(2, items.size());

        // next harvest will detect no change
        FileSystemHarvester fsh2 = new FileSystemHarvester();
        fsh2.init(getConfig("/fsh-config-caching.json"));
        List<DigitalObject> items2 = fsh2.getObjects();
        Assert.assertEquals(0, items2.size());

        // forced harvest
        FileSystemHarvester fsh3 = new FileSystemHarvester();
        fsh3.init(getConfig("/fsh-config-caching-force.json"));
        List<DigitalObject> items3 = fsh3.getObjects();
        Assert.assertEquals(2, items3.size());
    }

    /**
     * Tests a non recursive harvest with checksums with file changes on 2nd
     * harvest
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void getObjectsWithCachingChanges() throws Exception {
        // the initial harvest will pick up the file
        FileSystemHarvester fsh = new FileSystemHarvester();
        fsh.init(getConfig("/fsh-config-caching.json"));
        List<DigitalObject> items = fsh.getObjects();
        Assert.assertEquals(2, items.size());

        FileUtils.writeStringToFile(testFile, "changed!");

        // next harvest will detect the change
        FileSystemHarvester fsh2 = new FileSystemHarvester();
        fsh2.init(getConfig("/fsh-config-caching.json"));
        List<DigitalObject> items2 = fsh2.getObjects();
        Assert.assertEquals(1, items2.size());
    }

    /**
     * Tests a non recursive harvest with checksums with file deleted on 2nd
     * harvest
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void getObjectsWithCachingDeleted() throws Exception {
        // the initial harvest will pick up the file
        FileSystemHarvester fsh = new FileSystemHarvester();
        fsh.init(getConfig("/fsh-config-caching.json"));
        List<DigitalObject> items = fsh.getObjects();
        Assert.assertEquals(2, items.size());

        FileUtils.deleteQuietly(testFile);

        // next harvest will detect the change
        FileSystemHarvester fsh2 = new FileSystemHarvester();
        fsh2.init(getConfig("/fsh-config-caching.json"));
        List<DigitalObject> items2 = fsh2.getObjects();
        Assert.assertEquals(0, items2.size());

        List<DigitalObject> items4 = new ArrayList<DigitalObject>();
        do {
            List<DigitalObject> items3 = fsh2.getDeletedObjects();
            items4.addAll(items3);
        } while (fsh2.hasMoreDeletedObjects());
        Assert.assertEquals(1, items4.size());
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
            if (id.contains("/books/")) {
                expectedSize = 3;
            } else if (id.contains("/music/")) {
                expectedSize = 2;
            } else if (id.contains("/pictures/")) {
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
                if (id.contains("/books/")) {
                    expectedSize = 3;
                } else if (id.contains("/pictures/")) {
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
