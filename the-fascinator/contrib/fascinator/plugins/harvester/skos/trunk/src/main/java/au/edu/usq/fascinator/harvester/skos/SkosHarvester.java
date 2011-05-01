/* 
 * The Fascinator - SKOS Harvester Plugin
 * Copyright (C) 2010-2011 University of Southern Queensland
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
package au.edu.usq.fascinator.harvester.skos;

import au.edu.usq.fascinator.api.harvester.HarvesterException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonSimpleConfig;
import au.edu.usq.fascinator.common.harvester.impl.GenericHarvester;
import au.edu.usq.fascinator.common.storage.StorageUtils;
import au.edu.usq.fascinator.vocabulary.SKOS;

import com.hp.hpl.jena.vocabulary.RDF;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.Reasoning;
import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.impl.jena24.ModelImplJena24;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.Syntax;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.semanticdesktop.aperture.rdf.impl.RDFContainerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ontoware.aifbcommons.collection.ClosableIterator;

/**
 * <p>
 * This plugin harvest <a
 * href="http://www.w3.org/TR/2009/NOTE-skos-primer-20090818/">SKOS</a> rdf/xml
 * Concept Scheme. Sample of rdf/xml file for concept can be downloaded from <a
 * href
 * ="http://namespace.adfi.usq.edu.au/anzsrc/">http://namespace.adfi.usq.edu.
 * au/anzsrc/</a>
 * 
 * <h3>Configuration</h3>
 * <p>
 * Sample configuration file for SKOS harvester: <a href=
 * "https://fascinator.usq.edu.au/trac/browser/code/the-fascinator2/contrib/fascinator/plugins/harvester/skos/trunk/src/main/resources/harvest/anzsrc-for.json"
 * >anzsrc-for.json</a> and <a href=
 * "https://fascinator.usq.edu.au/trac/browser/code/the-fascinator2/contrib/fascinator/plugins/harvester/skos/trunk/src/main/resources/harvest/anzsrc-seo.json"
 * >anzsrc-seo.json</a>.
 * </p>
 * 
 * <table border="1">
 * <tr>
 * <th>Option</th>
 * <th>Description</th>
 * <th>Required</th>
 * <th>Default</th>
 * </tr>
 * 
 * <tr>
 * <td>baseFile</td>
 * <td>File of the rdf/xml skos scheme</td>
 * <td><b>Yes</b></td>
 * <td>${fascinator.home}/skos/for.rdf</td>
 * </tr>
 * 
 * </table>
 * 
 * <h3>Examples</h3>
 * <ol>
 * <li>
 * Harvesting for concept scheme
 * 
 * <pre>
 *    "harvester": {
 *         "type": "skos",
 *         "skos": {
 *             "baseFile": "${fascinator.home}/skos/for.rdf"
 *         }
 *     }
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * <h3>Rule file</h3>
 * <p>
 * Sample rule file for the Geonames harvester: <a href=
 * "https://fascinator.usq.edu.au/trac/browser/code/the-fascinator2/contrib/fascinator/plugins/harvester/skos/trunk/src/main/resources/harvest/skos.py"
 * >skos.py</a>
 * </p>
 * 
 * <h3>Wiki Link</h3>
 * <p>
 * <b>None</b>
 * </p>
 * 
 * @author Linda Octalina
 */
public class SkosHarvester extends GenericHarvester {

    /** logging */
    private Logger log = LoggerFactory.getLogger(SkosHarvester.class);

    /** Skos rdf model **/
    private Model rdfModel;

    private URI conceptScheme;

    private RDFContainer container;

    /**
     * Skos harvester constructor
     */
    public SkosHarvester() {
        super("skos", "SKOS Harvester");
    }

    /**
     * Init method to initialise skos harvester
     */
    @Override
    public void init() throws HarvesterException {
        JsonSimpleConfig config;
        log.info("Initialising Skos harvester");
        // Read config
        try {
            config = new JsonSimpleConfig(getJsonConfig().toString());
        } catch (IOException ex) {
            throw new HarvesterException("Failed reading configuration", ex);
        }

        String baseFile = config.getString("", "harvester", "skos", "baseFile");
        File skosFile;
        if (!baseFile.equals("")) {
            skosFile = new File(baseFile);
        } else {
            throw new HarvesterException("No skos file specified");
        }

        try {
            rdfModel = getRdfModel(new FileInputStream(skosFile));
            setConceptScheme();
            container = new RDFContainerImpl(rdfModel, conceptScheme);
        } catch (FileNotFoundException e) {
            log.error("Skos rdf file not found");
        }
    }

    /**
     * Harvest the next set of skos concept, and return their Object IDs
     * 
     * @return Set<String> The set of object IDs just harvested
     * @throws HarvesterException If there are errors
     */
    @Override
    public Set<String> getObjectIdList() throws HarvesterException {
        Set<String> skosObjectIdList = new HashSet<String>();

        String newUri = "anzrc/";
        //Create the conceptSchema object
        newUri += getType(conceptScheme.toString()) + "/";
        try {
            skosObjectIdList.add(createSkosObject(conceptScheme, newUri));
        } catch (StorageException e1) {
            throw new HarvesterException("Fail to create object "
                    + e1.getMessage());
        } catch (IOException e1) {
            throw new HarvesterException("Fail to create payload "
                    + e1.getMessage());
        }

        // Concept list
        Collection concepts = container.getAll(SKOS.hasTopConcept);
        try {
            createConceptStructure(skosObjectIdList, concepts, newUri);
        } catch (StorageException e) {
            throw new HarvesterException("Fail to create skos object");
        } catch (IOException e) {
            throw new HarvesterException("File creation file");
        }
        return skosObjectIdList;
    }

