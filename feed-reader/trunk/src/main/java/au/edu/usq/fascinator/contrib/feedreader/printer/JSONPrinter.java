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
package au.edu.usq.fascinator.contrib.feedreader.printer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.contrib.feedreader.util.FeedHelper;
import au.edu.usq.fascinator.contrib.feedreader.util.PlainTextExtractor;
import org.htmlparser.util.ParserException;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndContent;

import com.sun.syndication.feed.synd.SyndEntry;
import java.util.ArrayList;
import java.util.HashMap;
import net.sf.json.JSONSerializer;

/**
 * Provides functionality to convert a feed entry into a JSON String.
 * 
 * @author Duncan Dickinson
 * 
 */
public class JSONPrinter {

    private static Logger log = LoggerFactory.getLogger(HTMLPrinter.class);

    public static String toJSON(SyndEntry entry) {

        HashMap<String, Object> data = new HashMap<String, Object>();
        //ID
        data.put("dc.identifier", FeedHelper.getID(entry));

        // Title
        data.put("dc.title", entry.getTitle());

        // Published date
        data.put("dc.date", RDFPrinter.formatDate(entry.getPublishedDate()));

        // Last Modified
        data.put("dc.modified", RDFPrinter.formatDate(entry.getUpdatedDate()));

        // Subjects
        ArrayList<String> categories = new ArrayList<String>();
        for (SyndCategory cat : FeedHelper.getCategories(entry)) {
            categories.add(cat.getName());
        }
        data.put("dc.subject", categories);

        // Description
        if (entry.getDescription() != null) {
            SyndContent desc = entry.getDescription();
            String text;
            try {
                text = PlainTextExtractor.getPlainText(desc.getType(), desc.getValue());
            } catch (ParserException e) {
                text = "Failed to parse abstract";
            }
            if (text != null) {
                data.put("dc.description", text);
            }
        }

        // Links
        data.put("dc.relation", FeedHelper.getLinks(entry));

        // Contents
        StringBuilder plainText = new StringBuilder();
        for (SyndContent content : FeedHelper.getContents(entry)) {
            try {
                plainText.append(PlainTextExtractor.getPlainText(content.getType(), content.getValue()));
            } catch (ParserException e) {
                plainText.append("Failed to parse content");
            }
        }
        data.put("content", plainText.toString());


        return JSONSerializer.toJSON(data).toString();

    }
}
