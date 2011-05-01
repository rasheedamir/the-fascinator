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

import au.edu.usq.fascinator.common.JsonConfigHelper;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper class for database access to collect FFmpeg transcoding information
 * 
 * This class can be used for statistical collection, as well as to support
 * decision making on whether a re-transcode is required.
 *
 * @author Greg Pendlebury
 */
public class FfmpegDatabase {
    /** Logging */
    private final Logger log = LoggerFactory.getLogger(FfmpegDatabase.class);

    /** JDBC Driver */
    private static String DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    /** Connection string prefix */
    private static String DERBY_PROTOCOL = "jdbc:derby:";

    /** FFmpeg database name */
    private static String FFMPEG_DATABASE = "ffmpegTranscodings";

    /** Basic statistics table */
    private static String STATS_TABLE = "transcodings";

    /** Hash table */
    private static String HASH_TABLE = "hashes";

    /** Database home directory */
    private String derbyHome;

    /** Database connection */
    private Connection conn;

    /** SQL Statement */
    private Statement sql;

    /** Result set */
    private ResultSet result;

    /** Insert statement */
    private PreparedStatement insert;

    /** Select statement */
    private PreparedStatement select;

    /** Insert statement - Hashes */
    private PreparedStatement insertHash;

    /** Select statement - Hashes */
    private PreparedStatement selectHash;

    public FfmpegDatabase(JsonConfigHelper config) throws Exception {
        // Find our database home directory
        derbyHome = config.get("database/derbyHome");
        if (derbyHome == null) {
            throw new Exception("Database home not specified!");

        } else {
            // Establish its validity and existance, create if necessary
            File file = new File(derbyHome);
            if (file.exists()) {
                if (!file.isDirectory()) {
                    throw new Exception("Database home '" + derbyHome +
                            "' is not a directory!");
                }
            } else {
                file.mkdirs();
                if (!file.exists()) {
                    throw new Exception("Database home '" + derbyHome +
                            "' does not exist and could not be created!");
                }
            }
        }
        // Set the system property to match, the DriverManager will look here
        System.setProperty("derby.system.home", derbyHome);

        // Load the JDBC driver
        try {
            Class.forName(DERBY_DRIVER).newInstance();
        } catch (Exception ex) {
            log.error("Driver load failed: ", ex);
            throw new Exception("Driver load failed: ", ex);
        }

        // Database prep work
        Properties props = new Properties();
        try {
            // Establish a database connection, create the database if needed
            conn = DriverManager.getConnection(DERBY_PROTOCOL +
                    FFMPEG_DATABASE + ";create=true", props);
            sql = conn.createStatement();

            // Look for our tables
            checkTable(STATS_TABLE);
            checkTable(HASH_TABLE);
        } catch (SQLException ex) {
            log.error("Error during database preparation:", ex);
            throw new Exception(
                    "Error during database preparation:", ex);
        }
        log.debug("Derby security database online!");
    }

    /**
     * Shutdown the database connections and cleanup.
     *
     * @throws Exception if there are errors
     */
    public void shutdown() throws Exception {
        // Release all our queries
        close(sql);
        close(insert);
        close(select);

        // Derby can only be shutdown from one thread,
        //    we'll catch errors from the rest.
        String threadedShutdownMessage = DERBY_DRIVER
                + " is not registered with the JDBC driver manager";
        try {
            // Tell the database to close
            DriverManager.getConnection(DERBY_PROTOCOL + ";shutdown=true");
            // Shutdown just this database (but not the engine)
            //DriverManager.getConnection(DERBY_PROTOCOL + SECURITY_DATABASE +
            //        ";shutdown=true");
        } catch (SQLException ex) {
            // These test values are used if the engine is NOT shutdown
            //if (ex.getErrorCode() == 45000 &&
            //        ex.getSQLState().equals("08006")) {

            // Valid response
            if (ex.getErrorCode() == 50000 &&
                    ex.getSQLState().equals("XJ015")) {
            // Error response
            } else {
                // Make sure we ignore simple thread issues
                if (!ex.getMessage().equals(threadedShutdownMessage)) {
                    throw new Exception("Error during database shutdown:", ex);
                }
            }
        } finally {
            try {
                // Close our connection
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException ex) {
                throw new Exception("Error closing connection:", ex);
            }
        }
    }

