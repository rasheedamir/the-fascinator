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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.htmlparser.util.ParserException;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Syntax;
import org.semanticdesktop.aperture.rdf.impl.RDFContainerImpl;
import org.semanticdesktop.aperture.vocabulary.NIE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.contrib.feedreader.util.FeedHelper;
import au.edu.usq.fascinator.contrib.feedreader.util.PlainTextExtractor;
import au.edu.usq.fascinator.vocabulary.DCTERMS;

import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndLink;
import com.sun.syndication.feed.synd.SyndPerson;

/**
 * Provides functionality to convert a feed entry into an RDF representation
 * 
 * @author Duncan Dickinson
 * 
 */
public class RDFPrinter {
	private static Logger log = LoggerFactory.getLogger(RDFPrinter.class);

	/**
	 * Uses DC and NIE to create an RDF representation of the feed
	 * 
	 * @param entry
	 * @return RDF/XML representation of the feed.
	 */
	public static String toRDFXML(SyndEntry entry) {
		// create an empty Model
		Model model = RDF2Go.getModelFactory().createModel();
		model.open();

		model.setNamespace("nie", NIE.NS_NIE.toString());
		model.setNamespace("dc", DCTERMS.NS_DCTERMS.toString());

		String identifier = FeedHelper.getID(entry);
		// create the resource
		RDFContainerImpl post = new RDFContainerImpl(model, identifier);

		// Resource post = model.createResource(identifier);
		post.add(DCTERMS.identifier, identifier);
		post.add(DCTERMS.title, entry.getTitle());

		// Set authors
		for (SyndPerson author : FeedHelper.getAuthors(entry)) {
			post.add(DCTERMS.creator, author.getName());
		}

		// Creation/Publish date
		if (entry.getPublishedDate() != null) {
			post.add(DCTERMS.created, formatDate(entry.getPublishedDate()));
		}

		// Modified date
		if (entry.getUpdatedDate() != null) {
			post.add(DCTERMS.modified, formatDate(entry.getUpdatedDate()));
		}

		// Add categories
		for (SyndCategory category : FeedHelper.getCategories(entry)) {
			post.add(DCTERMS.subject, category.getName());
		}

		// Add any referenced links
		for (SyndLink link : FeedHelper.getLinks(entry)) {
			post.add(DCTERMS.references, link.getHref());
		}

		// Abstract
		if (entry.getDescription() != null) {
			SyndContent desc = entry.getDescription();
			String text;
			try {
				text = PlainTextExtractor.getPlainText(desc.getType(), desc
						.getValue());
			} catch (ParserException e) {
				text = "Failed to parse abstract";
			}
			if (text != null)
				post.add(DCTERMS.description, text);
		}

		// Plain text
		StringBuilder plainText = new StringBuilder();
		for (SyndContent content : FeedHelper.getContents(entry)) {
			try {
				plainText.append(PlainTextExtractor.getPlainText(content
						.getType(), content.getValue()));
			} catch (ParserException e) {
				plainText.append("Failed to parse content");
			}
		}
		post.add(NIE.plainTextContent, plainText.toString());

		// ByteArrayOutputStream os = new ByteArrayOutputStream();
		String out = model.serialize(Syntax.RdfXml);
		model.close();
		return out;
	}

	/**
	 * (Hopefully) formats a date to W3C standard
	 * 
	 * @param d
	 * @return
	 */

	public static String formatDate(Date d) {
		if (d == null)
			return null;
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		return df.format(d);
	}

}
