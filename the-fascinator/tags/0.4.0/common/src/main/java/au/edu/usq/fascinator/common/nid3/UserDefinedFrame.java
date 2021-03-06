/**
 * generated by http://RDFReactor.semweb4j.org ($Id: CodeGenerator.java 1535 2008-09-09 15:44:46Z max.at.xam.de $) on 15/10/09 2:48 PM
 */
package au.edu.usq.fascinator.common.nid3;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.ontoware.rdfreactor.runtime.Base;
import org.ontoware.rdfreactor.runtime.ReactorResult;

/**
 * This class manages access to these properties:
 * <ul>
 *   <li> UserDefinedFrameDescription </li>
 *   <li> UserDefinedFrameValue </li>
 * </ul>
 *
 * This class was generated by <a href="http://RDFReactor.semweb4j.org">RDFReactor</a> on 15/10/09 2:48 PM
 */
public class UserDefinedFrame extends Thing {

    /** http://www.semanticdesktop.org/ontologies/2007/05/10/nid3#UserDefinedFrame */
    @SuppressWarnings("hiding")
	public static final URI RDFS_CLASS = new URIImpl("http://www.semanticdesktop.org/ontologies/2007/05/10/nid3#UserDefinedFrame", false);

    /** http://www.semanticdesktop.org/ontologies/2007/05/10/nid3#userDefinedFrameDescription */
    @SuppressWarnings("hiding")
	public static final URI USERDEFINEDFRAMEDESCRIPTION = new URIImpl("http://www.semanticdesktop.org/ontologies/2007/05/10/nid3#userDefinedFrameDescription",false);

    /** http://www.semanticdesktop.org/ontologies/2007/05/10/nid3#userDefinedFrameValue */
    @SuppressWarnings("hiding")
	public static final URI USERDEFINEDFRAMEVALUE = new URIImpl("http://www.semanticdesktop.org/ontologies/2007/05/10/nid3#userDefinedFrameValue",false);

    /** 
     * All property-URIs with this class as domain.
     * All properties of all super-classes are also available. 
     */
    @SuppressWarnings("hiding")
    public static final URI[] MANAGED_URIS = {
      new URIImpl("http://www.semanticdesktop.org/ontologies/2007/05/10/nid3#userDefinedFrameDescription",false),
      new URIImpl("http://www.semanticdesktop.org/ontologies/2007/05/10/nid3#userDefinedFrameValue",false) 
    };


	// protected constructors needed for inheritance
	
	/**
	 * Returns a Java wrapper over an RDF object, identified by URI.
	 * Creating two wrappers for the same instanceURI is legal.
	 * @param model RDF2GO Model implementation, see http://rdf2go.semweb4j.org
	 * @param classURI URI of RDFS class
	 * @param instanceIdentifier Resource that identifies this instance
	 * @param write if true, the statement (this, rdf:type, TYPE) is written to the model
	 *
	 * [Generated from RDFReactor template rule #c1] 
	 */
	protected UserDefinedFrame ( Model model, URI classURI, org.ontoware.rdf2go.model.node.Resource instanceIdentifier, boolean write ) {
		super(model, classURI, instanceIdentifier, write);
	}

	// public constructors

	/**
	 * Returns a Java wrapper over an RDF object, identified by URI.
	 * Creating two wrappers for the same instanceURI is legal.
	 * @param model RDF2GO Model implementation, see http://rdf2go.ontoware.org
	 * @param instanceIdentifier an RDF2Go Resource identifying this instance
	 * @param write if true, the statement (this, rdf:type, TYPE) is written to the model
	 *
	 * [Generated from RDFReactor template rule #c2] 
	 */
	public UserDefinedFrame ( Model model, org.ontoware.rdf2go.model.node.Resource instanceIdentifier, boolean write ) {
		super(model, RDFS_CLASS, instanceIdentifier, write);
	}