    /**
     * Retrieve from the database the timestamp of the last time this particular
     * transcoding was performed.
     *
     * @param oid: The object identifier of the source
     * @param render: The render string used to transcode
     * @param source: The source file transcoded from
     * @param output: The output file transcoded to
     * @param resolution: The resolution of the output
     * @return long: The timestamp when the transcoding occurred
     */
    public long getLastTranscoded(String oid, String render, String source,
            String output, String resolution) {
        // MD5 hash the render string
        String hash = DigestUtils.md5Hex(render);

        try {
            // First run
            if (select == null) {
                select = conn.prepareStatement("SELECT MAX(datetime)as time FROM " +
                        STATS_TABLE + " WHERE oid = ? AND renderhash = ? " +
                        "AND infile = ? AND outfile = ? AND resolution = ?");
            }

            // Prepare and execute
            select.setString(1, oid);
            select.setString(2, hash);
            select.setString(3, source);
            select.setString(4, output);
            select.setString(5, resolution);
            result = select.executeQuery();

            // Build response
            Timestamp ts = null;
            if (result.next()) {
                ts = result.getTimestamp("time");
            }
            close(result);

            if (ts == null) {
                return -1;
            } else {
                return ts.getTime();
            }
        } catch(SQLException ex) {
            log.error("Error querying transcoding information: ", ex);
            return -1;
        }
    }

