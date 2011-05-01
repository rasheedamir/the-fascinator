package au.edu.usq.fascinator;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.transformer.Transformer;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.JsonConfig;

//package au.edu.usq.fascinator.extractor.aperture.Extractor;

public class ConveyerBelt {
    private JsonConfig config;
    private File jsonFile;

    private static Logger log = LoggerFactory.getLogger(ConveyerBelt.class);

    public ConveyerBelt(File jsonFile) {
        this.jsonFile = jsonFile;
        try {
            config = new JsonConfig(jsonFile);
        } catch (IOException ioe) {
            log.warn("Failed to load config from {}", jsonFile);
        }
    }

    public DigitalObject transform(DigitalObject object)
            throws TransformerException {
        String transformPluginInfo = config.get("transformer/conveyer", null);
        DigitalObject result = object;

        if (transformPluginInfo != null) {
            String[] pluginList = transformPluginInfo.split(",");

            for (String pluginName : pluginList) {
                Transformer transPlugin = PluginManager
                        .getTransformer(pluginName.trim());
                try {
                    transPlugin.init(jsonFile);
                    result = transPlugin.transform(result);
                } catch (PluginException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return result;
    }
}
