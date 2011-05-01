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
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class ObjectFieldType {

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String pid;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String label;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String fType;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String cModel;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String state;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String ownerId;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String cDate;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String mDate;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String dcmDate;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String title;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String creator;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String subject;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String description;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String publisher;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String contributor;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String date;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String type;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String format;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String identifier;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String source;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String language;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String relation;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String coverage;

    @XmlElement(namespace = ResultType.NAMESPACE)
    private String rights;

    public String getPid() {
        return pid;
    }

    public String getLabel() {
        return label;
    }

    public String getFType() {
        return fType;
    }

    public String getCModel() {
        return cModel;
    }

    public String getState() {
        return state;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getCDate() {
        return cDate;
    }

    public String getMDate() {
        return mDate;
    }

    public String getDcmDate() {
        return dcmDate;
    }

    public String getTitle() {
        return title;
    }

    public String getCreator() {
        return creator;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getContributor() {
        return contributor;
    }

    public String getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public String getFormat() {
        return format;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getSource() {
        return source;
    }

    public String getLanguage() {
        return language;
    }

    public String getRelation() {
        return relation;
    }

    public String getCoverage() {
        return coverage;
    }

    public String getRights() {
        return rights;
    }
}