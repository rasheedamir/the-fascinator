package au.edu.usq.fascinator.transformer.ims;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.common.MimeTypeUtil;
import au.edu.usq.fascinator.common.storage.impl.GenericPayload;

public class ImsPayload extends GenericPayload{

    private File filePath;
    private boolean isZip = false;
    private ZipEntry zipEntry;

    public ImsPayload(File filePath, ZipEntry zipEntry) {
        this.filePath = filePath;
        this.zipEntry = zipEntry;
        String name = filePath.getName();
        if (this.zipEntry != null) {
            isZip = true;
            name = zipEntry.getName();
        }
        setId(name);
        setLabel(name);
        setContentType(MimeTypeUtil.getMimeType(name));
        setType(PayloadType.Enrichment); // need to store somewhere
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (isZip) {
            ZipFile zipFile = new ZipFile(filePath);
            return zipFile.getInputStream(zipEntry);
        } else {
            return new FileInputStream(filePath);
        }
    }
}
