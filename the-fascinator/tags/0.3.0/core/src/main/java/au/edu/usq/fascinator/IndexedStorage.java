package au.edu.usq.fascinator;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;

/**
 * Wraps a storage plugin to provide indexing functions
 * 
 * @author Oliver Lucido
 */
public class IndexedStorage implements Storage {

    private Logger log = LoggerFactory.getLogger(IndexedStorage.class);

    private Storage storage;

    private Indexer indexer;

    public IndexedStorage(Storage storage, Indexer indexer) {
        this.storage = storage;
        this.indexer = indexer;
    }

    public String addObject(DigitalObject object) throws StorageException {
        String sid = storage.addObject(object);
        try {
            indexer.index(object.getId());
        } catch (IndexerException ie) {
            log.error("Failed to add {} to index", object);
            throw new StorageException(ie);
        }
        return sid;
    }

    public void addPayload(String oid, Payload payload) {
        storage.addPayload(oid, payload);
        try {
            indexer.index(oid, payload.getId());
        } catch (IndexerException ie) {
            log.error("Failed to add {}, {} to index", oid, payload);
        }
    }

    public DigitalObject getObject(String oid) {
        return storage.getObject(oid);
    }

    public Payload getPayload(String oid, String pid) {
        return storage.getPayload(oid, pid);
    }

    public void removeObject(String oid) {
        storage.removeObject(oid);
        try {
            indexer.remove(oid);
        } catch (IndexerException ie) {
            log.error("Failed to remove {} from index", oid);
        }
    }

    public void removePayload(String oid, String pid) {
        storage.removePayload(oid, pid);
        try {
            indexer.remove(oid, pid);
        } catch (IndexerException ie) {
            log.error("Failed to remove {}, {} from index", oid, pid);
        }
    }

    public String getId() {
        return storage.getId();
    }

    public String getName() {
        return storage.getName();
    }

    public void init(File jsonFile) throws PluginException {
        indexer.init(jsonFile);
        storage.init(jsonFile);
    }

    public void shutdown() throws PluginException {
        indexer.shutdown();
        storage.shutdown();
    }

    @Override
    public void init(String jsonString) throws PluginException {
        // TODO Auto-generated method stub

    }

    @Override
    public List<DigitalObject> getObjectList() {
        // TODO Auto-generated method stub
        return null;
    }

}