	/**
	 * Returns a Java wrapper over an RDF object, identified by a URI, given as a String.
	 * Creating two wrappers for the same URI is legal.
	 * @param model RDF2GO Model implementation, see http://rdf2go.ontoware.org
	 * @param uriString a URI given as a String
	 * @param write if true, the statement (this, rdf:type, TYPE) is written to the model
	 * @throws ModelRuntimeException if URI syntax is wrong
	 *
	 * [Generated from RDFReactor template rule #c7] 
	 */
	public UserDefinedFrame ( Model model, String uriString, boolean write) throws ModelRuntimeException {
		super(model, RDFS_CLASS, new URIImpl(uriString,false), write);
	}

	/**
	 * Returns a Java wrapper over an RDF object, identified by a blank node.
	 * Creating two wrappers for the same blank node is legal.
	 * @param model RDF2GO Model implementation, see http://rdf2go.ontoware.org
	 * @param bnode BlankNode of this instance
	 * @param write if true, the statement (this, rdf:type, TYPE) is written to the model
	 *
	 * [Generated from RDFReactor template rule #c8] 
	 */
	public UserDefinedFrame ( Model model, BlankNode bnode, boolean write ) {
		super(model, RDFS_CLASS, bnode, write);
	}

	/**
	 * Returns a Java wrapper over an RDF object, identified by 
	 * a randomly generated URI.
	 * Creating two wrappers results in different URIs.
	 * @param model RDF2GO Model implementation, see http://rdf2go.ontoware.org
	 * @param write if true, the statement (this, rdf:type, TYPE) is written to the model
	 *
	 * [Generated from RDFReactor template rule #c9] 
	 */
	public UserDefinedFrame ( Model model, boolean write ) {
		super(model, RDFS_CLASS, model.newRandomUniqueURI(), write);
	}

    ///////////////////////////////////////////////////////////////////
    // typing

	/**
	 * Return an existing instance of this class in the model. No statements are written.
	 * @param model an RDF2Go model
	 * @param instanceResource an RDF2Go resource
	 * @return an instance of UserDefinedFrame  or null if none existst
	 *
	 * [Generated from RDFReactor template rule #class0] 
	 */
	public static UserDefinedFrame  getInstance(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.getInstance(model, instanceResource, UserDefinedFrame.class);
	}

	/**
	 * Create a new instance of this class in the model. 
	 * That is, create the statement (instanceResource, RDF.type, http://www.semanticdesktop.org/ontologies/2007/05/10/nid3#UserDefinedFrame).
	 * @param model an RDF2Go model
	 * @param instanceResource an RDF2Go resource
	 *
	 * [Generated from RDFReactor template rule #class1] 
	 */
	public static void createInstance(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		Base.createInstance(model, RDFS_CLASS, instanceResource);
	}

	/**
	 * @param model an RDF2Go model
	 * @param instanceResource an RDF2Go resource
	 * @return true if instanceResource is an instance of this class in the model
	 *
	 * [Generated from RDFReactor template rule #class2] 
	 */
	public static boolean hasInstance(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.hasInstance(model, RDFS_CLASS, instanceResource);
	}

	/**
	 * @param model an RDF2Go model
	 * @return all instances of this class in Model 'model' as RDF resources
	 *
	 * [Generated from RDFReactor template rule #class3] 
	 */
	public static ClosableIterator<org.ontoware.rdf2go.model.node.Resource> getAllInstances(Model model) {
		return Base.getAllInstances(model, RDFS_CLASS, org.ontoware.rdf2go.model.node.Resource.class);
	}

	/**
	 * @param model an RDF2Go model
	 * @return all instances of this class in Model 'model' as a ReactorResult,
	 * which can conveniently be converted to iterator, list or array.
	 *
	 * [Generated from RDFReactor template rule #class3-as] 
	 */
	public static ReactorResult<? extends UserDefinedFrame> getAllInstances_as(Model model) {
		return Base.getAllInstances_as(model, RDFS_CLASS, UserDefinedFrame.class );
	}

    /**
	 * Remove rdf:type UserDefinedFrame from this instance. Other triples are not affected.
	 * To delete more, use deleteAllProperties
	 * @param model an RDF2Go model
	 * @param instanceResource an RDF2Go resource
	 *
	 * [Generated from RDFReactor template rule #class4] 
	 */
	public static void deleteInstance(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		Base.deleteInstance(model, RDFS_CLASS, instanceResource);
	}

