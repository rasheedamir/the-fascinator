/* 
 * The Fascinator - Solr Portal
 * Copyright (C) 2008  University of Southern Queensland
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
package au.edu.usq.fascinator.harvester.fedora.restclient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "datastreamProfile")
@XmlAccessorType(XmlAccessType.NONE)
public class DatastreamProfile {

    @XmlAttribute
    private String pid;

    @XmlAttribute
    private String dsId;

    @XmlElement
    private String dsLabel;

    @XmlElement(name = "dsVersionID")
    private String dsVersionId;

    @XmlElement
    private String dsCreateDate;

    @XmlElement
    private String dsState;

    @XmlElement(name = "dsMIME")
    private String dsMime;

    @XmlElement(name = "dsFormatURI")
    private String dsFormatUri;

    @XmlElement
    private String dsControlGroup;

    @XmlElement
    private String dsSize;

    @XmlElement
    private String dsVersionable;

    @XmlElement
    private String dsInfoType;

    @XmlElement
    private String dsLocation;

    @XmlElement
    private String dsLocationType;

    @XmlElement
    private String dsChecksumType;

    @XmlElement
    private String dsChecksum;

    public String getPid() {
        return pid;
    }

    public String getDsId() {
        return dsId;
    }

    public String getDsLabel() {
        return dsLabel;
    }

    public String getDsVersionId() {
        return dsVersionId;
    }

    public String getDsCreateDate() {
        return dsCreateDate;
    }

    public String getDsState() {
        return dsState;
    }

    public String getDsMime() {
        return dsMime;
    }

    public String getDsFormatUri() {
        return dsFormatUri;
    }

    public String getDsControlGroup() {
        return dsControlGroup;
    }

    public String getDsSize() {
        return dsSize;
    }

    public String getDsVersionable() {
        return dsVersionable;
    }

    public String getDsInfoType() {
        return dsInfoType;
    }

    public String getDsLocation() {
        return dsLocation;
    }

    public String getDsLocationType() {
        return dsLocationType;
    }

    public String getDsChecksumType() {
        return dsChecksumType;
    }

    public String getDsChecksum() {
        return dsChecksum;
    }

}