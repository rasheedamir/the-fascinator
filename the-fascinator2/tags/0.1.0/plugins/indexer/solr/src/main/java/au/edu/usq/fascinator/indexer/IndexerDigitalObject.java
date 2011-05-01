package au.edu.usq.fascinator.indexer;

import java.util.List;

import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;

public class IndexerDigitalObject implements DigitalObject {

    private DigitalObject digitalObject;
    
    private String metadataId;

    public IndexerDigitalObject(DigitalObject digitalObject, String metadataId) {
        this.digitalObject = digitalObject;
        this.metadataId = metadataId;
    }

    @Override
    public String getId() {
        return digitalObject.getId();
    }

    @Override
    public Payload getMetadata() {
        return getPayload(metadataId);
    }

    @Override
    public Payload getPayload(String pid) {
        return digitalObject.getPayload(pid);
    }

    @Override
    public List<Payload> getPayloadList() {
        return digitalObject.getPayloadList();
    }

}