	/**
	 * Delete all (this, *, *), i.e. including rdf:type
	 * @param model an RDF2Go model
	 * @param resource
	 */
	public static void deleteAllProperties(Model model,	org.ontoware.rdf2go.model.node.Resource instanceResource) {
		Base.deleteAllProperties(model, instanceResource);
	}

    ///////////////////////////////////////////////////////////////////
    // property access methods

	/**
	 * @param model an RDF2Go model
	 * @param objectValue
	 * @return all A's as RDF resources, that have a relation 'UserDefinedFrame' to this UserDefinedFrame instance
	 *
	 * [Generated from RDFReactor template rule #getallinverse1static] 
	 */
	public static ClosableIterator<org.ontoware.rdf2go.model.node.Resource> getAllUserDefinedFrame_Inverse( Model model, Object objectValue) {
		return Base.getAll_Inverse(model, ID3Audio.USERDEFINEDFRAME, objectValue);
	}

	/**
	 * @return all A's as RDF resources, that have a relation 'UserDefinedFrame' to this UserDefinedFrame instance
	 *
	 * [Generated from RDFReactor template rule #getallinverse1dynamic] 
	 */
	public ClosableIterator<org.ontoware.rdf2go.model.node.Resource> getAllUserDefinedFrame_Inverse() {
		return Base.getAll_Inverse(this.model, ID3Audio.USERDEFINEDFRAME, this.getResource() );
	}

	/**
	 * @param model an RDF2Go model
	 * @param objectValue
	 * @return all A's as a ReactorResult, that have a relation 'UserDefinedFrame' to this UserDefinedFrame instance
	 *
	 * [Generated from RDFReactor template rule #getallinverse-as1static] 
	 */
	public static ReactorResult<org.ontoware.rdf2go.model.node.Resource> getAllUserDefinedFrame_Inverse_as( Model model, Object objectValue) {
		return Base.getAll_Inverse_as(model, ID3Audio.USERDEFINEDFRAME, objectValue, org.ontoware.rdf2go.model.node.Resource.class);
	}



    /**
     * Check if org.ontoware.rdfreactor.generator.java.JProperty@42aec705 has at least one value set 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return true if this property has at least one value
	 *
	 * [Generated from RDFReactor template rule #get0has-static] 
     */
	public static boolean hasUserDefinedFrameDescription(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.has(model, instanceResource, USERDEFINEDFRAMEDESCRIPTION);
	}

    /**
     * Check if org.ontoware.rdfreactor.generator.java.JProperty@42aec705 has at least one value set 
     * @return true if this property has at least one value
	 *
	 * [Generated from RDFReactor template rule #get0has-dynamic] 
     */
	public boolean hasUserDefinedFrameDescription() {
		return Base.has(this.model, this.getResource(), USERDEFINEDFRAMEDESCRIPTION);
	}

    /**
     * Check if org.ontoware.rdfreactor.generator.java.JProperty@42aec705 has the given value (maybe among other values).  
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be checked
     * @return true if this property contains (maybe among other) the given value
	 *
	 * [Generated from RDFReactor template rule #get0has-value-static] 
     */
	public static boolean hasUserDefinedFrameDescription(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, org.ontoware.rdf2go.model.node.Node value ) {
		return Base.hasValue(model, instanceResource, USERDEFINEDFRAMEDESCRIPTION);
	}

    /**
     * Check if org.ontoware.rdfreactor.generator.java.JProperty@42aec705 has the given value (maybe among other values).  
	 * @param value the value to be checked
     * @return true if this property contains (maybe among other) the given value
	 *
	 * [Generated from RDFReactor template rule #get0has-value-dynamic] 
     */
	public boolean hasUserDefinedFrameDescription( org.ontoware.rdf2go.model.node.Node value ) {
		return Base.hasValue(this.model, this.getResource(), USERDEFINEDFRAMEDESCRIPTION);
	}

