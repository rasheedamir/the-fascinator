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
package au.edu.usq.fascinator.contrib.feedreader.util;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.module.DCModuleImpl;
import com.sun.syndication.feed.module.DCSubject;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndCategoryImpl;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndLink;
import com.sun.syndication.feed.synd.SyndLinkImpl;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.feed.synd.SyndPersonImpl;

/**
 * Provides some handy functions for handling feed data.
 * 
 * @author Duncan Dickinson
 * 
 */
public class FeedHelper {

    /**
     * Some feeds don't provide a URI so this function falls back to the feed's
     * link as the identifier
     * 
     * @param entry
     *            The feed entry
     * @return An identifier for the feed entry
     * 
     */
    public static String getID(SyndEntry entry) {
        // Determine an identifier for the post
        if (entry.getUri() != null) {
            return entry.getUri();
        } else {
            return entry.getLink();
        }
    }

    /**
     * Allows for easier handling of authors. RSS feeds don't always result in a
     * list of authors so this function builds you a list, saving checking.
     * 
     * @param entry
     *            The feed entry
     * @return A list of all authors for the entry
     */
    public static List<SyndPerson> getAuthors(SyndEntry entry) {
        if (entry.getAuthors().size() > 0) {
            return entry.getAuthors();

        }

        ArrayList<SyndPerson> authors = new ArrayList<SyndPerson>();
        SyndPerson author = new SyndPersonImpl();
        author.setName(entry.getAuthor());
        authors.add(author);
        return authors;

    }

    /**
     * Allows for easier handling of links
     * 
     * @param entry
     *            The feed entry
     * @return A list of links from the entry
     */
    public static List<SyndLink> getLinks(SyndEntry entry) {
        if (entry.getLinks().size() > 0) {
            return entry.getLinks();
        }
        ArrayList<SyndLink> links = new ArrayList<SyndLink>();
        SyndLinkImpl link = new SyndLinkImpl();
        link.setHref(entry.getLink());
        links.add(link);
        return links;
    }

    /**
     * This is provided as an interface for dealing with the contents of an
     * entry.
     * 
     * At present it just returns entry.getContents() but exists in case
     * handling needs to be abstracted in the future.
     * 
     * @param entry
     *            The feed entry
     * @return
     */
    public static List<SyndContent> getContents(SyndEntry entry) {
        return entry.getContents();
    }

    /**
     * Some feeds don't structure the categories nicely so this function just
     * normalises handling and grabs and DC.Subjects if they exist
     * 
     * @param entry
     *            The feed entry
     * @return
     */
    public static List<SyndCategory> getCategories(SyndEntry entry) {
        ArrayList<SyndCategory> categories = new ArrayList<SyndCategory>();
        categories.addAll(entry.getCategories());

        /*
         * Services such as Slashdot seem to have their tags/categories within
         * the Dublin Core
         */
        DCModuleImpl dc = (DCModuleImpl) entry
                .getModule(com.sun.syndication.feed.module.DCModule.URI);
        if (dc != null) {
            for (DCSubject dcSubject : (List<DCSubject>) dc.getSubjects()) {
                SyndCategoryImpl category = new SyndCategoryImpl();
                category.setName(dcSubject.getValue());
                category.setTaxonomyUri(dcSubject.getTaxonomyUri());
                categories.add(category);
            }
        }
        return categories;
    }

}
