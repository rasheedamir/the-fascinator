package au.edu.usq.fascinator;

import java.io.File;

import au.edu.usq.fascinator.common.storage.impl.FilePayload;
import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject;

public class RulesDigitalObject extends GenericDigitalObject {

    public RulesDigitalObject(File rulesFile) {
        super(rulesFile.getAbsolutePath());
        FilePayload payload = new FilePayload(rulesFile);
        payload.setLabel("Fascinator Indexing Rules");
        addPayload(new FilePayload(rulesFile));
    }
}