    /**
     * Create concept and its children
     * 
     * @param skosObjectIdList created id list
     * @param collection of children
     * @param conceptUri new concept for reference in fascinator
     * @throws HarvesterException if fail to harvest skos concept
     * @throws StorageException if fail to create new object
     * @throws IOException if fail to create new payload
     */
    private void createConceptStructure(Set<String> skosObjectIdList,
            Collection collection, String conceptUri)
            throws HarvesterException, StorageException, IOException {
        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
            Node node = (Node) iterator.next();
            String conceptType = getType(node.toString());
            String newConceptUri = conceptUri + conceptType + "/";

            RDFContainer conceptContainer = new RDFContainerImpl(rdfModel,
                    node.toString());
            Collection narrowers = conceptContainer.getAll(SKOS.narrower); // Need to loop through all the child again...

            // Create the top concept object
            skosObjectIdList.add(createSkosObject(new URIImpl(node.toString()),
                    newConceptUri));
            // Process the child again
            if (narrowers.size() > 0) {
                createConceptStructure(skosObjectIdList, narrowers,
                        newConceptUri);
            }
        }
    }

    /**
     * Get concept type
     * 
     * @param uri concept uri
     * @return the type of the concept
     */
    private String getType(String uri) {
        String type = uri;
        if (uri.indexOf("#") >= -1) {
            type = uri.split("#")[1];
        }
        return type;
    }

    /**
     * Set concept scheme found in the rdf
     * 
     * @throws HarvesterException no conceptScheme found
     */
    private void setConceptScheme() throws HarvesterException {
        ClosableIterator<? extends Statement> it = rdfModel.findStatements(
                Variable.ANY, new URIImpl(RDF.type.getURI()),
                SKOS.ConceptScheme);
        conceptScheme = null;
        while (it.hasNext()) {
            Statement s = it.next();
            conceptScheme = new URIImpl(s.getSubject().toString());
        }

        if (conceptScheme == null) {
            throw new HarvesterException("Conceptscheme not found in skos rdf");
        }
    }

    /**
     * Get conceptScheme value
     * 
     * @return the conceptscheme
     */
    public URI getConceptScheme() {
        return conceptScheme;
    }

    @Override
    public boolean hasMoreObjects() {
        return false;
    }

    /**
     * Create skos digital object and the payload
     * 
     * @param conceptUri concept to be created
     * @param newConceptUri new uri to be used in fascinator
     * @return created object id
     * @throws HarvesterException if fail to harvest skos concept
     * @throws StorageException if fail to create new object
     * @throws IOException if fail to create new payload
     */
    private String createSkosObject(URI conceptUri, String newConceptUri)
            throws HarvesterException, StorageException, IOException {
        Storage storage = getStorage();
        String conceptId = getType(conceptUri.toString());
        log.info("Creating Skos: {}", conceptId);
        String oid = DigestUtils.md5Hex(conceptId);
        DigitalObject object = StorageUtils.getDigitalObject(storage, oid);
        String pid = conceptId + ".rdf";

        Payload payload = StorageUtils.createOrUpdatePayload(object, pid,
                IOUtils.toInputStream(serialize(conceptUri), "UTF-8"));
        payload.setContentType("text/xml");
        payload.close();

        // update object metadata
        Properties props = object.getMetadata();
        props.setProperty("render-pending", "true");
        props.setProperty("skos-uri", newConceptUri);
        props.setProperty("concept-uri", conceptUri.toString());

        object.close();
        return object.getId();
    }

    /**
     * Serialize the node found
     * 
     * @param uri uri of the concept
     * @return serialized concept in String
     */
    private String serialize(URI uri) {
        ClosableIterator<? extends Statement> iterator = rdfModel
                .findStatements(uri, Variable.ANY, Variable.ANY);

        Model m = RDF2Go.getModelFactory().createModel();
        m.open();
        m.addAll(iterator);
        return m.serialize(Syntax.RdfXml);
    }

    /*****
     * Parse RDF data from an inputstream
     * 
     * @param rdfIn, the inputstream to read and parse
     * @return Model object after parsing
     */
    public Model getRdfModel(InputStream rdfIn) {
        Model model = null;
        Reader reader = null;
        try {
            reader = new InputStreamReader(rdfIn, "UTF-8");
            model = new ModelImplJena24(Reasoning.rdfs);
            model.open();
            model.readFrom(reader);
        } catch (ModelRuntimeException mre) {
            log.error("Failed to create RDF model", mre);
        } catch (IOException ioe) {
            log.error("Failed to read RDF input", ioe);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                }
            }
        }
        return model;
    }

}
