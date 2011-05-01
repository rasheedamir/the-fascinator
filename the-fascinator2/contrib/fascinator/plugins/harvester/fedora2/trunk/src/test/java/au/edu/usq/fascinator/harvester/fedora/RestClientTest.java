/* 
 * The Fascinator - Plugin - Harvester - Fedora
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

package au.edu.usq.fascinator.harvester.fedora;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.junit.Assert;
import org.junit.Test;

import au.edu.usq.fascinator.harvester.fedora.restclient.DatastreamType;
import au.edu.usq.fascinator.harvester.fedora.restclient.ObjectDatastreamsType;
import au.edu.usq.fascinator.harvester.fedora.restclient.ResultType;


/**
 * Unit tests for the fedora harvester plugin
 * 
 * @author Linda Octalina
 * 
 */
public class RestClientTest {
    @Test
    public void result() throws Exception {
        JAXBContext jc = JAXBContext.newInstance(ResultType.class);
        Unmarshaller um = jc.createUnmarshaller();
        ResultType result = (ResultType) um.unmarshal(getClass()
                .getResourceAsStream("/result.xml"));
        Assert.assertEquals(0, result.getListSession().getCursor());
        Assert.assertEquals("c29863f172bcaad416db2133083e0edc", result
                .getListSession().getToken());
        Assert.assertEquals(10, result.getObjectFields().size());
    }

    @Test
    public void objectDatastreams() throws Exception {
        JAXBContext jc = JAXBContext.newInstance(ObjectDatastreamsType.class);
        Unmarshaller um = jc.createUnmarshaller();
        ObjectDatastreamsType result = (ObjectDatastreamsType) um
                .unmarshal(getClass().getResourceAsStream(
                        "/object-datastreams.xml"));
        Assert.assertEquals("uon:1", result.getPid());
        Assert.assertEquals(2, result.getDatastreams().size());
        DatastreamType dc = result.getDatastreams().get(0);
        Assert.assertEquals("DS1", dc.getDsid());
    }
}