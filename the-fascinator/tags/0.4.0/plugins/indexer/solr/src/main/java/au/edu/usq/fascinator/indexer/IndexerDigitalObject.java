package au.edu.usq.fascinator.indexer;

import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject;

public class IndexerDigitalObject extends GenericDigitalObject {

    public IndexerDigitalObject(DigitalObject digitalObject, String metadataId) {
        super(digitalObject);
        setMetadataId(metadataId);
    }
}