     /**
     * Get all values of property UserDefinedFrameDescription as an Iterator over RDF2Go nodes 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return a ClosableIterator of RDF2Go Nodes
	 *
	 * [Generated from RDFReactor template rule #get7static] 
     */
	public static ClosableIterator<org.ontoware.rdf2go.model.node.Node> getAllUserDefinedFrameDescription_asNode(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.getAll_asNode(model, instanceResource, USERDEFINEDFRAMEDESCRIPTION);
	}
	
    /**
     * Get all values of property UserDefinedFrameDescription as a ReactorResult of RDF2Go nodes 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return a List of RDF2Go Nodes
	 *
	 * [Generated from RDFReactor template rule #get7static-reactor-result] 
     */
	public static ReactorResult<org.ontoware.rdf2go.model.node.Node> getAllUserDefinedFrameDescription_asNode_(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.getAll_as(model, instanceResource, USERDEFINEDFRAMEDESCRIPTION, org.ontoware.rdf2go.model.node.Node.class);
	}

    /**
     * Get all values of property UserDefinedFrameDescription as an Iterator over RDF2Go nodes 
     * @return a ClosableIterator of RDF2Go Nodes
	 *
	 * [Generated from RDFReactor template rule #get8dynamic] 
     */
	public ClosableIterator<org.ontoware.rdf2go.model.node.Node> getAllUserDefinedFrameDescription_asNode() {
		return Base.getAll_asNode(this.model, this.getResource(), USERDEFINEDFRAMEDESCRIPTION);
	}

    /**
     * Get all values of property UserDefinedFrameDescription as a ReactorResult of RDF2Go nodes 
     * @return a List of RDF2Go Nodes
	 *
	 * [Generated from RDFReactor template rule #get8dynamic-reactor-result] 
     */
	public ReactorResult<org.ontoware.rdf2go.model.node.Node> getAllUserDefinedFrameDescription_asNode_() {
		return Base.getAll_as(this.model, this.getResource(), USERDEFINEDFRAMEDESCRIPTION, org.ontoware.rdf2go.model.node.Node.class);
	}
     /**
     * Get all values of property UserDefinedFrameDescription     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return a ClosableIterator of $type
	 *
	 * [Generated from RDFReactor template rule #get11static] 
     */
	public static ClosableIterator<java.lang.String> getAllUserDefinedFrameDescription(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.getAll(model, instanceResource, USERDEFINEDFRAMEDESCRIPTION, java.lang.String.class);
	}
	
    /**
     * Get all values of property UserDefinedFrameDescription as a ReactorResult of java.lang.String 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return a ReactorResult of $type which can conveniently be converted to iterator, list or array
	 *
	 * [Generated from RDFReactor template rule #get11static-reactorresult] 
     */
	public static ReactorResult<java.lang.String> getAllUserDefinedFrameDescription_as(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.getAll_as(model, instanceResource, USERDEFINEDFRAMEDESCRIPTION, java.lang.String.class);
	}

    /**
     * Get all values of property UserDefinedFrameDescription     * @return a ClosableIterator of $type
	 *
	 * [Generated from RDFReactor template rule #get12dynamic] 
     */
	public ClosableIterator<java.lang.String> getAllUserDefinedFrameDescription() {
		return Base.getAll(this.model, this.getResource(), USERDEFINEDFRAMEDESCRIPTION, java.lang.String.class);
	}

    /**
     * Get all values of property UserDefinedFrameDescription as a ReactorResult of java.lang.String 
     * @return a ReactorResult of $type which can conveniently be converted to iterator, list or array
	 *
	 * [Generated from RDFReactor template rule #get12dynamic-reactorresult] 
     */
	public ReactorResult<java.lang.String> getAllUserDefinedFrameDescription_as() {
		return Base.getAll_as(this.model, this.getResource(), USERDEFINEDFRAMEDESCRIPTION, java.lang.String.class);
	}
 
    /**
     * Adds a value to property UserDefinedFrameDescription as an RDF2Go node 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #add1static] 
     */
	public static void addUserDefinedFrameDescription( Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, org.ontoware.rdf2go.model.node.Node value) {
		Base.add(model, instanceResource, USERDEFINEDFRAMEDESCRIPTION, value);
	}
	
