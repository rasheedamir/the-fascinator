/*
 * The Fascinator - Plugin - Transformer - FFMPEG
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
package au.edu.usq.fascinator.transformer.ffmpeg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for the FFMPEG program
 * 
 * @author Oliver Lucido
 */
public class FfmpegImpl implements Ffmpeg {

    public static final String DEFAULT_EXECUTABLE = "ffmpeg";

    private Logger log = LoggerFactory.getLogger(FfmpegImpl.class);

    private String executable;

    public FfmpegImpl() {
        this(DEFAULT_EXECUTABLE);
    }

    public FfmpegImpl(String executable) {
        this.executable = executable;
    }

    @Override
    public boolean isAvailable() {
        boolean available = false;
        try {
            execute();
            available = true;
        } catch (IOException ioe) {
            log.warn("ffmpeg execute failed! searching system path...");
            File found = searchPathForExecutable(executable);
            if (found != null) {
                executable = found.getAbsolutePath();
                log.info("ffmpeg found at {}", executable);
                try {
                    execute();
                    available = true;
                } catch (IOException ioe2) {
                }
            }
            log.error("ffmpeg not available!");
        }
        return available;
    }

    private Process execute() throws IOException {
        List<String> noParams = Collections.emptyList();
        return execute(noParams);
    }

    private Process execute(List<String> params) throws IOException {
        List<String> cmd = new ArrayList<String>();
        cmd.add(executable);
        cmd.addAll(params);
        log.debug("Executing: {}", cmd);
        return new ProcessBuilder(cmd).redirectErrorStream(true).start();
    }

    private Process waitFor(Process proc, OutputStream out) {
        new InputHandler("stdout", proc.getInputStream(), out).start();
        try {
            proc.waitFor();
        } catch (InterruptedException ie) {
            log.error("ffmpeg was interrupted!", ie);
            proc.destroy();
        }
        return proc;
    }

    private Process executeAndWait(List<String> params, OutputStream out)
            throws IOException {
        return waitFor(execute(params), out);
    }

    @Override
    public String executeAndWait(List<String> params) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        executeAndWait(params, out);
        return out.toString("UTF-8");
    }

    @Override
    public FfmpegInfo getInfo(File inputFile) throws IOException {
        return new FfmpegInfo(this, inputFile);
    }

    private File searchPathForExecutable(String name) {
        if (System.getProperty("os.name").startsWith("Windows")
                && !name.endsWith(".exe")) {
            name += ".exe";
        }
        String[] dirs = System.getenv("PATH").split(File.pathSeparator);
        for (String dir : dirs) {
            File file = new File(dir, name);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }
}
