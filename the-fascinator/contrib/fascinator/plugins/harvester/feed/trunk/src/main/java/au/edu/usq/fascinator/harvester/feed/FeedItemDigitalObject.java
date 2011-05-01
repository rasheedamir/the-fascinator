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
package au.edu.usq.fascinator.harvester.feed;

import java.io.IOException;

import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject;

import com.sun.syndication.feed.synd.SyndEntry;



public class FeedItemDigitalObject extends GenericDigitalObject {

    /**
     * Creates an object for a local file
     * 
     * @param file a file
     */
    public FeedItemDigitalObject(SyndEntry entry) {
        super(entry.getUri() + "/content");
        this.setMetadataId("metadata");
        addPayload(new FeedItemMetadataPayload(entry));
        try {
			addPayload(new FeedItemContentPayload(entry));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