    /**
     * Adds a value to property UserDefinedFrameDescription as an RDF2Go node 
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #add1dynamic] 
     */
	public void addUserDefinedFrameDescription( org.ontoware.rdf2go.model.node.Node value) {
		Base.add(this.model, this.getResource(), USERDEFINEDFRAMEDESCRIPTION, value);
	}
    /**
     * Adds a value to property UserDefinedFrameDescription from an instance of java.lang.String 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 *
	 * [Generated from RDFReactor template rule #add3static] 
     */
	public static void addUserDefinedFrameDescription(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, java.lang.String value) {
		Base.add(model, instanceResource, USERDEFINEDFRAMEDESCRIPTION, value);
	}
	
    /**
     * Adds a value to property UserDefinedFrameDescription from an instance of java.lang.String 
	 *
	 * [Generated from RDFReactor template rule #add4dynamic] 
     */
	public void addUserDefinedFrameDescription(java.lang.String value) {
		Base.add(this.model, this.getResource(), USERDEFINEDFRAMEDESCRIPTION, value);
	}
  

    /**
     * Sets a value of property UserDefinedFrameDescription from an RDF2Go node.
     * First, all existing values are removed, then this value is added.
     * Cardinality constraints are not checked, but this method exists only for properties with
     * no minCardinality or minCardinality == 1.
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be set
	 *
	 * [Generated from RDFReactor template rule #set1static] 
     */
	public static void setUserDefinedFrameDescription( Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, org.ontoware.rdf2go.model.node.Node value) {
		Base.set(model, instanceResource, USERDEFINEDFRAMEDESCRIPTION, value);
	}
	
    /**
     * Sets a value of property UserDefinedFrameDescription from an RDF2Go node.
     * First, all existing values are removed, then this value is added.
     * Cardinality constraints are not checked, but this method exists only for properties with
     * no minCardinality or minCardinality == 1.
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #set1dynamic] 
     */
	public void setUserDefinedFrameDescription( org.ontoware.rdf2go.model.node.Node value) {
		Base.set(this.model, this.getResource(), USERDEFINEDFRAMEDESCRIPTION, value);
	}
    /**
     * Sets a value of property UserDefinedFrameDescription from an instance of java.lang.String 
     * First, all existing values are removed, then this value is added.
     * Cardinality constraints are not checked, but this method exists only for properties with
     * no minCardinality or minCardinality == 1.
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #set3static] 
     */
	public static void setUserDefinedFrameDescription(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, java.lang.String value) {
		Base.set(model, instanceResource, USERDEFINEDFRAMEDESCRIPTION, value);
	}
	
    /**
     * Sets a value of property UserDefinedFrameDescription from an instance of java.lang.String 
     * First, all existing values are removed, then this value is added.
     * Cardinality constraints are not checked, but this method exists only for properties with
     * no minCardinality or minCardinality == 1.
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #set4dynamic] 
     */
	public void setUserDefinedFrameDescription(java.lang.String value) {
		Base.set(this.model, this.getResource(), USERDEFINEDFRAMEDESCRIPTION, value);
	}
  


    /**
     * Removes a value of property UserDefinedFrameDescription as an RDF2Go node 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be removed
	 *
	 * [Generated from RDFReactor template rule #remove1static] 
     */
	public static void removeUserDefinedFrameDescription( Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, org.ontoware.rdf2go.model.node.Node value) {
		Base.remove(model, instanceResource, USERDEFINEDFRAMEDESCRIPTION, value);
	}
	
    /**
     * Removes a value of property UserDefinedFrameDescription as an RDF2Go node
	 * @param value the value to be removed
	 *
	 * [Generated from RDFReactor template rule #remove1dynamic] 
     */
	public void removeUserDefinedFrameDescription( org.ontoware.rdf2go.model.node.Node value) {
		Base.remove(this.model, this.getResource(), USERDEFINEDFRAMEDESCRIPTION, value);
	}
    /**
     * Removes a value of property UserDefinedFrameDescription given as an instance of java.lang.String 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be removed
	 *
	 * [Generated from RDFReactor template rule #remove3static] 
     */
	public static void removeUserDefinedFrameDescription(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, java.lang.String value) {
		Base.remove(model, instanceResource, USERDEFINEDFRAMEDESCRIPTION, value);
	}
	
