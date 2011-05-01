/*
 * Copyright (C) 2010 University of Southern Queensland
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
package au.edu.usq.fascinator.contrib.feedreader;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.fetcher.FetcherEvent;

/**
 * A listener that doesn't care what happens. Useful if you just want to update
 * the cache.
 * 
 * @author Duncan Dickinson
 */
public class IgnorantItemListener extends ItemListener {

    /**
     * Generic logging
     */
    private static Logger log = LoggerFactory
            .getLogger(IgnorantItemListener.class);

    public IgnorantItemListener() {
        super();
    }

    public void feedReaderStateChangeEvent(FeedReaderStateChangeEvent event) {
        super.feedReaderStateChangeEvent(event);

        log.debug("Event (" + this.getFeedURL() + "): " + event.getEventType());
        if (FetcherEvent.EVENT_TYPE_FEED_RETRIEVED.equals(event.getEventType())) {
            if (log.isDebugEnabled()) {
                log.debug("Feed change: " + this.getFeedURL());
                List<SyndEntry> itemList = this.getFeed().getEntries();
                for (SyndEntry entry : itemList) {
                    log.debug("New item: " + entry.getContents());
                }
            }
        } else if (FetcherEvent.EVENT_TYPE_FEED_UNCHANGED.equals(event
                .getEventType())) {
            log.debug("No change in feed: " + this.getFeedURL());
        }

    }

}
