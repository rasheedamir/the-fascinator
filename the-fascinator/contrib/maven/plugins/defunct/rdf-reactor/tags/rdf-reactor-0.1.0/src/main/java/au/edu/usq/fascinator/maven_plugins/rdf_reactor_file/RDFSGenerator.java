package au.edu.usq.fascinator.maven_plugins.rdf_reactor_file;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.SimpleLayout;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Maven plugin for generating Java classes from RDFS.
 * 
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
            schema.generateCode(skipBuiltins, workingDirectory,
                    outputDirectory, rdfReactorLogfile, getLog());
        }

        // add generated code to list of files to be compiled
        if (project != null) {
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        }
    }

    private void initialiseRDFReactorLogfile() throws MojoExecutionException {
        try {
            // make sure that directory for log file exists.
            rdfReactorLogfile.getParentFile().mkdirs();
            // configure logging infrastructure for RDFReactor
            FileAppender logFileAppender = new FileAppender(new SimpleLayout(),
                    rdfReactorLogfile.getAbsolutePath());
            BasicConfigurator.configure(logFileAppender);
        } catch (IOException ioe) {
            throw new MojoExecutionException(
                    "Cannot open log file for writing RDFReactor log messages",
                    ioe);
        }
        getLog()
                .info(
                        "RDFReactor's log messages are written to "
                                + rdfReactorLogfile);
    }
}
