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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * To get the datastreamProfile of a datastream e.g.
 * http://localhost:8080/fedora
 * /objects/uuid:9d23cadf-82c0-4b8d-82e6-5e00b07a5b70
 * /datastreams/aperture.rdf.xml
 * 
 * @author Linda Octalina
 * 
 */

@XmlRootElement(name = "datastreamProfile")
@XmlAccessorType(XmlAccessType.NONE)
public class DatastreamProfile {

    @XmlElement(name = "dsLabel")
    private String dsLabel;

    @XmlElement(name = "dsVersionID")
    private String dsVersionID;

    @XmlElement(name = "dsCreateDate")
    private String dsCreateDate;

    @XmlElement(name = "dsState")
    private String dsState;

    @XmlElement(name = "dsMIME")
    private String dsMIME;

    @XmlElement(defaultValue = "", name = "dsFormatURI")
    private String dsFormatURI;

    @XmlElement(name = "dsControlGroup")
    private String dsControlGroup;

    @XmlElement(name = "dsSize")
    private String dsSize;

    @XmlElement(name = "dsVersionable")
    private String dsVersionable;

    @XmlElement(defaultValue = "", name = "dsInfoType")
    private String dsInfoType;

    @XmlElement(name = "dsLocation")
    private String dsLocation;

    @XmlElement(name = "dsLocationType")
    private String dsLocationType;

    @XmlElement(defaultValue = "none", name = "dsChecksumType")
    private String dsChecksumType;

    @XmlElement(name = "dsChecksum")
    private String dsChecksum;

    @XmlElement(name = "dsAltID")
    private String dsAltID;

    // @Override
    // public int compareTo(DatastreamProfile that) {
    // return dsLabel.compareTo(that.getDsLabel());
    // }

    public void setDsLabel(String dsLabel) {
        this.dsLabel = dsLabel;
    }

    public String getDsLabel() {
        return dsLabel;
    }

    public void setDsVersionID(String dsVersionID) {
        this.dsVersionID = dsVersionID;
    }

    public String getDsVersionID() {
        return dsVersionID;
    }

    public void setDsCreateDate(String dsCreateDate) {
        this.dsCreateDate = dsCreateDate;
    }

    public String getDsCreateDate() {
        return dsCreateDate;
    }

    public void setDsState(String dsState) {
        this.dsState = dsState;
    }

    public String getDsState() {
        return dsState;
    }

    public void setDsMIME(String dsMIME) {
        this.dsMIME = dsMIME;
    }

    public String getDsMIME() {
        return dsMIME;
    }

    public void setDsFormatURI(String dsFormatURI) {
        this.dsFormatURI = dsFormatURI;
    }

    public String getDsFormatURI() {
        return dsFormatURI;
    }

    public void setDsControlGroup(String dsControlGroup) {
        this.dsControlGroup = dsControlGroup;
    }

    public String getDsControlGroup() {
        return dsControlGroup;
    }

    public void setDsSize(String dsSize) {
        this.dsSize = dsSize;
    }

    public String getDsSize() {
        return dsSize;
    }

    public void setDsVersionable(String dsVersionable) {
        this.dsVersionable = dsVersionable;
    }

    public String getDsVersionable() {
        return dsVersionable;
    }

    public void setDsInfoType(String dsInfoType) {
        this.dsInfoType = dsInfoType;
    }

    public String getDsInfoType() {
        return dsInfoType;
    }

    public void setDsLocation(String dsLocation) {
        this.dsLocation = dsLocation;
    }

    public String getDsLocation() {
        return dsLocation;
    }

    public void setDsLocationType(String dsLocationType) {
        this.dsLocationType = dsLocationType;
    }

    public String getDsLocationType() {
        return dsLocationType;
    }

    public void setDsChecksumType(String dsChecksumType) {
        this.dsChecksumType = dsChecksumType;
    }

    public String getDsChecksumType() {
        return dsChecksumType;
    }

    public void setDsChecksum(String dsChecksum) {
        this.dsChecksum = dsChecksum;
    }

    public String getDsChecksum() {
        return dsChecksum;
    }

    public void setDsAltID(String dsAltID) {
        this.dsAltID = dsAltID;
    }

    public String getDsAltID() {
        return dsAltID;
    }

    @Override
    public String toString() {
        return dsLabel;
    }
}
