package au.edu.usq.fascinator.transformer.ims;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject;

public class ImsDigitalObject extends GenericDigitalObject{
	/** Logging */
    private Logger log = LoggerFactory.getLogger(ImsDigitalObject.class);
    
    private boolean isImsPackage=false;
    
    private String manifestFile = "imsmanifest.xml";
    
	public ImsDigitalObject (DigitalObject zipDigitalObject, String filePath) {
		super(zipDigitalObject);
		try {
            if (filePath.endsWith(".zip")) { 
                File zipPathFile = new File(filePath);
                ZipFile zipFile = new ZipFile(zipPathFile);
                ZipEntry manifestEntry = zipFile.getEntry(manifestFile);
                if (manifestEntry != null) {
                    isImsPackage = true;
	            Enumeration<? extends ZipEntry> entries = zipFile.entries();
	            while (entries.hasMoreElements()) {
	                ZipEntry entry = entries.nextElement();
	                if (!entry.isDirectory()) {
	                    addPayload(new ImsPayload(zipPathFile, entry));
	                }
	            }
                    // imsmanifest.json
                }
                setMetadataId(manifestFile);
            }
        } catch (IOException ioe) {
            log.error("Failed to add payloads: {}", ioe.toString());
        }
	}
	
	public boolean getIsImsPackage() {
		return isImsPackage;
	}
}
