/*
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
package au.edu.usq.fascinator.maven_plugins.schemagen;

import java.io.File;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;

/**
 * @author dickinso
 * 
 */
abstract class SchemaGenerator extends AbstractMojo {

    /**
     * The file or URI of the schema
     * 
     * @parameter property="schema"
     * @required
     */
    private Map<String, String> schema;

    /**
     * The file or directory for the output
     * 
     * @parameter expression="${project.build.directory}/schemagen"
     *            property="outputFolder"
     */
    private File outputFolder;

    /**
     * The Java package name
     * 
     * @parameter expression="" property="packageName"
     */
    private String packageName;

    /**
     * True to limit elements to only those in the namespace, false otherwise
     * 
     * @parameter expression="true" property="namespaceStrict"
     */
    private boolean namespaceStrict = true;

    public File getOutputFolder() {
        return outputFolder;
    }

    public String getPackageName() {
        return packageName;
    }

    public Map<String, String> getSchema() {
        return schema;
    }

    public boolean getNamespaceStrict() {
        return isNamespaceStrict();
    }

    public boolean isNamespaceStrict() {
        return namespaceStrict;
    }

    public void setNamespaceStrict(boolean namespaceStrict) {
        this.namespaceStrict = namespaceStrict;
    }

    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setSchema(Map<String, String> schema) {
        this.schema = schema;
    }

}
