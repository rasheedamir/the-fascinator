/* 
 * The Fascinator - Indexer
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
package au.edu.usq.fascinator.indexer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.request.DirectXmlRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.JSONResponseWriter;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.io.SAXReader;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.access.AccessControlException;
import au.edu.usq.fascinator.api.access.AccessControlManager;
import au.edu.usq.fascinator.api.access.AccessControlSchema;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.indexer.SearchRequest;
import au.edu.usq.fascinator.api.indexer.rule.RuleException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfig;

public class SolrIndexer implements Indexer {

    private static final String DEFAULT_SOLR_HOME = System
            .getProperty("user.home")
            + File.separator + ".fascinator" + File.separator + "solr";

    private Logger log = LoggerFactory.getLogger(SolrIndexer.class);

    private JsonConfig config;

    private AccessControlManager access;

    private Storage storage;

    private SolrServer solr;

    private CoreContainer coreContainer;

    private boolean autoCommit;

    private String propertiesId;

    private List<File> tempFiles;

    private SAXReader saxReader;

    private Map<String, String> namespaces;

    private String username;

    private String password;

    private boolean loaded;

    public String getId() {
        return "solr";
    }

    public String getName() {
        return "Apache Solr Indexer";
    }

    public SolrIndexer() {
        loaded = false;
    }

    public void init(File jsonFile) throws IndexerException {
        if (!loaded) {
            loaded = true;
            try {
                config = new JsonConfig(jsonFile);
            } catch (IOException ioe) {
                throw new IndexerException(ioe);
            }

            String accessControlType = "accessmanager";
            try {
                access = PluginManager.getAccessManager(accessControlType);
                access.init(jsonFile);
            } catch (PluginException pe) {
                log.error("Failed to load access manager: {}", accessControlType);
            }

            String storageType = config.get("storage/type");
            try {
                storage = PluginManager.getStorage(storageType);
                storage.init(jsonFile);
            } catch (PluginException pe) {
                log.error("Failed to load storage plugin: {}", storageType);
            }

            try {
                boolean isEmbedded = Boolean.parseBoolean(config
                        .get("indexer/solr/embedded"));
                if (isEmbedded) {
                    String home = config.get("indexer/solr/home",
                            DEFAULT_SOLR_HOME);
                    log.info("home={}", home);
                    File homeDir = new File(home);
                    if (!homeDir.exists()) {
                        log.info("Preparing default Solr home...");
                        prepareHome(homeDir);
                    }
                    System.setProperty("solr.solr.home", homeDir
                            .getAbsolutePath());
                    File coreXmlFile = new File(homeDir, "solr.xml");
                    coreContainer = new CoreContainer(
                            homeDir.getAbsolutePath(), coreXmlFile);
                    for (SolrCore core : coreContainer.getCores()) {
                        log.info("loaded core: {}", core.getName());
                    }
                    solr = new EmbeddedSolrServer(coreContainer, "search");
                } else {
                    URI solrUri = new URI(config.get("indexer/solr/uri"));
                    solr = new CommonsHttpSolrServer(solrUri.toURL());
                    username = config.get("indexer/solr/username");
                    password = config.get("indexer/solr/password");
                    if (username != null && password != null) {
                        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                                username, password);
                        HttpClient hc = ((CommonsHttpSolrServer) solr)
                                .getHttpClient();
                        hc.getParams().setAuthenticationPreemptive(true);
                        hc.getState()
                                .setCredentials(AuthScope.ANY, credentials);
                    }
                }
            } catch (MalformedURLException mue) {
                log.error("Malformed URL", mue);
            } catch (URISyntaxException urise) {
                log.error("Invalid URI", urise);
            } catch (IOException ioe) {
                log.error("Failed to read Solr configuration", ioe);
            } catch (ParserConfigurationException pce) {
                log.error("Failed to parse Solr configuration", pce);
            } catch (SAXException saxe) {
                log.error("Failed to load Solr configuration", saxe);
            }

            autoCommit = Boolean.parseBoolean(config.get(
                    "indexer/solr/autocommit", "true"));
            propertiesId = config.get("indexer/propertiesId", "SOF-META");

            namespaces = new HashMap<String, String>();
            DocumentFactory docFactory = new DocumentFactory();
            docFactory.setXPathNamespaceURIs(namespaces);

            saxReader = new SAXReader(docFactory);
        }
        loaded = true;
    }

    private void prepareHome(File homeDir) throws IOException {
        try {
            URI defaultSolrUri = getClass().getResource("/solr").toURI();
            log.info("defaultSolrUri={},url={}", defaultSolrUri, defaultSolrUri
                    .getScheme());
            FileUtils.copyURLToFile(defaultSolrUri.toURL(), homeDir);
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() throws PluginException {
        if (coreContainer != null) {
            coreContainer.shutdown();
        }
    }

    public Storage getStorage() {
        return storage;
    }

    @Override
    public void search(SearchRequest request, OutputStream response)
            throws IndexerException {
        if (solr instanceof EmbeddedSolrServer) {
            EmbeddedSolrServer ess = ((EmbeddedSolrServer) solr);
            SolrQuery query = new SolrQuery();
            query.setQuery(request.getQuery());
            for (String name : request.getParamsMap().keySet()) {
                Set<String> values = request.getParams(name);
                query.setParam(name, values.toArray(new String[] {}));
            }
            try {
                QueryResponse resp = ess.query(query);
                JSONResponseWriter jrw = new JSONResponseWriter();
                jrw.init(resp.getResponse());
                SolrQueryRequest a = new LocalSolrQueryRequest(coreContainer
                        .getCore("search"), query);
                SolrQueryResponse b = new SolrQueryResponse();
                b.setAllValues(resp.getResponse());
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Writer w = new OutputStreamWriter(out);
                jrw.write(w, a, b);
                w.close();
                log.info("out={}", out.toString("UTF-8"));
                IOUtils.copy(new ByteArrayInputStream(out.toByteArray()),
                        response);
            } catch (SolrServerException sse) {
                sse.printStackTrace();
                throw new IndexerException(sse);
            } catch (IOException ioe) {
                throw new IndexerException(ioe);
            }
        } else {
            SolrSearcher searcher = new SolrSearcher(
                    ((CommonsHttpSolrServer) solr).getBaseURL());
            if (username != null && password != null) {
                searcher.authenticate(username, password);
            }
            InputStream result;
            try {
                StringBuilder extras = new StringBuilder();
                for (String name : request.getParamsMap().keySet()) {
                    for (String param : request.getParams(name)) {
                        extras.append(name);
                        extras.append("=");
                        extras.append(URLEncoder.encode(param, "UTF-8"));
                        extras.append("&");
                    }
                }
                extras.append("wt=json");
                result = searcher.get(request.getQuery(), extras.toString(),
                        false);
                IOUtils.copy(result, response);
                result.close();
            } catch (IOException ioe) {
                throw new IndexerException(ioe);
            }
        }
    }

    @Override
    public void remove(String oid) throws IndexerException {
        log.debug("Deleting " + oid + " from index");
        try {
            solr.deleteByQuery("storage_id:\"" + oid + "\"");
            solr.commit();
        } catch (SolrServerException sse) {
            throw new IndexerException(sse);
        } catch (IOException ioe) {
            throw new IndexerException(ioe);
        }
    }

    @Override
    public void remove(String oid, String pid) throws IndexerException {
        log.debug("Deleting {}::{} from index", oid, pid);
        try {
            solr.deleteByQuery("storage_id:\"" + oid + "\" AND identifer:\""
                    + pid + "\"");
            solr.commit();
        } catch (SolrServerException sse) {
            throw new IndexerException(sse);
        } catch (IOException ioe) {
            throw new IndexerException(ioe);
        }
    }

    @Override
    public void index(String oid) throws IndexerException {
        List<Payload> payloadList = storage.getObject(oid).getPayloadList();
        for (Payload payload : payloadList) {
            index(oid, payload.getId());
        }
    }

    @Override
    public void index(String oid, String pid) throws IndexerException {
        if (propertiesId.equals(pid)) {
            return;
        }
        log.info("Indexing " + oid + "/" + pid);

        // get the indexer properties or we can't index
        Properties props = getIndexerProperties(oid);
        if (props == null) {
            log.warn("Indexer properties not found, object not indexed");
            throw new IndexerException("Indexer properties not found");
        }

        try {
            // create the item for indexing
            String objectId = props.getProperty("oid", oid);
            DigitalObject object = storage.getObject(objectId);

            // get the indexer rules
            String rulesPid = props.getProperty("rulesOid");
            File rules = createTempFile("rules", ".script");
            FileOutputStream rulesOut = new FileOutputStream(rules);
            Payload rulesScript = storage.getPayload(rulesPid, props
                    .getProperty("rulesPid"));
            IOUtils.copy(rulesScript.getInputStream(), rulesOut);
            rulesOut.close();

            // primary metadata datastream
            String metaPid = props.getProperty("metaPid", "DC");
            object = new IndexerDigitalObject(object, metaPid);

            // index the object
            String set = null; // TODO
            File solrFile = null;
            if (metaPid.equals(pid)) {
                solrFile = indexMetadata(object, oid, set, rules, props);
            } else {
                solrFile = indexDatastream(object, oid, pid, rules, props);
            }
            if (solrFile != null) {
                InputStream inputDoc = new FileInputStream(solrFile);
                String xml = IOUtils.toString(inputDoc, "UTF-8");
                inputDoc.close();
                SolrRequest update = new DirectXmlRequest("/update", xml);
                solr.request(update);
                if (autoCommit) {
                    solr.commit();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Indexing failed!\n-----\n{}\n-----\n", e.getMessage());
        } finally {
            cleanupTempFiles();
        }
    }

    private File indexMetadata(DigitalObject item, String pid, String set,
            File rulesFile, Properties props) throws IOException,
            StorageException, RuleException {
        log.info("Indexing metadata...");
        Payload ds = item.getPayload(props.getProperty("metaPid"));
        InputStreamReader in = new InputStreamReader(ds.getInputStream(),
                "UTF-8");
        return index(item, pid, null, set, in, rulesFile, props);
    }

    private File indexDatastream(DigitalObject item, String pid, String dsId,
            File rulesFile, Properties props) throws IOException, RuleException {
        log.info("Indexing datastream...");
        Reader in = new StringReader("<add><doc/></add>");
        return index(item, pid, dsId, null, in, rulesFile, props);
    }

    private File index(DigitalObject object, String sid, String pid,
            String set, Reader in, File ruleScript, Properties props)
            throws IOException, RuleException {
        File solrFile = createTempFile("solr", ".xml");
        Writer out = new OutputStreamWriter(new FileOutputStream(solrFile),
                "UTF-8");
        try {
            String engineName = props.getProperty("scriptType", "python");
            ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
            ScriptEngine scriptEngine = scriptEngineManager
                    .getEngineByName(engineName);
            if (scriptEngine == null) {
                throw new RuleException("No script engine found for '"
                        + engineName + "'");
            }
            RuleManager rules = new RuleManager();
            scriptEngine.put("indexer", this);
            scriptEngine.put("rules", rules);
            scriptEngine.put("object", object);
            scriptEngine.put("payloadId", pid);
            scriptEngine.put("storageId", sid);
            scriptEngine.put("params", props);
            scriptEngine.put("isMetadata", pid == null);
            // TODO add required solr fields?
            scriptEngine.eval(new FileReader(ruleScript));
            rules.run(in, out);
            if (rules.cancelled()) {
                log.info("Indexing rules were cancelled");
                return null;
            }
        } catch (Exception e) {
            throw new RuleException(e);
        } finally {
            in.close();
            out.close();
        }
        return solrFile;
    }

    private Properties getIndexerProperties(String oid) {
        try {
            Payload sofMeta = storage.getPayload(oid, propertiesId);
            Properties props = new Properties();
            props.load(sofMeta.getInputStream());
            return props;
        } catch (Exception ioe) {
            log.warn("Failed to load properties", ioe);
        }
        return null;
    }

    private File createTempFile(String prefix, String postfix)
            throws IOException {
        File tempFile = File.createTempFile(prefix, postfix);
        if (tempFiles == null) {
            tempFiles = Collections.synchronizedList(new ArrayList<File>());
        }
        tempFiles.add(tempFile);
        return tempFile;
    }

    private void cleanupTempFiles() {
        for (File tempFile : tempFiles) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
        tempFiles = Collections.synchronizedList(new ArrayList<File>());
    }

    // Helper methods

    public InputStream getResource(String path) {
        return getClass().getResourceAsStream(path);
    }

    public Document getXmlDocument(Payload payload) {
        try {
            return getXmlDocument(payload.getInputStream());
        } catch (IOException ioe) {
            log.error("Failed to parse XML", ioe);
        }
        return null;
    }

    public Document getXmlDocument(InputStream xmlIn) {
        Reader reader = null;
        try {
            reader = new InputStreamReader(xmlIn, "UTF-8");
            return saxReader.read(reader);
        } catch (UnsupportedEncodingException uee) {
        } catch (DocumentException de) {
            log.error("Failed to parse XML", de);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                }
            }
        }
        return null;
    }

    public Model getRdfModel(Payload payload) {
        try {
            return getRdfModel(payload.getInputStream());
        } catch (IOException ioe) {
            log.info("Failed to read payload stream", ioe);
        }
        return null;
    }

    public Model getRdfModel(InputStream rdfIn) {
        Model model = null;
        Reader reader = null;
        try {
            reader = new InputStreamReader(rdfIn, "UTF-8");
            model = RDF2Go.getModelFactory().createModel();
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

    public AccessControlSchema getAccessSchema(String plugin) {
        if (access == null) return null;
        access.setActivePlugin(plugin);
        return access.getEmptySchema();
    }

    public void setAccessSchema(AccessControlSchema schema, String plugin) {
        if (access == null) return;

        try {
            access.setActivePlugin(plugin);
            access.applySchema(schema);
        } catch (AccessControlException ex) {
            log.error("Failed to query security plugin for roles", ex);
        }
    }

    public List<String> getRolesWithAccess(String recordId) {
        if (access == null) {
            return null;
        }
        try {
            return access.getRoles(recordId);
        } catch (AccessControlException ex) {
            log.error("Failed to query security plugin for roles", ex);
            return null;
        }
    }

    public void registerNamespace(String prefix, String uri) {
        namespaces.put(prefix, uri);
    }

    public void unregisterNamespace(String prefix) {
        namespaces.remove(prefix);
    }

    @Override
    public void init(String jsonString) throws PluginException {
        // TODO Auto-generated method stub

    }

}
