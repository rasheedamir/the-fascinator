package au.edu.usq.fascinator.harvester.feed;

/* 
 * The Fascinator - Plugin - Harvester - Test Harvester
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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.HarvestClient;
import au.edu.usq.fascinator.api.Configurable;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.harvester.Harvester;
import au.edu.usq.fascinator.api.harvester.HarvesterException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.contrib.feedreader.FeedReader;
import au.edu.usq.fascinator.contrib.feedreader.FeedReaderStateChangeEvent;
import au.edu.usq.fascinator.contrib.feedreader.ItemListener;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.fetcher.FetcherEvent;

/**
 * @author dickinso
 * 
 */
public class FeedHarvester extends ItemListener implements Harvester, Configurable  {

	/**
	 * 
	 */
	private JsonConfig config;

	private static String template=null;
	
	/**
	 * 
	 */
	private HashSet<FeedReader> feeds;

	private String cacheDirectory;

	/**
	 * 
	 */
	private boolean moreObjects = true;

	List<DigitalObject> latestFeedItems;
	
	/** Logging */
	private static Logger log = LoggerFactory.getLogger(FeedHarvester.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see au.edu.usq.fascinator.api.Plugin#getId()
	 */
	public String getId() {
		return "feed-harvester";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see au.edu.usq.fascinator.api.Plugin#getName()
	 */
	public String getName() {
		return Messages.getString("harvester-plugin-description");
	}

	public static void setTemplate(String templateFile){
		template = templateFile;
	}
	
	public static String getTemplate(){
		return template;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see au.edu.usq.fascinator.api.Plugin#init(java.io.File)
	 */
	public void init(File jsonFile) throws PluginException {
		try {
			config = new JsonConfig(jsonFile);
		} catch (IOException ioe) {
			throw new HarvesterException(ioe);
		}
		// Load the list of requested URLs
		List<Object> urlList = config.getList("harvester/feed-harvester/urls");
		// Load the cache location
		cacheDirectory = config.get("harvester/feed-harvester/cache", "");
		FeedHarvester.setTemplate(config.get("harvester/feed-harvester/template", ""));
		feeds = new HashSet<FeedReader>();
		
		// Now iterate through the urls and create FeedReaders
		for (Object feed : urlList) {
			try {
				FeedReader reader = new FeedReader((String) feed, cacheDirectory);
				reader.addFeedReaderStateChangeListener(this);
				feeds.add(reader);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (java.lang.ClassCastException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see au.edu.usq.fascinator.api.harvester.Harvester#getObjects()
	 */
	public List<DigitalObject> getObjects() throws HarvesterException {
		latestFeedItems = new ArrayList<DigitalObject>();
		
		for (FeedReader feed: feeds) {
			feed.read();
		}
		
		moreObjects = false;
		return latestFeedItems;
	}
	
	public void feedReaderStateChangeEvent(FeedReaderStateChangeEvent event) {
		super.feedReaderStateChangeEvent(event);
		if (FetcherEvent.EVENT_TYPE_FEED_RETRIEVED.equals(event.getEventType())) {
		    for (SyndEntry entry: (List<SyndEntry>)this.getFeed().getEntries()){
		        latestFeedItems.add(new FeedItemDigitalObject(entry));   
		    }
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see au.edu.usq.fascinator.api.harvester.Harvester#hasMoreObjects()
	 */
	public boolean hasMoreObjects() {
		return moreObjects;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see au.edu.usq.fascinator.api.Plugin#shutdown()
	 */
	public void shutdown() throws PluginException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see au.edu.usq.fascinator.api.Configurable#getConfig()
	 */
	public String getConfig() {
		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(getClass().getResourceAsStream(
					"/" + getId() + "-config.html"), writer);
		} catch (IOException ioe) {
			writer.write("<span class=\"error\">" + ioe.getMessage()
					+ "</span>");
		}
		return writer.toString();
	}
	
    public static void main(String[] args) {
    	HarvestClient.main(args);
    }

}
