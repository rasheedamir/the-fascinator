/*
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
package au.edu.usq.fascinator.maven_plugins.rdf_reactor_file;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Maven plugin for generating Java classes from RDFS.
 * 
 * @author Duncan Dickinson
 * @goal rdfs
 * @phase generate-sources
 */
public class RDFSGenerator extends AbstractMojo {

    /**
     * A list of one or more SchemaItem objects. The SchemaItem instance
     * provides the RDFS Files and URLs
     * 
     * @see SchemaItem
     * @parameter
     * @required
     */
    private List<SchemaItem> schemaList;

    /**
     * The directory where any downloaded schema shall be written to.
     * 
     * @parameter 
     *            expression="${project.build.directory}/downloaded-resources/schema"
     */
    private File workingDirectory;

    /**
     * The directory where generated java code shall be written to.
     * 
     * @parameter 
     *            expression="${project.build.directory}/generated-sources/rdfs-classes"
     * @required
     */
    private File outputDirectory;

    /**
     * Specifies whether implicitly existing RDF classes (contained in RDF or
     * RDFS) shall be generated as well (false) or not (true).
     * 
     * @parameter default-value="true"
     */
    private boolean skipBuiltins;

    /**
     * The Maven Project Object.
     * 
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * Log file location of RDFReactor.
     * 
     * @parameter default-value="target/rdfreactor.log"
     * @readonly
     */
    private File rdfReactorLogfile;

    public void execute() throws MojoExecutionException, MojoFailureException {
        initialiseRDFReactorLogfile();

        this.workingDirectory.mkdirs();

        for (SchemaItem schema : schemaList) {
            getLog().info("Preparing schema: " + schema.getSchemaName());
            schema.generateCode(skipBuiltins,
                    workingDirectory,
                    outputDirectory,
                    rdfReactorLogfile,
                    getLog());
        }

        // add generated code to list of files to be compiled
        if (project != null) {
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        }
    }

    private void initialiseRDFReactorLogfile() throws MojoExecutionException {
        // make sure that directory for log file exists.
        rdfReactorLogfile.getParentFile().mkdirs();

        // try {
        //
        // // configure logging infrastructure for RDFReactor
        //
        // FileAppender logFileAppender = new FileAppender(new SimpleLayout(),
        // rdfReactorLogfile.getAbsolutePath());
        // BasicConfigurator.configure(logFileAppender);
        //
        // } catch (IOException ioe) {
        // throw new
        // MojoExecutionException("Cannot open log file for writing RDFReactor log messages",
        // ioe);
        // }

        getLog().info("RDFReactor's log messages are written to "
                + rdfReactorLogfile);
    }

}
