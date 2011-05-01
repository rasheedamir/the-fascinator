package au.edu.usq.fascinator.portal;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.harvester.Harvester;
import au.edu.usq.fascinator.common.JsonConfigHelper;

public class HarvestContent extends JsonConfigHelper {

    private File jsonFile;

    public HarvestContent(File jsonFile) throws IOException {
        super(jsonFile);
        this.jsonFile = jsonFile;
    }

    public String getId() {
        return FilenameUtils.getBaseName(jsonFile.getName());
    }

    public String getDescription() {
        return get("content/description");
    }

    public Map<String, Object> getIndexerParams() {
        return getMap("indexer/params");
    }

    public String getIndexerParam(String name) {
        return get("indexer/params/" + name);
    }

    public void setIndexerParam(String name, String value) {
        set("indexer/params/" + name, value);
    }

    public File getRulesFile() {
        return new File(get("indexer/script/rules"));
    }

    public Harvester getHarvester() {
        Harvester harvester = PluginManager.getHarvester(get("harvester/type"));
        if (harvester != null) {
            try {
                harvester.init(jsonFile);
            } catch (PluginException e) {
                e.printStackTrace();
            }
        }
        return harvester;
    }
}
