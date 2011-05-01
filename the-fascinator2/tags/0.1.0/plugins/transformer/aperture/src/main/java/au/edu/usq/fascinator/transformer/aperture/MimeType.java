/*
 * The Fascinator - Plugin - Transformer - Aperture
 * Copyright (C) 2009  University of Southern Queensland
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
package au.edu.usq.fascinator.transformer.aperture;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.semanticdesktop.aperture.mime.identifier.MimeTypeIdentifier;
import org.semanticdesktop.aperture.mime.identifier.magic.MagicMimeTypeIdentifier;
import org.semanticdesktop.aperture.util.IOUtil;

/**
 *
 */
public class MimeType {

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: MimeType filePath");
            System.exit(1);
        }
        System.out.println("MIME Type: " + getMimeType(args[0]));
    }

    public static String getMimeType(String filePath) {
        File file = new File(filePath);
        return MimeType.getMimeType(file);
    }

    public static String getMimeType(File file) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // create a MimeTypeIdentifier
        MimeTypeIdentifier identifier = new MagicMimeTypeIdentifier();

        // read as many bytes of the file as desired by the MIME type identifier

        BufferedInputStream buffer = new BufferedInputStream(stream);
        byte[] bytes = null;
        try {
            bytes = IOUtil.readBytes(buffer, identifier.getMinArrayLength());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            stream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // let the MimeTypeIdentifier determine the MIME type of this file
        String mimeType = identifier.identify(bytes, file.getPath(), null);
        // skip when the MIME type could not be determined
        if (mimeType == null) {
            System.err.println("MIME type could not be established.");
            return "";
        }
        return mimeType;
    }
}
