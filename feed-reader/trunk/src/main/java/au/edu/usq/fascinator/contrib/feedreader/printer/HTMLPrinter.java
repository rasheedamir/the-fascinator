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

import java.io.IOException;
import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.contrib.feedreader.util.FeedHelper;

import com.sun.syndication.feed.synd.SyndEntry;

/**
 * Provides functionality to convert a feed entry into an HTML segment
 * representation. This representation is not a full HTML page.
 * 
 * @author Duncan Dickinson
 * 
 */
public class HTMLPrinter {
    private static VelocityEngine ve = null;
    private static Logger log = LoggerFactory.getLogger(HTMLPrinter.class);

    /**
     * Uses Velocity to create an HTML representation of the feed.
     * 
     * The method looks for a velocity.properties file to configure velocity
     * 
     * Sample velocity.properties entry:
     * 
     * <pre>
     * class.resource.loader.description = Velocity Classpath Resource Loader
     * class.resource.loader.class = org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
     * </pre>
     * 
     * @param entry
     *            The feed entry
     * @param templateFile
     *            a velocity template
     * @return an HTML segment
     * @throws ResourceNotFoundException
     * @throws ParseErrorException
     * @throws MethodInvocationException
     * @throws IOException
     * @throws Exception
     */
    public static String toXHTMLSegment(SyndEntry entry, String templateFile)
            throws ResourceNotFoundException, ParseErrorException,
            MethodInvocationException, IOException, Exception {

        if (ve == null) {
            ve = new VelocityEngine();
            ve.init("velocity.properties");
        }
        VelocityContext context = new VelocityContext();

        Template template = Velocity.getTemplate(templateFile);

        // Title
        context.put("title", entry.getTitle());

        // Published date
        context.put("date", entry.getPublishedDate());

        // Last Modified
        context.put("modified", entry.getUpdatedDate());

        // Subjects
        context.put("subject", FeedHelper.getCategories(entry));

        // Description
        if (entry.getDescription() != null) {
            context.put("description", entry.getDescription());
        }

        // Contents
        context.put("content", FeedHelper.getContents(entry));

        // Links
        context.put("relation", FeedHelper.getLinks(entry));

        StringWriter sw = new StringWriter();
        template.merge(context, sw);
        return sw.toString();

    }
}
