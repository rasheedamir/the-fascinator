package au.edu.usq.fascinator;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.transformer.Transformer;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.JsonConfig;

/**
 * ConveyerBelt class to handler transformation of an object e.g. ICE & aperture
 * transformer
 * 
 * @author Oliver Lucido & Linda Octalina
 * 
 */
public class ConveyerBelt {
    private JsonConfig config;
    private File jsonFile;
    private String type;

    private String configString;

    private static Logger log = LoggerFactory.getLogger(ConveyerBelt.class);

    public ConveyerBelt(File jsonFile, String type) {
        this.jsonFile = jsonFile;
        this.type = type;
        try {
            config = new JsonConfig(jsonFile);
        } catch (IOException ioe) {
            log.warn("Failed to load config from {}", jsonFile);
        }
    }

    public ConveyerBelt(String json, String type) {
        try {
            configString = json;
            config = new JsonConfig(json);
            this.type = type;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            log.warn("Failed to load config from {}", jsonFile);
        }
    }

    public DigitalObject transform(DigitalObject object)
            throws TransformerException {
        List<Object> pluginList = config.getList("transformer/" + type);
        DigitalObject result = object;
        if (pluginList != null) {
            for (Object pluginName : pluginList) {
                Transformer transPlugin = PluginManager
                        .getTransformer(pluginName.toString().trim());
                // log.info("------ conveyer belt, getting: "
                // + pluginName.toString());
                try {
                    if (jsonFile == null) {
                        // log.info("init from string " + configString);
                        transPlugin.init(configString);
                    } else {
                        log.info("init from file");
                        transPlugin.init(jsonFile);
                    }
                    log.info("transforming");
                    result = transPlugin.transform(result);
                    log.info("transform result = " + result);
                } catch (PluginException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }
}
