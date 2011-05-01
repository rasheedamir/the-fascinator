package au.edu.usq.fascinator.transformer.ffmpeg;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FfmpegMockImpl implements Ffmpeg {

    private Logger log = LoggerFactory.getLogger(FfmpegMockImpl.class);

    @Override
    public String executeAndWait(List<String> params) throws IOException {
        log.debug("executeAndWait: {}", params);
        int len = params.size();
        String resource = "/default.txt";
        if (len == 2) {
            resource = "/identify.txt";
        } else if (len == 20) {
            File srcFile = new File(params.get(1));
            File destFile = new File(params.get(len - 1));
            FileUtils.copyFile(srcFile, destFile);
            resource = "/transform.txt";
        }
        return IOUtils.toString(getClass().getResourceAsStream(resource));
    }

    @Override
    public FfmpegInfo getInfo(File inputFile) throws IOException {
        return new FfmpegInfo(this, inputFile);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
