/* 
 * The Fascinator - Common Library
 * Copyright (C) 2008 University of Southern Queensland
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.common.storage.impl.GenericPayload;
import au.edu.usq.fascinator.contrib.feedreader.FeedHelper;

import com.sun.syndication.feed.synd.SyndEntry;

public class FeedItemContentPayload extends GenericPayload {

	private static String htmlTemplate = null;
    private SyndEntry feedEntry;

    public FeedItemContentPayload(SyndEntry payload) throws IOException {
        this.feedEntry = payload;
        setId("content");
        setLabel("RSS/ATOM Feed");
        setContentType("text/xml");
        setType(PayloadType.Data);
    	if (FeedItemContentPayload.htmlTemplate == null) {
        	FeedItemContentPayload.htmlTemplate = FeedHarvester.getTemplate();
    	}
    }

    @Override
    public InputStream getInputStream()  {
        try {
            return IOUtils.toInputStream(FeedHelper.toXHTMLSegment(feedEntry,FeedItemContentPayload.htmlTemplate));
        } catch (ResourceNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseErrorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
