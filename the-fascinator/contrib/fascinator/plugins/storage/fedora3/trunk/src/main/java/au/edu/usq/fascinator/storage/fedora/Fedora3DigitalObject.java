/*
 * The Fascinator - Fedora Commons 3.x storage plugin
 * Copyright (C) 2009-2011 University of Southern Queensland
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
package au.edu.usq.fascinator.storage.fedora;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.MimeTypeUtil;
import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject;
import au.edu.usq.fedora.RestClient;
import au.edu.usq.fedora.types.DatastreamProfile;
import au.edu.usq.fedora.types.DatastreamType;
import au.edu.usq.fedora.types.ObjectDatastreamsType;

/**
 * Maps a Fedora object to a Fascinator digital object.
 *
 * @author Oliver Lucido
 */
public class Fedora3DigitalObject extends GenericDigitalObject {

    /** Logging */
    private Logger log = LoggerFactory.getLogger(Fedora3DigitalObject.class);

    /** Internal Fedora PID */
    private String fedoraPid;

    /** Fedora REST API client */
    private RestClient client;

    public Fedora3DigitalObject(String oid, String fedoraPid, RestClient client) {
        super(oid);
        // log.debug("Construct Fedora3DigitalObject oid={} fid={}", oid,
        // fedoraPid);
        this.fedoraPid = fedoraPid;
        this.client = client;
        buildManifest();
    }

    private void buildManifest() {
        // log.debug("buildManifest({})", getId());
        Map<String, Payload> manifest = getManifest();
        try {
            ObjectDatastreamsType odt = client.listDatastreams(fedoraPid);
            List<DatastreamType> dsList = odt.getDatastreams();
            for (DatastreamType dt : dsList) {
                String dsId = dt.getDsid();
                if (!"DC".equals(dsId)) {
                    DatastreamProfile dsp = client.getDatastream(fedoraPid,
                            dsId);
                    String pid = dsp.getDsLabel();
                    pid = "TF-OBJ-META".equals(dsId) ? dsId : pid;
                    Payload payload = new Fedora3Payload(dsp, pid, fedoraPid,
                            dsId, client);
                    if (PayloadType.Source.equals(payload.getType())) {
                        setSourceId(pid);
                    }
                    manifest.put(pid, payload);
                }
            }
        } catch (IOException ioe) {
            log.error("Failed to get payload ID list!", ioe);
        }
    }

    @Override
    public Payload createStoredPayload(String pid, InputStream in)
            throws StorageException {
        // log.debug("createStoredPayload({},{})", getId(), pid);

        Map<String, Payload> manifest = getManifest();
        if (manifest.containsKey(pid)) {
            throw new StorageException("pID '" + pid
                    + "' already exists in manifest.");
        }

        Fedora3Payload payload = null;
        try {
            String dsId = getDatastreamId(pid);
            DatastreamProfile dsp = client.getDatastream(fedoraPid, dsId);
            if (dsp == null) {
                PayloadType pt = PayloadType.Enrichment;
                if (getSourceId() == null && !"TF-OBJ-META".equals(pid)) {
                    setSourceId(pid);
                    pt = PayloadType.Source;
                }
                String prefix = FilenameUtils.getBaseName(pid);
                String suffix = FilenameUtils.getExtension(pid);
                prefix = StringUtils.rightPad(prefix, 3, "_");
                suffix = "".equals(suffix) ? null : "." + suffix;
                File content = File.createTempFile(prefix, suffix);
                FileOutputStream out = new FileOutputStream(content);
                IOUtils.copy(in, out);
                out.close();
                String dsLabel = pid;
                String contentType = MimeTypeUtil.getMimeType(content);
                String altIds = pt.toString();
                client.addDatastream(fedoraPid, dsId, dsLabel, contentType,
                        altIds, content);
                content.delete();
                payload = new Fedora3Payload(pid, fedoraPid, dsId, client);
                payload.setType(pt);
                payload.setContentType(contentType);
                payload.setLabel(pid);
                payload.setLinked(false);
                manifest.put(pid, payload);
            } else {
                throw new StorageException("pID '" + pid + "' already exists.");
            }
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
        return payload;
    }

    @Override
    public Payload createLinkedPayload(String pid, String linkPath)
            throws StorageException {
        // log.debug("createLinkedPayload({},{},{})", new String[] { getId(),
        // pid, linkPath });
        try {
            FileInputStream in = new FileInputStream(linkPath);
            return createStoredPayload(pid, in);
        } catch (FileNotFoundException fnfe) {
            throw new StorageException(fnfe);
        }
    }

    @Override
    public Payload getPayload(String pid) throws StorageException {
        String dsId = getDatastreamId(pid);
        // log.debug("getPayload({},{})", getId(), pid);
        try {
            DatastreamProfile dsp = client.getDatastream(fedoraPid, dsId);
            if (dsp == null) {
                throw new StorageException("pID '" + pid + "' does not exist.");
            } else {
                return new Fedora3Payload(dsp, pid, fedoraPid, dsId, client);
            }
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
    }

    @Override
    public void removePayload(String pid) throws StorageException {
        // log.debug("removePayload({},{})", getId(), pid);
        try {
            client.purgeDatastream(fedoraPid, pid);
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
    }

    @Override
    public Payload updatePayload(String pid, InputStream in)
            throws StorageException {
        // log.debug("updatePayload({},{})", getId(), pid);
        Map<String, Payload> manifest = getManifest();
        if (!manifest.containsKey(pid)) {
            throw new StorageException("pID '" + pid + "': file not found");
        }
        try {
            Payload payload = manifest.get(pid);
            String prefix = FilenameUtils.getBaseName(pid);
            String suffix = FilenameUtils.getExtension(pid);
            prefix = StringUtils.rightPad(prefix, 3, "_");
            suffix = "".equals(suffix) ? null : "." + suffix;
            File content = File.createTempFile(prefix, suffix);
            FileOutputStream out = new FileOutputStream(content);
            IOUtils.copy(in, out);
            out.close();
            String dsId = getDatastreamId(pid);
            String dsLabel = payload.getLabel();
            String altIds = payload.getType().toString();
            String contentType = MimeTypeUtil.getMimeType(content);
            client.modifyDatastream(fedoraPid, dsId, dsLabel, altIds,
                    contentType, content);
            content.delete();
            return manifest.get(pid);
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }

    private String getDatastreamId(String pid) {
        if ("TF-OBJ-META".equals(pid)) {
            return pid;
        }
        return "DS" + DigestUtils.md5Hex(pid);
    }
}
