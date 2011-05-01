package au.edu.usq.fascinator.maven_plugins.rdf_reactor_file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.Reasoning;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdfreactor.generator.CodeGenerator;

/**
 * Represents a set of RDF schemas that are to be translated into Java classes.
 * Each file and URL provided is merged into an RDF2Go model and then fed into
 * RDFReactor. This way, you can avoid method stubs by linking any any schema
 * you need to use. Please note that this class uses and in-memory model.
 * Including a large number of schema may cause delays.
 * 
 * @author Duncan Dickinson
 */
public class SchemaItem {

    /**
     * Holds a model of the combined RDF
     */
    private Model model = RDF2Go.getModelFactory().createModel();

    /**
     * Just a logger.
     */
    private Log log = null;

    /**
     * A human readable name for the set of schema
     * 
     * @parameter
     * @required
     */
    private String schemaName;

    /**
     * A list of URLs that are aggregated into the schema.
     * 
     * @parameter
     */
    private HashSet<URL> schemaUrlLibrary;

    /**
     * A list of files that are aggregated into the schema.
     * 
     * @parameter
     */
    private HashSet<File> schemaFileLibrary;

    /**
     * The package into which all of the resultant Java classes are placed
     * 
     * @parameter
     * @required
     */
    private String packageName;

    /**
     * Adds a prefix to the resultant Java methods
     * 
     * @parameter
     */
    private String methodPrefix;

    /**
     * @return
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * @return
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * @return
     */
    public String getMethodPrefix() {
        return methodPrefix;
    }

    /**
     * @return
     */
    public HashSet<URL> getSchemaUrlLibrary() {
        return schemaUrlLibrary;
    }

    /**
     * @return
     */
    public HashSet<File> getSchemaFileLibrary() {
        return schemaFileLibrary;
    }

    /**
     * Loads the schema from schemaUrlLibrary and schemaFileLibrary
     * 
     * @param workingDirectory
     *            URLs are downloaded to the workingDirectory
     * @return An RDF2Go Model containing a combination of all of the schemas
     * @throws IOException
     */
    private Model loadSchema(File workingDirectory) throws IOException {
        if (schemaUrlLibrary != null) {
            for (URL schema : schemaUrlLibrary) {
                log.info("Schema requested from " + schema.toExternalForm());
                File rdfs = downloadSchema(schema, getSchemaFilePath(
                        workingDirectory, schema));
                model.readFrom(new FileReader(rdfs));
            }
        }

        if (schemaFileLibrary != null) {
            for (File schema : schemaFileLibrary) {
                log.info("Schema requested from " + schema.getAbsolutePath());
                model.readFrom(new FileReader(schema));
            }
        }

        return model;
    }

    /**
     * @param workingDirectory
     * @param url
     * @return
     */
    private String getSchemaFilePath(File workingDirectory, URL url) {
        String rdfsFile = workingDirectory.getPath() + "\\"
                + schemaName.replaceAll(" ", "_") + "\\"
                + url.toExternalForm().replaceAll("\\W", "_") + ".rdfs";
        return rdfsFile;
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
    private File downloadSchema(URL url, String outputFileName)
            throws IOException {
        String inputLine;
        BufferedReader in = null;
        BufferedWriter out = null;
        URLConnection connection = null;

        File schemaFile = new File(outputFileName);
        schemaFile.getParentFile().mkdirs();

        try {
            log
                    .info("Accessing " + url.toExternalForm() + " for "
                            + schemaName);
            connection = url.openConnection();
            connection.addRequestProperty("accept", "application/rdf+xml");
            connection.connect();
        } catch (IOException e) {
            throw new IOException("Failed to access requested URL ("
                    + url.toExternalForm() + ") for " + schemaName, e);
        }
        try {
            try {

                in = new BufferedReader(new InputStreamReader(connection
                        .getInputStream()));
            } catch (IOException e) {
                throw new IOException("Failed to read from the requested URL ("
                        + url.toExternalForm() + ") for " + schemaName, e);
            }

            try {
                out = new BufferedWriter(new FileWriter(outputFileName));
            } catch (IOException e) {
                throw new IOException("Failed to create the output file "
                        + outputFileName + " for the requested URL ("
                        + url.toExternalForm() + ") for " + schemaName, e);
            }

            try {
                while ((inputLine = in.readLine()) != null) {
                    out.append(inputLine + "\n");
                }
            } catch (IOException e) {
                throw new IOException("Failed to create a local version of ("
                        + url.toExternalForm() + ") for " + schemaName, e);
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

    /**
     * Generates the Java code for the requested RDFS Most of these parameters
     * are as per the org.ontoware.rdfreactor.generator.CodeGenerator.generate
     * method
     * 
     * @param skipBuiltins
     *            if false, internal helper classes are re-generated. This is
     *            usually not needed.
     * @param workingDirectory
     *            location to download any schema listed as a URL
     * @param outputDirectory
     *            location for generated Java classes
     * @param rdfReactorLogfile
     * @param mavenLogger
     * @throws MojoExecutionException
     * @throws MojoFailureException
     *             when no schema is provided, the various schema could not be
     *             loaded or the generation fails
     */
    protected void generateCode(boolean skipBuiltins, File workingDirectory,
            File outputDirectory, File rdfReactorLogfile, Log mavenLogger)
            throws MojoExecutionException, MojoFailureException {
        model.open();
        this.log = mavenLogger;
        // Check to make sure that we have a file or a URL
        if (schemaUrlLibrary == null && schemaFileLibrary == null) {
            throw new MojoFailureException(this, "Schema file error",
                    "No locations (file or URL given for " + this.schemaName);
        }

        Model workingSchemaModel = null;
        try {
            workingSchemaModel = loadSchema(workingDirectory);
        } catch (IOException e) {
            throw new MojoFailureException(e, "Schema access error",
                    "Could not access the requested schema for " + schemaName);
        }
        log.info("Generating code for " + schemaName
                + ". Classes will be in package " + packageName
                + " and with method prefix " + methodPrefix
                + ". skipBuiltins is " + skipBuiltins + ".");

        try {
            CodeGenerator.generate(workingSchemaModel, outputDirectory,
                    packageName, Reasoning.rdfs, skipBuiltins, methodPrefix);
        } catch (Exception e) {
            throw new MojoFailureException(e, "RDFS processing error",
                    "Could not generate code from the specified RDF schema file.");
        } finally {
            model.close();
        }

    }
}
