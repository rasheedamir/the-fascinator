/*
 * The Fascinator - GUI File Uploader
 * Copyright (C) 2010 University of Southern Queensland
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
package au.edu.usq.fascinator.portal.guitoolkit;

import au.edu.usq.fascinator.common.JsonConfig;
import au.edu.usq.fascinator.common.JsonConfigHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Present a file upload interface to the user for ingesting
 * into various harvest plugins.
 *
 * @author Greg Pendlebury
 */
public class GUIFileUploader {
    private static String DEFAULT_FILE_NAME = "workflow-config.json";
    private GUIFormRenderer fr;
    private Map<String, String> harvesters;
    private static Logger log = LoggerFactory.getLogger(GUIFileUploader.class);
    private JsonConfigHelper workflow_config;

    public GUIFileUploader(JsonConfig config, List<String> user_roles) {
        // Init our form renderer
        fr = new GUIFormRenderer(config);

        // We need a list of harvesting plugins that
        //   support file upload intitiated ingest.
        String workflow_config_path =  config.get("portal/uploader");
        File workflow_config_file = new File(workflow_config_path);

        // If the specified file doesn't exist, create it from our default
        File access_file = new File(workflow_config_path);
        if (!access_file.exists()) {
            try {
                access_file.getParentFile().mkdirs();
                OutputStream out;
                out = new FileOutputStream(access_file);
                IOUtils.copy(getClass().getResourceAsStream("/" + DEFAULT_FILE_NAME), out);
                out.close();
                log.debug("Workflow config file not found, created from default.");
            } catch (Exception ex) {
                log.error("Failed creating default workflow config file.", ex);
            }
        }

        // Now we know it exists and where it is, go and parse it
        try {
            workflow_config = new JsonConfigHelper(workflow_config_file);
            Map<String, JsonConfigHelper> available_workflows =
                    workflow_config.getJsonMap("/");
            harvesters = new LinkedHashMap();
            List<String> allowed_roles = new ArrayList<String>();
            // Foreach workflow
            for (String workflow : available_workflows.keySet()) {
                // Check who is allowed to upload files for it
                for (Object role : available_workflows.get(workflow)
                        .getList("security")) {
                    if (user_roles.contains(role.toString())) {
                        harvesters.put(workflow,
                                available_workflows.get(workflow)
                                .get("screen-label"));
                    }
                }
            }
       } catch (IOException ex) {
            log.error("Failed getting workflow config file.", ex);
        }
    }

    public String renderForm() {
        if (harvesters.isEmpty()) {
            return "Sorry, but your current security permissions don't allow for file uploading.";
        }

        String form_string = "" +
            "<form enctype='multipart/form-data' id='upload-file' method='post' action='workflow'>\n" +
              "<fieldset class='login'>\n" +
                "<legend>File Upload</legend>\n" +
                fr.ajaxFluidErrorHolder("upload-file") +
                "<p>\n" + fr.renderFormElement("upload-file-file", "file",
                        "Select a file to upload:") + "</p>\n" +
                "<p>\n" + fr.renderFormSelect("upload-file-workflow",
                        "Select the harvester to process the file:", harvesters)
                        + "</p>\n" +
                "<div class='center'>" +
                fr.renderFormElement("upload-file-submit", "button", null,
                        "Upload") +
                fr.ajaxProgressLoader("upload-file") +
                "</div>" +

                /* A real, ajax driven progess bar has been cut since
                 * Tapestry doesn't support setProgressListener().
                "<div id='upload-progress' class='hidden'>" +
                  "<div id='upload-progress-number'></div>" +
                  "<div class='upload-progress-holder'>" +
                    "<div id='upload-progress-filler'>&nbsp;</div>" +
                  "</div>" +
                "</div>" +
                 */

              "</fieldset>\n" +
            "</form>\n";

        return form_string;
    }
}