    /**
     * Removes a value of property UserDefinedFrameDescription given as an instance of java.lang.String 
	 * @param value the value to be removed
	 *
	 * [Generated from RDFReactor template rule #remove4dynamic] 
     */
	public void removeUserDefinedFrameDescription(java.lang.String value) {
		Base.remove(this.model, this.getResource(), USERDEFINEDFRAMEDESCRIPTION, value);
	}
  
    /**
     * Removes all values of property UserDefinedFrameDescription     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 *
	 * [Generated from RDFReactor template rule #removeall1static] 
     */
	public static void removeAllUserDefinedFrameDescription( Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		Base.removeAll(model, instanceResource, USERDEFINEDFRAMEDESCRIPTION);
	}
	
    /**
     * Removes all values of property UserDefinedFrameDescription	 *
	 * [Generated from RDFReactor template rule #removeall1dynamic] 
     */
	public void removeAllUserDefinedFrameDescription() {
		Base.removeAll(this.model, this.getResource(), USERDEFINEDFRAMEDESCRIPTION);
	}
     /**
     * Check if org.ontoware.rdfreactor.generator.java.JProperty@2e93d13f has at least one value set 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return true if this property has at least one value
	 *
	 * [Generated from RDFReactor template rule #get0has-static] 
     */
	public static boolean hasUserDefinedFrameValue(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.has(model, instanceResource, USERDEFINEDFRAMEVALUE);
	}

    /**
     * Check if org.ontoware.rdfreactor.generator.java.JProperty@2e93d13f has at least one value set 
     * @return true if this property has at least one value
	 *
	 * [Generated from RDFReactor template rule #get0has-dynamic] 
     */
	public boolean hasUserDefinedFrameValue() {
		return Base.has(this.model, this.getResource(), USERDEFINEDFRAMEVALUE);
	}

    /**
     * Check if org.ontoware.rdfreactor.generator.java.JProperty@2e93d13f has the given value (maybe among other values).  
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be checked
     * @return true if this property contains (maybe among other) the given value
	 *
	 * [Generated from RDFReactor template rule #get0has-value-static] 
     */
	public static boolean hasUserDefinedFrameValue(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, org.ontoware.rdf2go.model.node.Node value ) {
		return Base.hasValue(model, instanceResource, USERDEFINEDFRAMEVALUE);
	}

    /**
     * Check if org.ontoware.rdfreactor.generator.java.JProperty@2e93d13f has the given value (maybe among other values).  
	 * @param value the value to be checked
     * @return true if this property contains (maybe among other) the given value
	 *
	 * [Generated from RDFReactor template rule #get0has-value-dynamic] 
     */
	public boolean hasUserDefinedFrameValue( org.ontoware.rdf2go.model.node.Node value ) {
		return Base.hasValue(this.model, this.getResource(), USERDEFINEDFRAMEVALUE);
	}

     /**
     * Get all values of property UserDefinedFrameValue as an Iterator over RDF2Go nodes 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return a ClosableIterator of RDF2Go Nodes
	 *
	 * [Generated from RDFReactor template rule #get7static] 
     */
	public static ClosableIterator<org.ontoware.rdf2go.model.node.Node> getAllUserDefinedFrameValue_asNode(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.getAll_asNode(model, instanceResource, USERDEFINEDFRAMEVALUE);
	}
	
    /**
     * Get all values of property UserDefinedFrameValue as a ReactorResult of RDF2Go nodes 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return a List of RDF2Go Nodes
	 *
	 * [Generated from RDFReactor template rule #get7static-reactor-result] 
     */
	public static ReactorResult<org.ontoware.rdf2go.model.node.Node> getAllUserDefinedFrameValue_asNode_(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.getAll_as(model, instanceResource, USERDEFINEDFRAMEVALUE, org.ontoware.rdf2go.model.node.Node.class);
	}