    /**
     * Store the transcoding data in the provided map
     *
     * @param data: A map of transcoding data from the transformer
     */
    public void storeTranscoding(Map<String, String> data) throws Exception {
        String oid = data.get("oid");
        long datetime = Long.parseLong(data.get("datetime"));
        Timestamp timestamp = new Timestamp(datetime);
        int timespent = Integer.valueOf(data.get("timespent"));
        int mediaduration = Integer.valueOf(data.get("mediaduration"));
        String renderhash = getHash(data.get("renderString"));
        String inresolution = data.get("inresolution");
        String outresolution = data.get("outresolution");
        String insize = data.get("insize");
        String outsize = data.get("outsize");
        String infile = data.get("infile");
        String outfile = data.get("outfile");

        // First run
        if (insert == null) {
            insert = conn.prepareStatement("INSERT INTO " + STATS_TABLE +
                    " (oid, datetime, timespent, mediaduration, renderhash, " +
                    "inresolution, outresolution, insize, outsize, infile, " +
                    "outfile) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        }

        // Prepare and execute
        insert.setString(1, oid);
        insert.setTimestamp(2, timestamp);
        insert.setInt(3, timespent);
        insert.setInt(4, mediaduration);
        insert.setString(5, renderhash);
        insert.setString(6, inresolution);
        insert.setString(7, outresolution);
        insert.setString(8, insize);
        insert.setString(9, outsize);
        insert.setString(10, infile);
        insert.setString(11, outfile);
        insert.executeUpdate();
    }

    /**
     * Take a list of parameters and hash them. Also encapsulates all logic
     * required to ensure that the hash is reversible from the database at a
     * later date.
     *
     * @param params: The list of parameters to hash
     * @return String: The hash to use in its place
     */
    private String getHash(String renderString) {
        String hash = DigestUtils.md5Hex(renderString);

        try {
            // First run
            if (selectHash == null) {
                selectHash = conn.prepareStatement("SELECT count(*) as total FROM "
                        + HASH_TABLE + " WHERE hash = ?");
            }

            // Prepare and execute
            selectHash.setString(1, hash);
            result = selectHash.executeQuery();

            // Check response
            boolean stored = false;
            if (result.next()) {
                if (result.getInt("total") == 1) {
                    stored = true;
                }
            }
            close(result);

            // If the hash is already in the database we are done
            if (stored) {
                return hash;
            }

            // Store the hash
            storeHash(hash, renderString);

        } catch(SQLException ex) {
            log.error("Error storing MD5 hash: ", ex);
        }

        // Make sure we return the hash, even if we failed to store it
        return hash;
    }

    /**
     * Store the hash and its original render string in the database
     *
     * @param hash: The MD5 hash to store
     * @param string: The string that made the hash
     * @throws SQLException if there were database errors during storage
     */
    private void storeHash(String hash, String string) throws SQLException {
        // First run
        if (insertHash == null) {
            insertHash = conn.prepareStatement("INSERT INTO " + HASH_TABLE +
                    " (hash, renderstring) VALUES (?, ?)");
        }

        // Prepare and execute
        insertHash.setString(1, hash);
        insertHash.setString(2, string);
        insertHash.executeUpdate();
    }

    /**
     * Check for the existence of a table and arrange for its creation if
     * not found.
     *
     * @param table The table to look for and create.
     * @throws SQLException if there was an error.
     */
    private void checkTable(String table) throws SQLException {
        boolean tableFound = findTable(table);

        // Create the table if we couldn't find it
        if (!tableFound) {
            log.debug("Table '{}' not found, creating now!", table);
            createTable(table);

            // Double check it was created
            if (!findTable(table)) {
                log.error("Unknown error creating table '{}'", table);
                throw new SQLException(
                        "Could not find or create table '" + table + "'");
            }
        }
    }

    /**
     * Check if the given table exists in the database.
     *
     * @param table The table to look for
     * @return boolean flag if the table was found or not
     * @throws SQLException if there was an error accessing the database
     */
    private boolean findTable(String table) throws SQLException {
        boolean tableFound = false;
        DatabaseMetaData meta = conn.getMetaData();
        result = (ResultSet) meta.getTables(null, null, null, null);
        while (result.next() && !tableFound) {
            if (result.getString("TABLE_NAME").equalsIgnoreCase(table)) {
                tableFound = true;
            }
        }
        close(result);
        return tableFound;
    }

    /**
     * Create the given table in the database.
     *
     * @param table The table to create
     * @throws SQLException if there was an error during creation,
     *                      or an unknown table was specified.
     */
    private void createTable(String table) throws SQLException {
        if (table.equals(STATS_TABLE)) {
            sql.execute(
                    "CREATE TABLE " + STATS_TABLE +
                    "(id INT NOT NULL GENERATED ALWAYS AS IDENTITY, " +
                    "oid VARCHAR(255) NOT NULL, " +
                    "datetime TIMESTAMP NOT NULL, " +
                    "timespent INT NOT NULL, " +
                    "mediaduration INT NOT NULL, " +
                    "renderhash VARCHAR(255) NOT NULL, " +
                    "inresolution VARCHAR(30) NOT NULL, " +
                    "outresolution VARCHAR(30) NOT NULL, " +
                    "insize INT NOT NULL, " +
                    "outsize INT NOT NULL, " +
                    "infile VARCHAR(255) NOT NULL, " +
                    "outfile VARCHAR(255) NOT NULL, " +
                    "PRIMARY KEY (id))");

            // ====================
            // Create indexes, use DESC if the field is expected
            //      to have MIN/MAX queries run against it
            // ====================

            // Last time this transcoding was done
            sql.execute(
                    "CREATE INDEX a ON " + STATS_TABLE + "(datetime DESC, " +
                    "oid, renderhash, inFile, outFile, outResolution)");
            // History of transcodings for this file output
            sql.execute(
                    "CREATE INDEX b ON " + STATS_TABLE + "(oid, outFile)");
            // History of transcodings for this object
            sql.execute(
                    "CREATE INDEX c ON " + STATS_TABLE + "(oid)");
            return;
        }
        if (table.equals(HASH_TABLE)) {
            sql.execute(
                    "CREATE TABLE " + HASH_TABLE +
                    "(hash VARCHAR(255) NOT NULL, " +
                    "renderstring VARCHAR(255) NOT NULL, " +
                    "PRIMARY KEY (hash))");
            return;
        }
        throw new SQLException("Unknown table '" + table + "' requested!");
    }

    /**
     * Attempt to close a ResultSet. Basic wrapper for exception
     * catching and logging
     *
     * @param resultSet The ResultSet to try and close.
     */
    private void close(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException ex) {
                log.error("Error closing result set: ", ex);
            }
        }
        resultSet = null;
    }

    /**
     * Attempt to close a Statement. Basic wrapper for exception
     * catching and logging
     *
     * @param statement The Statement to try and close.
     */
    private void close(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ex) {
                log.error("Error closing statement: ", ex);
            }
        }
        statement = null;
    }
}
