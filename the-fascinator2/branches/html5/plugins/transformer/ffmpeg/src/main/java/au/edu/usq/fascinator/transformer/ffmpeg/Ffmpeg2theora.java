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
 * Wrapper for the FFMPEG2THEORA program
 * 
 * @author Linda Octalina
 */
public class Ffmpeg2theora implements Ffmpeg {

    /** Logger */
    private Logger log = LoggerFactory.getLogger(Ffmpeg2theora.class);

    /** Transcoding binary */
    private String executable;

    /** Level of available functionality */
    private String availability = "Unknown";

    /**
     * Instantiate using default binaries
     * 
     */
    public Ffmpeg2theora() {
        this(DEFAULT_BIN_TRANSCODE_THEORA);
    }

    /**
     * Instantiate using provided binaries (if found)
     * 
     * @param executable Binary for transcoding
     * @param metadata Binary for metadata extraction
     */
    public Ffmpeg2theora(String executable) {
        this.executable = executable == null ? DEFAULT_BIN_TRANSCODE_THEORA
                : executable;
    }

    /**
     * What level of functionality is available from Ffmepg2theora on this
     * system
     * 
     * @return String indicating the level of available functionality
     */
    @Override
    public String testAvailability() {
        // Cache our response
        if (availability != null && availability.equals("Unknown")) {
            // Test our install
            boolean ffmpeg2theora = testConverter();

            if (ffmpeg2theora) {
                // An older FFmpeg build is installed
                availability = DEFAULT_BIN_TRANSCODE_THEORA;
            } else {
                // We couldn't find an install
                availability = null;
            }

        }

        return availability;
    }

    /**
     * Test for the existance of either the provided or default ogv, ogg
     * transcoder (ffmpeg2theora)
     * 
     * @return boolean Flag if it was found
     */
    private boolean testConverter() {
        // DEFAULT_EXECUTABLE = FFmpeg
        if (executable.contains(DEFAULT_BIN_TRANSCODE_THEORA)) {
            try {
                execute();
                return true;

                // Try searching for it on the path
            } catch (IOException ioe) {
                File found = searchPathForExecutable(DEFAULT_BIN_TRANSCODE_THEORA);
                if (found != null) {
                    executable = found.getAbsolutePath();
                    log.info("FFmpeg2Theora found at {}", executable);
                    try {
                        execute();
                        return true;
                    } catch (IOException ioe2) {
                        log.error("FFmpeg2Theora not functioning correctly!");
                    }
                }
            }
        }
        return false;
    }

    /**
     * Execute the binary without parameters to test for existance
     * 
     * @return Process that was executed
     * @throws IOException if execution failed
     */
    private Process execute() throws IOException {
        List<String> noParams = Collections.emptyList();
        return execute(noParams);
    }

    /**
     * Execute that binary with provided parameters
     * 
     * @param params to execute with
     * @return Process that was executed
     * @throws IOException if execution failed
     */
    private Process execute(List<String> params) throws IOException {
        List<String> cmd = new ArrayList<String>();
        cmd.add(executable);
        cmd.addAll(params);
        log.debug("Executing: {}", cmd);
        return new ProcessBuilder(cmd).start();
    }

    /**
     * Wait for the completion of the provided Process and stream its output to
     * the provided location.
     * 
     * @param proc Process to wait on
     * @param out Stream to route output
     * @return Process that was executed
     */
    private Process waitFor(Process proc, OutputStream out) {
        // FFmpeg2Theore, we're reading from STDERR
        new InputHandler("stderr", proc.getErrorStream(), out).start();
        new InputGobbler("stdout", proc.getInputStream()).start();
        try {
            // Wait for execution to finish
            proc.waitFor();
            // We need a tiny pause on Solaris
            // or consecutive calls will fail
            Thread.sleep(100);
        } catch (InterruptedException ie) {
            log.error("ffmpeg2Theora was interrupted!", ie);
            proc.destroy();
        }
        return proc;
    }

    /**
     * Start a process with the provided parameters, wait for its completion and
     * route the output to the provided stream
     * 
     * @param params List of parameters to provide the process
     * @param out Stream to route output
     * @return Process that was executed
     * @throws IOException if execution failed
     */
    private Process executeAndWait(List<String> params, OutputStream out)
            throws IOException {
        return waitFor(execute(params), out);
    }

    /**
     * Transform a file using the provided parameters
     * 
     * @param params List of parameters to provide the binary
     * @return String containing the raw output
     * @throws IOException if execution failed
     */
    @Override
    public String transform(List<String> params) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        executeAndWait(params, out);
        String response = out.toString("UTF-8");
        // if (response.contains("Compile-time maximum width")) {
        // throw new IOException("Maximum resolution exceeded!\n=====\n"
        // + response);
        // }
        return response;
    }

    /**
     * Search along the system path for an executable with the provided name
     * 
     * @param name of the exectuable to find
     * @return File on disk is found, null if not found
     */
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

    @Override
    public String extract(File file) throws IOException {
        return null;
    }

    @Override
    public FfmpegInfo getInfo(File inputFile) throws IOException {
        return null;
    }
}