    /**
     * Get all values of property UserDefinedFrameValue as an Iterator over RDF2Go nodes 
     * @return a ClosableIterator of RDF2Go Nodes
	 *
	 * [Generated from RDFReactor template rule #get8dynamic] 
     */
	public ClosableIterator<org.ontoware.rdf2go.model.node.Node> getAllUserDefinedFrameValue_asNode() {
		return Base.getAll_asNode(this.model, this.getResource(), USERDEFINEDFRAMEVALUE);
	}

    /**
     * Get all values of property UserDefinedFrameValue as a ReactorResult of RDF2Go nodes 
     * @return a List of RDF2Go Nodes
	 *
	 * [Generated from RDFReactor template rule #get8dynamic-reactor-result] 
     */
	public ReactorResult<org.ontoware.rdf2go.model.node.Node> getAllUserDefinedFrameValue_asNode_() {
		return Base.getAll_as(this.model, this.getResource(), USERDEFINEDFRAMEVALUE, org.ontoware.rdf2go.model.node.Node.class);
	}
     /**
     * Get all values of property UserDefinedFrameValue     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return a ClosableIterator of $type
	 *
	 * [Generated from RDFReactor template rule #get11static] 
     */
	public static ClosableIterator<java.lang.String> getAllUserDefinedFrameValue(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.getAll(model, instanceResource, USERDEFINEDFRAMEVALUE, java.lang.String.class);
	}
	
    /**
     * Get all values of property UserDefinedFrameValue as a ReactorResult of java.lang.String 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return a ReactorResult of $type which can conveniently be converted to iterator, list or array
	 *
	 * [Generated from RDFReactor template rule #get11static-reactorresult] 
     */
	public static ReactorResult<java.lang.String> getAllUserDefinedFrameValue_as(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.getAll_as(model, instanceResource, USERDEFINEDFRAMEVALUE, java.lang.String.class);
	}

    /**
     * Get all values of property UserDefinedFrameValue     * @return a ClosableIterator of $type
	 *
	 * [Generated from RDFReactor template rule #get12dynamic] 
     */
	public ClosableIterator<java.lang.String> getAllUserDefinedFrameValue() {
		return Base.getAll(this.model, this.getResource(), USERDEFINEDFRAMEVALUE, java.lang.String.class);
	}

    /**
     * Get all values of property UserDefinedFrameValue as a ReactorResult of java.lang.String 
     * @return a ReactorResult of $type which can conveniently be converted to iterator, list or array
	 *
	 * [Generated from RDFReactor template rule #get12dynamic-reactorresult] 
     */
	public ReactorResult<java.lang.String> getAllUserDefinedFrameValue_as() {
		return Base.getAll_as(this.model, this.getResource(), USERDEFINEDFRAMEVALUE, java.lang.String.class);
	}
 
    /**
     * Adds a value to property UserDefinedFrameValue as an RDF2Go node 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #add1static] 
     */
	public static void addUserDefinedFrameValue( Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, org.ontoware.rdf2go.model.node.Node value) {
		Base.add(model, instanceResource, USERDEFINEDFRAMEVALUE, value);
	}
	
    /**
     * Adds a value to property UserDefinedFrameValue as an RDF2Go node 
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #add1dynamic] 
     */
	public void addUserDefinedFrameValue( org.ontoware.rdf2go.model.node.Node value) {
		Base.add(this.model, this.getResource(), USERDEFINEDFRAMEVALUE, value);
	}
    /**
     * Adds a value to property UserDefinedFrameValue from an instance of java.lang.String 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 *
	 * [Generated from RDFReactor template rule #add3static] 
     */
	public static void addUserDefinedFrameValue(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, java.lang.String value) {
		Base.add(model, instanceResource, USERDEFINEDFRAMEVALUE, value);
	}
	
    /**
     * Adds a value to property UserDefinedFrameValue from an instance of java.lang.String 
	 *
	 * [Generated from RDFReactor template rule #add4dynamic] 
     */
	public void addUserDefinedFrameValue(java.lang.String value) {
		Base.add(this.model, this.getResource(), USERDEFINEDFRAMEVALUE, value);
	}
  

