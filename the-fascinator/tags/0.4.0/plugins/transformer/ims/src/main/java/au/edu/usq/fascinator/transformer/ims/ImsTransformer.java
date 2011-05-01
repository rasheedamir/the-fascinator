package au.edu.usq.fascinator.transformer.ims;

import java.io.File;
import java.io.IOException;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.transformer.Transformer;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.JsonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImsTransformer implements Transformer{

	private JsonConfig config;

        private static Logger log = LoggerFactory
                .getLogger(ImsTransformer.class);

	public ImsTransformer() {
		
	}
	
	@Override
	public DigitalObject transform(DigitalObject in)
			throws TransformerException {
            File inFile;
            String filePath = config.get("sourceFile");
            if(filePath!=null){
                inFile = new File(filePath);
            } else {
                inFile = new File(in.getId());
            }

            if (inFile.exists()) {
                log.info("unpacking ims");
                ImsDigitalObject imsObject = new ImsDigitalObject(in, inFile.getAbsolutePath());
                return imsObject;
            } else {
                log.info("not found inFile {}", inFile);
            }
            return in;
	}

	@Override
	public String getId() {
		return "ims";
	}

	@Override
	public String getName() {
		return "IMS Transformer";
	}

	@Override
	public void init(File jsonFile) throws PluginException {
		try {
			config = new JsonConfig(jsonFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	@Override
	public void init(String jsonString) throws PluginException {
		try {
			config = new JsonConfig(jsonString);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void shutdown() throws PluginException {
		
	}
	
	public String getFileExt(File fileName) {
		return fileName.getName().substring(fileName.getName().lastIndexOf('.'));
	}

}
