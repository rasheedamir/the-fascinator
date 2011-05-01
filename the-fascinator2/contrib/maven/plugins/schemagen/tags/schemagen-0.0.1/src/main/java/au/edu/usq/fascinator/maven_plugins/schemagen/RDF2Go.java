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
package au.edu.usq.fascinator.maven_plugins.schemagen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Maven plugin for generating Java classes from RDFS for use with RDF2Go.
 * 
 * @author Duncan Dickinson
 * @goal rdf2go
 * 
 */
public class RDF2Go extends SchemaGenerator {

    public static void main(String[] args) throws Exception {
        org.ontoware.rdf2go.util.VocabularyWriter.main(args);
    }

    /* (non-Javadoc)
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Using RDF2Go to generate schema");
        for (String item : getSchema().keySet()) {
            getLog().info("Loading: " + item + " - " + getSchema().get(item));

            File schema;
            try {
                File f = new File(System.getProperty("java.io.tmpdir"), item
                        + ".xml");

                schema = downloadSchema(new URL(getSchema().get(item)), f
                        .getAbsolutePath());
                getLog().info("Schema downloaded: " + schema.exists());
            } catch (IOException e) {
                throw new MojoFailureException(e.getMessage());
            }

            File outFolder = new File(getOutputFolder().getPath()
                    + File.separator
                    + getPackageName().replace('.', File.separatorChar));
            outFolder.mkdirs();

            String[] args = { "-i", schema.getAbsolutePath(), "-o",
                    outFolder.getAbsolutePath(), "--package", getPackageName(),
                    "-n", item, "-a", getSchema().get(item),
                    "-namespacestrict", Boolean.toString(getNamespaceStrict()) };
            try {
                getLog().info("Calling VocabularyWriter for "
                        + schema.getAbsolutePath());
                main(args);
                getLog().info("Completed VocabularyWriter for "
                        + schema.getAbsolutePath());
            } catch (Exception e) {
                throw new MojoFailureException(e.getMessage());
            }
        }
    }

    /**
     * Downloads the schema located at the specified URL
     * 
     * @param url
     *            The URL of the schema
     * @param outputFileName
     *            the download destination
     * @return a file handle for the downloaded file
     * @throws IOException
     */
    public File downloadSchema(URL url, String outputFileName)
            throws IOException {
        String inputLine;
        BufferedReader in = null;
        BufferedWriter out = null;
        URLConnection connection = null;

        File schemaFile = new File(outputFileName);
        schemaFile.getParentFile().mkdirs();

        try {
            getLog().info("Accessing " + url.toExternalForm());
            connection = url.openConnection();
            connection.addRequestProperty("accept", "application/rdf+xml");
            connection.connect();
        } catch (IOException e) {
            throw new IOException("Failed to access requested URL ("
                    + url.toExternalForm() + ")", e);
        }
        try {
            try {

                in = new BufferedReader(new InputStreamReader(connection
                        .getInputStream()));
            } catch (IOException e) {
                throw new IOException("Failed to read from the requested URL ("
                        + url.toExternalForm() + ")", e);
            }

            try {
                out = new BufferedWriter(new FileWriter(outputFileName));
            } catch (IOException e) {
                throw new IOException("Failed to create the output file "
                        + outputFileName + " for the requested URL ("
                        + url.toExternalForm() + ")", e);
            }

            try {
                while ((inputLine = in.readLine()) != null) {
                    out.append(inputLine + "\n");
                }
            } catch (IOException e) {
                throw new IOException("Failed to create a local version of ("
                        + url.toExternalForm() + ")", e);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
        return schemaFile;
    }
}
