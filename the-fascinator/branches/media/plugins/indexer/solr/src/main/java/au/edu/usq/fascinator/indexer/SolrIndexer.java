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

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.indexer.SearchRequest;
import au.edu.usq.fascinator.api.indexer.rule.RuleException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.FascinatorHome;
import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.common.PythonUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.logging.Level;

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
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class SolrIndexer implements Indexer {

    private static final String DEFAULT_SOLR_HOME = FascinatorHome
            .getPath("solr");

    private static String DEFAULT_METADATA_PAYLOAD = "TF-OBJ-META";

    private Logger log = LoggerFactory.getLogger(SolrIndexer.class);
    private JsonConfig config;

    private Storage storage;

    private SolrServer solr;
    private SolrServer anotar;
    private CoreContainer coreContainer;
    private boolean autoCommit;
    private boolean anotarAutoCommit;

    private String propertiesId;

    private List<File> tempFiles;

    private String username;
    private String password;

    private boolean loaded;

    private Map<String, String> customParams;

    private PythonInterpreter python;
    private PythonUtils pyUtils;

    @Override
    public String getId() {
        return "solr";
    }

    @Override
    public String getName() {
        return "Apache Solr Indexer";
    }

    public SolrIndexer() {
        loaded = false;
    }

    @Override
    public void init(String jsonString) throws IndexerException {
        try {
            config = new JsonConfig(new ByteArrayInputStream(jsonString
                    .getBytes("UTF-8")));
            init();
        } catch (UnsupportedEncodingException e) {
            throw new IndexerException(e);
        } catch (IOException e) {
            throw new IndexerException(e);
        }
    }

    @Override
    public void init(File jsonFile) throws IndexerException {
        try {
            config = new JsonConfig(jsonFile);
            init();
        } catch (IOException ioe) {
            throw new IndexerException(ioe);
        }
    }

    private void init() throws IndexerException {
        if (!loaded) {
            loaded = true;

            String storageType = config.get("storage/type");
            try {
                storage = PluginManager.getStorage(storageType);
                storage.init(config.toString());
            } catch (PluginException pe) {
                throw new IndexerException(pe);
            }

            solr = initCore("solr");
            anotar = initCore("anotar");

            autoCommit = Boolean.parseBoolean(config.get(
                    "indexer/solr/autocommit", "true"));
            anotarAutoCommit = Boolean.parseBoolean(config.get(
                    "indexer/anotar/autocommit", "true"));
            propertiesId = config.get("indexer/propertiesId",
                    DEFAULT_METADATA_PAYLOAD);

            customParams = new HashMap<String, String>();

            python = new PythonInterpreter();
            try {
                pyUtils = new PythonUtils(config);
            } catch (PluginException ex) {
                throw new IndexerException(ex);
            }
        }
        loaded = true;
    }

    public void setCustomParam(String property, String value) {
        customParams.put(property, value);
    }

    private SolrServer initCore(String coreName) {
        try {
            boolean isEmbedded = Boolean.parseBoolean(config.get("indexer/"
                    + coreName + "/embedded"));
            if (isEmbedded) {
                /* TODO - Fix embedded for Solr 1.4 */
                String home = config.get("indexer/" + coreName + "/home",
                        DEFAULT_SOLR_HOME);
                log.info("home={}", home);
                File homeDir = new File(home);
                if (!homeDir.exists()) {
                    log.info("Preparing default Solr home...");
                    prepareHome(homeDir);
                }
                System.setProperty("solr.solr.home", homeDir.getAbsolutePath());
                File coreXmlFile = new File(homeDir, "solr.xml");
                coreContainer = new CoreContainer(homeDir.getAbsolutePath(),
                        coreXmlFile);
                for (SolrCore core : coreContainer.getCores()) {
                    log.info("loaded core: {}", core.getName());
                }
                return new EmbeddedSolrServer(coreContainer, "search");
            } else {
                URI solrUri = new URI(config
                        .get("indexer/" + coreName + "/uri"));
                CommonsHttpSolrServer thisCore = new CommonsHttpSolrServer(
                        solrUri.toURL());
                username = config.get("indexer/" + coreName + "/username");
                password = config.get("indexer/" + coreName + "/password");
                if (username != null && password != null) {
                    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                            username, password);
                    HttpClient hc = ((CommonsHttpSolrServer) solr)
                            .getHttpClient();
                    hc.getParams().setAuthenticationPreemptive(true);
                    hc.getState().setCredentials(AuthScope.ANY, credentials);
                }
                return thisCore;
            }
        } catch (MalformedURLException mue) {
            log.error(coreName + " : Malformed URL", mue);
        } catch (URISyntaxException urise) {
            log.error(coreName + " : Invalid URI", urise);
        } catch (IOException ioe) {
            log.error(coreName + " : Failed to read Solr configuration", ioe);
        } catch (ParserConfigurationException pce) {
            log.error(coreName + " : Failed to parse Solr configuration", pce);
        } catch (SAXException saxe) {
            log.error(coreName + " : Failed to load Solr configuration", saxe);
        }
        return null;
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
        pyUtils.shutdown();
        cleanupTempFiles();
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
        try {
            DigitalObject object = storage.getObject(oid);
            // Some workflow actions create payloads, so we can't iterate
            // directly against the object.
            String[] oldManifest = {};
            oldManifest = object.getPayloadIdList().toArray(oldManifest);
            for (String payloadId : oldManifest) {
                Payload payload = object.getPayload(payloadId);
                index(object, payload);
            }
        } catch (StorageException ex) {
            throw new IndexerException(ex);
        }
    }

    @Override
    public void index(String oid, String pid) throws IndexerException {
        try {
            DigitalObject object = storage.getObject(oid);
            Payload payload = object.getPayload(pid);
            index(object, payload);
        } catch (StorageException ex) {
            throw new IndexerException(ex);
        }
    }

    public void index(DigitalObject object, Payload payload)
            throws IndexerException {
        String oid = object.getId();
        String pid = payload.getId();

        // Don't proccess annotations through this function
        if (pid.startsWith("anotar.")) {
            annotate(object, payload);
            return;
        }
        log.info("Indexing OID:'" + oid + "', PID: '" + pid + "'");

        // get the indexer properties or we can't index
        Properties props;
        try {
            props = object.getMetadata();
        } catch (StorageException ex) {
            throw new IndexerException("Failed loading properties : ", ex);
        }

        try {
            // Get the harvester config
            String confOid = props.getProperty("jsonConfigOid");
            DigitalObject confObj = storage.getObject(confOid);
            Payload confPayload = confObj.getPayload(confObj.getSourceId());
            // Get the rules script
            String rulesOid = props.getProperty("rulesOid");
            DigitalObject rulesObj = storage.getObject(rulesOid);
            Payload rulesPayload = rulesObj.getPayload(rulesObj.getSourceId());

            // index the object
            File solrFile = index(object, payload, confPayload.open(),
                    rulesPayload.open(), props);

            if (solrFile != null) {
                InputStream inputDoc = new FileInputStream(solrFile);
                String xml = IOUtils.toString(inputDoc, "UTF-8");
                inputDoc.close();
                SolrRequest update = new DirectXmlRequest("/update", xml);
                solr.request(update);
                if (autoCommit) {
                    log.debug("Running forced commit!");
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

    @Override
    public void commit() {
        try {
            solr.commit();
        } catch (Exception e) {
            // TODO - what to do?
        }
    }

    @Override
    public void annotate(String oid, String pid) throws IndexerException {
        try {
            DigitalObject object = storage.getObject(oid);
            Payload payload = object.getPayload(pid);
            index(object, payload);
        } catch (StorageException ex) {
            throw new IndexerException(ex);
        }
    }

    private void annotate(DigitalObject object, Payload payload)
            throws IndexerException {
        String oid = object.getId();
        String pid = payload.getId();
        if (propertiesId.equals(pid)) {
            return;
        }

        try {
            // Get the rules file
            InputStream rulesStream = getClass().getResourceAsStream(
                    "/anotar.py");

            File solrFile = null;
            Properties props = new Properties();
            props.setProperty("metaPid", pid);

            solrFile = index(object, payload, null, rulesStream, props);
            if (solrFile != null) {
                InputStream inputDoc = new FileInputStream(solrFile);
                String xml = IOUtils.toString(inputDoc, "UTF-8");
                inputDoc.close();
                SolrRequest update = new DirectXmlRequest("/update", xml);
                anotar.request(update);
                if (anotarAutoCommit) {
                    anotar.commit();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Indexing failed!\n-----\n{}\n-----\n", e.getMessage());
        } finally {
            cleanupTempFiles();
        }
    }

    @Override
    public void annotateSearch(SearchRequest request, OutputStream response)
            throws IndexerException {
        if (anotar instanceof EmbeddedSolrServer) {
            EmbeddedSolrServer ess = ((EmbeddedSolrServer) anotar);
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
                    ((CommonsHttpSolrServer) anotar).getBaseURL());
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

    private File index(DigitalObject object, Payload payload,
            InputStream inConf, InputStream inRules, Properties props)
            throws IOException, RuleException {
        Reader in = new StringReader("<add><doc/></add>");
        return index(object, payload, in, inConf, inRules, props);
    }

    private File index(DigitalObject object, Payload payload, Reader in,
            InputStream inConf, InputStream inRules, Properties props)
            throws IOException, RuleException {
        File solrFile = createTempFile("solr", ".xml");
        Writer out = new OutputStreamWriter(new FileOutputStream(solrFile),
                "UTF-8");
        InputStream rulesIn = null;
        try {
            // Make our harvest config more useful
            JsonConfigHelper jsonConfig = null;
            if (inConf == null) {
                jsonConfig = new JsonConfigHelper();
            } else {
                jsonConfig = new JsonConfigHelper(inConf);
                inConf.close();
            }

            RuleManager rules = new RuleManager();
            python.set("indexer", this);
            python.set("jsonConfig", jsonConfig);
            python.set("rules", rules);
            python.set("object", object);
            python.set("payload", payload);
            python.set("params", props);
            python.set("inputReader", in);
            python.set("pyUtils", pyUtils);
            python.execfile(inRules);
            rules.run(in, out);
            if (rules.cancelled()) {
                log.info("Indexing rules were cancelled");
                return null;
            }
            python.cleanup();
        } catch (Exception e) {
            throw new RuleException(e);
        } finally {
            if (inRules != null) {
                inRules.close();
            }
        }
        return solrFile;
    }

    private File createTempFile(String prefix, String postfix)
            throws IOException {
        File tempFile = File.createTempFile(prefix, postfix);
        tempFile.deleteOnExit();
        if (tempFiles == null) {
            tempFiles = Collections.synchronizedList(new ArrayList<File>());
        }
        tempFiles.add(tempFile);
        return tempFile;
    }

    private void cleanupTempFiles() {
        if (tempFiles != null) {
            for (File tempFile : tempFiles) {
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
            tempFiles = null;
        }
    }
}
