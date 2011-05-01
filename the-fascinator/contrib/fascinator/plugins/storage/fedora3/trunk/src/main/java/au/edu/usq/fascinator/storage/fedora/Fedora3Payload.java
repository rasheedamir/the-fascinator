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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.MimeTypeUtil;
import au.edu.usq.fascinator.common.storage.impl.GenericPayload;
import au.edu.usq.fedora.RestClient;
import au.edu.usq.fedora.types.DatastreamProfile;

/**
 * Maps a Fedora datastream to a Fascinator payload.
 *
 * @author Oliver Lucido
 */
public class Fedora3Payload extends GenericPayload {

    // private Logger log = LoggerFactory.getLogger(Fedora3Payload.class);

    private InputStream stream;

    private String fedoraPid;

    private String dsId;

    private RestClient client;

    public Fedora3Payload(String pid, String fedoraPid, String dsId,
            RestClient client) {
        super(pid, pid, MimeTypeUtil.DEFAULT_MIME_TYPE);
        // log.debug("Construct NEW({},{},{})", new String[] { pid, fedoraPid,
        // dsId });
        init(fedoraPid, dsId, client);
    }

    public Fedora3Payload(DatastreamProfile dsp, String pid, String fedoraPid,
            String dsId, RestClient client) {
        super(pid, dsp.getDsLabel(), dsp.getDsMIME(), PayloadType.valueOf(dsp
                .getDsAltID()));
        // log.debug("Construct EXISTING ({},{},{})", new String[] { pid,
        // fedoraPid, dsId });
        init(fedoraPid, dsId, client);
    }

    private void init(String fedoraPid, String dsId, RestClient client) {
        stream = null;
        this.fedoraPid = fedoraPid;
        this.dsId = dsId;
        this.client = client;
    }

    @Override
    public InputStream open() throws StorageException {
        close();
        try {
            // log.debug("open({})", getId());
            stream = client.getStream(fedoraPid, dsId);
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
        return stream;
    }

    @Override
    public void close() throws StorageException {
        if (stream != null) {
            try {
                // log.debug("close({})", getId());
                stream.close();
            } catch (IOException ioe) {
                // ignore
            }
        }
        stream = null;
        if (hasMetaChanged()) {
            updateMeta();
        }
    }

    private void updateMeta() throws StorageException {
        // log.debug("updateMeta({})", getId());
        Properties options = new Properties();
        PayloadType type = getType();
        if (type == null) {
            type = PayloadType.Enrichment;
        }
        options.setProperty("altIDs", getType().toString());
        options.setProperty("label", getLabel());
        options.setProperty("mimeType", getContentType());
        options.setProperty("ignoreContent", "true");
        try {
            client.modifyDatastream(fedoraPid, dsId, options);
            setMetaChanged(false);
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
    }
}
