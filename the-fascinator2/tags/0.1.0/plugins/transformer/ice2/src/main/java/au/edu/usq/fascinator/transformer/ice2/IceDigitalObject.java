/*
 * The Fascinator - Plugin - Transformer - ICE 2
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
package au.edu.usq.fascinator.transformer.ice2;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject;

/**
 * Digital object representation for ICE renditions
 * 
 * @author Linda Octalina
 * @author Oliver Lucido
 */
public class IceDigitalObject extends GenericDigitalObject {

    /** Logging */
    private Logger log = LoggerFactory.getLogger(IceDigitalObject.class);

    /**
     * Creates a DigitalObject with rendition payloads from the specified zip
     * 
     * @param object original object
     * @param filePath path to the zip renditions
     */
    public IceDigitalObject(DigitalObject object, String filePath) {
        super(object);
        try {
            log.info("filePath: " + filePath);
            if (filePath.endsWith(".zip")) {
                File zipPathFile = new File(filePath);
                ZipFile zipFile = new ZipFile(zipPathFile);
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        addPayload(new IcePayload(zipPathFile, entry));
                    }
                }
            } else {
                File filePathFile = new File(filePath);
                addPayload(new IcePayload(filePathFile, null));
            }
        } catch (IOException ioe) {
            log.error("Failed to add rendition payloads: {}", ioe);
        }
    }
}