    /**
     * Sets a value of property UserDefinedFrameValue from an RDF2Go node.
     * First, all existing values are removed, then this value is added.
     * Cardinality constraints are not checked, but this method exists only for properties with
     * no minCardinality or minCardinality == 1.
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be set
	 *
	 * [Generated from RDFReactor template rule #set1static] 
     */
	public static void setUserDefinedFrameValue( Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, org.ontoware.rdf2go.model.node.Node value) {
		Base.set(model, instanceResource, USERDEFINEDFRAMEVALUE, value);
	}
	
    /**
     * Sets a value of property UserDefinedFrameValue from an RDF2Go node.
     * First, all existing values are removed, then this value is added.
     * Cardinality constraints are not checked, but this method exists only for properties with
     * no minCardinality or minCardinality == 1.
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #set1dynamic] 
     */
	public void setUserDefinedFrameValue( org.ontoware.rdf2go.model.node.Node value) {
		Base.set(this.model, this.getResource(), USERDEFINEDFRAMEVALUE, value);
	}
    /**
     * Sets a value of property UserDefinedFrameValue from an instance of java.lang.String 
     * First, all existing values are removed, then this value is added.
     * Cardinality constraints are not checked, but this method exists only for properties with
     * no minCardinality or minCardinality == 1.
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #set3static] 
     */
	public static void setUserDefinedFrameValue(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, java.lang.String value) {
		Base.set(model, instanceResource, USERDEFINEDFRAMEVALUE, value);
	}
	
    /**
     * Sets a value of property UserDefinedFrameValue from an instance of java.lang.String 
     * First, all existing values are removed, then this value is added.
     * Cardinality constraints are not checked, but this method exists only for properties with
     * no minCardinality or minCardinality == 1.
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #set4dynamic] 
     */
	public void setUserDefinedFrameValue(java.lang.String value) {
		Base.set(this.model, this.getResource(), USERDEFINEDFRAMEVALUE, value);
	}
  


    /**
     * Removes a value of property UserDefinedFrameValue as an RDF2Go node 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be removed
	 *
	 * [Generated from RDFReactor template rule #remove1static] 
     */
	public static void removeUserDefinedFrameValue( Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, org.ontoware.rdf2go.model.node.Node value) {
		Base.remove(model, instanceResource, USERDEFINEDFRAMEVALUE, value);
	}
	
    /**
     * Removes a value of property UserDefinedFrameValue as an RDF2Go node
	 * @param value the value to be removed
	 *
	 * [Generated from RDFReactor template rule #remove1dynamic] 
     */
	public void removeUserDefinedFrameValue( org.ontoware.rdf2go.model.node.Node value) {
		Base.remove(this.model, this.getResource(), USERDEFINEDFRAMEVALUE, value);
	}
    /**
     * Removes a value of property UserDefinedFrameValue given as an instance of java.lang.String 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be removed
	 *
	 * [Generated from RDFReactor template rule #remove3static] 
     */
	public static void removeUserDefinedFrameValue(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, java.lang.String value) {
		Base.remove(model, instanceResource, USERDEFINEDFRAMEVALUE, value);
	}
	
    /**
     * Removes a value of property UserDefinedFrameValue given as an instance of java.lang.String 
	 * @param value the value to be removed
	 *
	 * [Generated from RDFReactor template rule #remove4dynamic] 
     */
	public void removeUserDefinedFrameValue(java.lang.String value) {
		Base.remove(this.model, this.getResource(), USERDEFINEDFRAMEVALUE, value);
	}
  
    /**
     * Removes all values of property UserDefinedFrameValue     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 *
	 * [Generated from RDFReactor template rule #removeall1static] 
     */
	public static void removeAllUserDefinedFrameValue( Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		Base.removeAll(model, instanceResource, USERDEFINEDFRAMEVALUE);
	}
	
    /**
     * Removes all values of property UserDefinedFrameValue	 *
	 * [Generated from RDFReactor template rule #removeall1dynamic] 
     */
	public void removeAllUserDefinedFrameValue() {
		Base.removeAll(this.model, this.getResource(), USERDEFINEDFRAMEVALUE);
	}
 }