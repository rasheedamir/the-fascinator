/* 
 * Fedora Commons 3.x Storage Plugin
 * Copyright (C) 2009 University of Southern Queensland
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
package au.edu.usq.fedora.types;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "objectDatastreams", namespace = ObjectDatastreamsType.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class ObjectDatastreamsType {

    public static final String NAMESPACE = "http://www.fedora.info/definitions/1/0/access/";

    @XmlAttribute
    private String pid;

    @XmlAttribute(name = "baseURL", namespace = NAMESPACE)
    private String baseUrl;

    @XmlElement(name = "datastream", namespace = NAMESPACE)
    private List<DatastreamType> datastreams;

    public String getPid() {
        return pid;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public List<DatastreamType> getDatastreams() {
        if (datastreams == null) {
            datastreams = new ArrayList<DatastreamType>();
        }
        return datastreams;
    }
}
