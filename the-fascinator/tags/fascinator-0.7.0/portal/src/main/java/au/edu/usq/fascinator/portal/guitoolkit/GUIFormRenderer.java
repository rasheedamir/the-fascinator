/*
 * The Fascinator - GUI Form Renderer
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

import java.util.Map;

import au.edu.usq.fascinator.common.JsonConfig;

/**
 * Render web forms based on descriptive metadata.
 * 
 * @author Greg Pendlebury
 */
public class GUIFormRenderer {

    public GUIFormRenderer(JsonConfig config) {
    }

    public String ajaxFluidErrorHolder(String prefix) {
        return "<div class='stop-error hidden' id='" + prefix + "-error'>\n"
                + "<span id='" + prefix + "-message'></span>\n" + "</div>\n";
    }

    public String ajaxFluidLoader(String prefix) {
        return "<img class='hidden' id='" + prefix
                + "-loading' src='images/icons/loading.gif' alt='Loading'/>\n";
    }

    public String ajaxProgressLoader(String prefix) {
        return "<img class='hidden' id='"
                + prefix
                + "-loading' src='images/loading-progress.gif' alt='Loading'/>\n";
    }

    public String renderFormElement(String name, String type, String label) {
        return renderFormElement(name, type, label, "");
    }

    public String renderFormElement(String name, String type, String label,
            String value) {
        String element = "";
        if (label != null && !label.equals("")) {
            element += "<label for='" + name + "'>" + label + "</label>\n";
        }
        element += "<input type='" + type + "' id='" + name + "' name='" + name
                + "'";
        if (value != null && !value.equals("")) {
            element += " value='" + value + "'";
        }
        element += "/>\n";
        return element;
    }

    public String renderFormSelect(String name, String label,
            Map<String, String> values) {
        String select = "";
        if (label != null && !label.equals("")) {
            select += "<label for='" + name + "'>" + label + "</label>\n";
        }
        select += "<select id='" + name + "' name='" + name + "'>\n";
        for (String plugins : values.keySet()) {
            select += "<option value='" + plugins + "'>" + values.get(plugins)
                    + "</option>\n";
        }
        select += "</select>\n";
        return select;
    }
}
