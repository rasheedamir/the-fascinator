/*
 * The Fascinator Copyright (C) 2009 University of Southern Queensland 
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

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic command line interface to multiple feeds
 * 
 * @author Duncan Dickinson
 */
public class FeedReader {

    private static Logger log = LoggerFactory.getLogger(FeedReader.class);
    private static final String OPT_URL = "--url";
    private static final String OPT_HTML = "--html";
    private static final String OPT_RDF = "--rdf";
    private static final String OPT_JSON = "--json";
    private static final String OPT_OUTPUT_DIR = "--output_dir";
    private static final String DEFAULT_OUTPUT_DIR = System.getProperty("user.home")
            + "/.feed-reader/output";
    private static final String OPT_CACHE_DIR = "--cache_dir";
    private static final String DEFAULT_CACHE_DIR = System.getProperty("user.home")
            + "/.feed-reader/cache";

    /**
     * @param args
     *            Command line arguments
     * @throws MalformedURLException
     * @throws FileNotFoundException
     */
    public static void main(String[] args) throws MalformedURLException,
            FileNotFoundException {
        HashMap<String, Object> argumentMap = null;

        if (args.length == 0) {
            displayUsageMessage();
            System.exit(1);
        }
        if (args.length == 1) {
            if (args[0].equals("-h")) {
                displayUsageMessage();
                System.exit(0);
            }
        }

        try {
            argumentMap = parseArgs(args);
            checkArgs(argumentMap);
        } catch (IllegalArgumentException e) {
            System.out.println("Incorrect arguments");
            displayUsageMessage();
            System.exit(1);
        }
        setArgDefaults(argumentMap);

        boolean outputHtml = (Boolean) argumentMap.get(OPT_HTML);
        boolean outputRdf = (Boolean) argumentMap.get(OPT_RDF);
        boolean outputJson = (Boolean) argumentMap.get(OPT_JSON);
        String cacheDir = (String) argumentMap.get(OPT_CACHE_DIR);
        String outputDir = (String) argumentMap.get(OPT_OUTPUT_DIR);

        log.info("Cache dir: " + argumentMap.get(OPT_CACHE_DIR));
        log.info("Output dir: " + argumentMap.get(OPT_OUTPUT_DIR));
        log.info("Produce HTML: " + outputHtml);
        log.info("Produce RDF: " + outputRdf);
        log.info("Produce JSON: " + outputJson);

        readFeeds(cacheDir,
                outputHtml,
                outputRdf,
                outputJson,
                outputDir,
                (ArrayList<URL>) argumentMap.get(OPT_URL));

    }

    public static void readFeeds(String cacheDir, boolean outputHtml,
            boolean outputRdf, boolean outputJson, String outputDir, ArrayList<URL> urlList)
            throws MalformedURLException, FileNotFoundException {
        for (URL url : urlList) {
            Feed feed = new Feed(url, cacheDir);
            feed.addFeedReaderStateChangeListener(new DemoItemListener(outputHtml,
                    outputRdf,
                    outputJson,
                    outputDir,
                    url));

            new Thread(feed).start();
        }
    }

    public static HashMap<String, Object> parseArgs(String[] args)
            throws IllegalArgumentException, MalformedURLException {
        HashMap<String, Object> argMap = new HashMap<String, Object>();

        // The URLs of the RSS/ATOM feeds
        ArrayList<URL> feedURLs = new ArrayList<URL>();

        for (String param : args) {
            String[] kv = param.split("=", 2);
            if (kv.length == 1) {
                if (kv[0].equals(FeedReader.OPT_RDF)) {
                    argMap.put(FeedReader.OPT_RDF, true);
                } else if (kv[0].equals(FeedReader.OPT_HTML)) {
                    argMap.put(FeedReader.OPT_HTML, true);
                } else if (kv[0].equals(FeedReader.OPT_JSON)) {
                    argMap.put(FeedReader.OPT_JSON, true);
                } else {
                    throw new IllegalArgumentException("Unknown option: "
                            + kv[0]);
                }
            } else if (kv[0].equals(FeedReader.OPT_URL)) {
                log.debug("--url Argument: " + kv[1]);
                feedURLs.add(new URL(kv[1]));
            } else {
                argMap.put(kv[0], kv[1]);
            }
        }
        argMap.put(OPT_URL, feedURLs);
        return argMap;
    }

    private static void checkArgs(HashMap<String, Object> argumentMap) {
        // we need url at least
        if (((ArrayList<URL>) argumentMap.get(OPT_URL)).size() == 0) {
            throw new IllegalArgumentException();
        }
    }

    private static void setArgDefaults(HashMap<String, Object> argumentMap) {
        if (!argumentMap.containsKey(OPT_CACHE_DIR)) {
            argumentMap.put(OPT_CACHE_DIR, DEFAULT_CACHE_DIR);
        }
        if (!argumentMap.containsKey(OPT_OUTPUT_DIR)) {
            argumentMap.put(OPT_OUTPUT_DIR, DEFAULT_OUTPUT_DIR);
        }

        if (!argumentMap.containsKey(OPT_RDF)) {
            argumentMap.put(OPT_RDF, false);
        }

        if (!argumentMap.containsKey(OPT_HTML)) {
            argumentMap.put(OPT_HTML, false);
        }
    }

    private static final void displayUsageMessage() {
        System.out.println("Usage: FeedReader [OPTIONS] \n\n"
                + "FeedReader displays the latest contents of a syndication feed from the URLS\n\n"
                + "Options:\n"
                + "-h\tView usage message (this one)\n"
                + OPT_URL
                + "=<feed url>\tThe URL of a feed.\n"
                + "\t\tNote: This option may be repeated.\n"
                + OPT_CACHE_DIR
                + "=<cache dir>\tThe location to cache feed entries - this will be created.\n"
                + "\t\t"
                + DEFAULT_CACHE_DIR
                + " is the default\n"
                + OPT_OUTPUT_DIR
                + "=<output dir>\tSend output to the specified directory.\n"
                + "\t\t" + DEFAULT_OUTPUT_DIR + " is the default\n"
                + "\t\tA subdir is created for each feed\n" + OPT_HTML
                + "\tProduce HTML output\n" + OPT_RDF
                + "\tProduce JSON output\n" + OPT_JSON
                + "\tProduce RDF output\n" + "\n");
    }
}
