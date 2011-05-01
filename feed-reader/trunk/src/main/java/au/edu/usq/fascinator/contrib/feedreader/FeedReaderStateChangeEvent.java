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

import java.net.URL;
import java.util.EventObject;

import com.sun.syndication.fetcher.FetcherEvent;

/**
 * Captures the changed state in a feed
 */
public class FeedReaderStateChangeEvent extends EventObject {

    private static final long serialVersionUID = -909191310892385597L;

    private FetcherEvent fetcherEvent;
    private Feed feed;
    private URL url;

    /**
     * @param source
     * @param e
     */
    public FeedReaderStateChangeEvent(Feed source, FetcherEvent e) {
        super(source);
        this.fetcherEvent = e;
        this.feed = source;
        this.url = source.getFeedURL();
    }

    /**
     * @return the reader
     */
    public synchronized Feed getFeed() {
        return this.feed;
    }

    /**
     * @return the fetcherEvent
     */
    public synchronized FetcherEvent getFetcherEvent() {
        return this.fetcherEvent;
    }

    /**
     * @return the fetcher event type
     * @see
     */
    public synchronized String getEventType() {
        return this.fetcherEvent.getEventType();
    }

    /**
     * @return the url
     */
    public synchronized URL getUrl() {
        return url;
    }

}
