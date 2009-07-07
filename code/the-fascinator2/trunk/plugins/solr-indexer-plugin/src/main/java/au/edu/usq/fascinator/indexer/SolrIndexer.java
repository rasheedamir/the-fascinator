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
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.request.DirectXmlRequest;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.indexer.rule.RuleException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonConfig;

public class SolrIndexer implements Indexer {

    private Logger log = LoggerFactory.getLogger(SolrIndexer.class);

    private JsonConfig config;

    private Storage storage;

    private SolrServer solr;

    private boolean autoCommit;

    private String propertiesId;

    private List<File> tempFiles;

    public String getId() {
        return "solr";
    }

    public String getName() {
        return "Apache Solr Indexer";
    }

    public void init(File jsonFile) throws IndexerException {
        try {
            config = new JsonConfig(jsonFile);
        } catch (IOException ioe) {
            throw new IndexerException(ioe);
        }

        String storageType = config.get("storage/type");
        try {
            storage = PluginManager.getStorage(storageType);
            storage.init(jsonFile);
        } catch (PluginException pe) {
            log.error("Failed to load storage plugin: {}", storageType);
        }

        try {
            URI solrUri = new URI(config.get("indexer/solr/uri"));
            if ("file".equals(solrUri.getScheme())) {
                SolrResourceLoader loader = new SolrResourceLoader(new File(
                        solrUri).getAbsolutePath());
                CoreContainer coreContainer = new CoreContainer(loader);
                CoreDescriptor coreDesc = new CoreDescriptor(coreContainer,
                        "MainCore", loader.getInstanceDir());
                try {
                    SolrCore mainCore = coreContainer.create(coreDesc);
                    coreContainer.register("MainCore", mainCore, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                solr = new EmbeddedSolrServer(coreContainer, "MainCore");
            } else {
                solr = new CommonsHttpSolrServer(solrUri.toURL());
                String username = config.get("indexer/solr/username");
                String password = config.get("indexer/solr/password");
                if (username != null && password != null) {
                    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                            username, password);
                    HttpClient hc = ((CommonsHttpSolrServer) solr)
                            .getHttpClient();
                    hc.getParams().setAuthenticationPreemptive(true);
                    hc.getState().setCredentials(AuthScope.ANY, credentials);
                }
            }
        } catch (MalformedURLException mue) {
            log.error("Malformed URL", mue);
        } catch (URISyntaxException urise) {
            log.error("Invalid URI", urise);
        }

        autoCommit = Boolean.parseBoolean(config.get("indexer/solr/autocommit",
                "true"));
        propertiesId = config.get("indexer/propertiesId", "SOF-META");
    }

    @Override
    public void shutdown() throws PluginException {
    }

    public Storage getStorage() {
        return storage;
    }

    public void delete(String oid) throws IndexerException {
        log.debug("Deleting " + oid + " from index");
        try {
            solr.deleteByQuery("oid:\"" + oid + "\"");
            solr.commit();
        } catch (SolrServerException sse) {
            throw new IndexerException(sse);
        } catch (IOException ioe) {
            throw new IndexerException(ioe);
        }
    }

    public void index(String oid) throws IndexerException {
        List<Payload> payloadList = storage.getObject(oid).getPayloadList();
        for (Payload payload : payloadList) {
            String pid = payload.getId();
            if (!propertiesId.equals(pid)) {
                index(oid, pid);
            }
        }
    }

    public void index(String oid, String pid) throws IndexerException {
        log.info("Indexing " + oid + "/" + pid);

        // get the indexer properties or we can't index
        Properties props = getIndexerProperties(oid);
        if (props == null) {
            log.warn("Indexer properties not found, object not indexed");
            throw new IndexerException("Indexer properties not found");
        }

        try {
            // create the item for indexing
            String itemId = props.getProperty("item.pid");
            DigitalObject item = storage.getObject(itemId);

            // get the indexer rules
            String rulesPid = props.getProperty("rules.pid");
            File rules = createTempFile("rules", ".script");
            FileOutputStream rulesOut = new FileOutputStream(rules);
            Payload rulesScript = storage.getPayload(rulesPid, "RULES.PY");
            IOUtils.copy(rulesScript.getInputStream(), rulesOut);
            rulesOut.close();

            // primary metadata datastream
            String metadataDsid = props.getProperty("metaId", "DC");

            // index the object
            String set = null; // TODO
            File solrFile = null;
            if (metadataDsid.equals(pid)) {
                solrFile = indexMetadata(item, oid, set, rules, props);
            } else {
                solrFile = indexDatastream(item, oid, pid, rules, props);
            }
            if (solrFile != null) {
                InputStream inputDoc = new FileInputStream(solrFile);
                String xml = IOUtils.toString(inputDoc);
                inputDoc.close();
                SolrRequest update = new DirectXmlRequest("/update", xml);
                solr.request(update);
                if (autoCommit) {
                    solr.commit();
                }
            }
        } catch (Exception e) {
            log.error("Index failed!\n-----\n{}\n-----\n", e);
        } finally {
            cleanupTempFiles();
        }
    }

    private File indexMetadata(DigitalObject item, String pid, String set,
            File rulesFile, Properties props) throws IOException,
            StorageException, RuleException {
        log.info("Indexing metadata...");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Payload ds = storage.getPayload(pid, item.getMetadata().getId());
        IOUtils.copy(ds.getInputStream(), out);
        InputStreamReader in = new InputStreamReader(new ByteArrayInputStream(
                out.toByteArray()), "UTF-8");
        return index(item, pid, null, set, in, rulesFile, props);
    }

    private File indexDatastream(DigitalObject item, String pid, String dsId,
            File rulesFile, Properties props) throws IOException, RuleException {
        log.info("Indexing datastream...");
        Reader in = new StringReader("<add><doc/></add>");
        return index(item, pid, dsId, null, in, rulesFile, props);
    }

    private File index(DigitalObject item, String pid, String dsId, String set,
            Reader in, File ruleScript, Properties props) throws IOException,
            RuleException {
        File solrFile = createTempFile("solr", ".xml");
        Writer out = new OutputStreamWriter(new FileOutputStream(solrFile),
                "UTF-8");
        try {
            String engineName = props.getProperty("rules.engine", "python");
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
            scriptEngine.put("pid", pid);
            scriptEngine.put("dsId", dsId);
            scriptEngine.put("item", item);
            scriptEngine.put("params", props);
            // TODO add required solr fields?
            scriptEngine.eval(new FileReader(ruleScript));
            rules.run(in, out);
            if (rules.cancelled()) {
                log.info("Indexing rules were cancelled");
                return null;
            }

            // PythonInterpreter python = new PythonInterpreter();
            // RuleManager rules = new RuleManager();
            // python.set("self", this);
            // python.set("rules", rules);
            // python.set("pid", pid);
            // python.set("dsId", dsId);
            // python.set("collection", set);
            // python.set("item", item);
            // python.set("params", props);
            // python.execfile(ruleScript.getAbsolutePath());
            // rules.run(in, out);
            // if (rules.cancelled()) {
            // log.info("Indexing rules were cancelled");
            // return null;
            // }
            // python.cleanup();
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
        } catch (IOException ioe) {
            log.warn("Failed to load properties", ioe);
        }
        return null;
    }

    private File createTempFile(String prefix, String postfix)
            throws IOException {
        File tempFile = File.createTempFile(prefix, postfix);
        if (tempFiles == null) {
            tempFiles = new ArrayList<File>();
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
        tempFiles = new ArrayList<File>();
    }

    // Helper methods

    public InputStream getResource(String path) {
        return getClass().getResourceAsStream(path);
    }

}
