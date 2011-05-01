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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic information about a media file
 * 
 * @author Oliver Lucido
 */
public class FfmpegInfo {

    private boolean supported = false;

    private boolean audio = false;

    private boolean video = false;

    private long duration = 0;

    public FfmpegInfo(Ffmpeg ffmpeg, File inputFile) throws IOException {
        List<String> params = new ArrayList<String>();
        params.add("-i");
        params.add(inputFile.getAbsolutePath());
        String stderr = ffmpeg.executeAndWait(params);
        // check if supported
        if (supported = (stderr.indexOf(": Unknown format") == -1)) {
            // get duration
            Pattern p = Pattern.compile("Duration: ((\\d+):(\\d+):(\\d+))");
            Matcher m = p.matcher(stderr);
            if (m.find()) {
                long hrs = Long.parseLong(m.group(2)) * 3600;
                long min = Long.parseLong(m.group(3)) * 60;
                long sec = Long.parseLong(m.group(4));
                duration = hrs + min + sec;
            }
            // check for video
            video = Pattern.compile("Stream #.*Video:.*").matcher(stderr)
                    .find();
            // check for audio
            audio = Pattern.compile("Stream #.*Audio:.*").matcher(stderr)
                    .find();
        }
    }

    public boolean isSupported() {
        return supported;
    }

    public boolean hasAudio() {
        return audio;
    }

    public boolean hasVideo() {
        return video;
    }

    public long getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return "Supported: " + supported + ", Audio: " + audio + ", Video: "
                + video + ", Duration: " + duration + " seconds";
    }
}
