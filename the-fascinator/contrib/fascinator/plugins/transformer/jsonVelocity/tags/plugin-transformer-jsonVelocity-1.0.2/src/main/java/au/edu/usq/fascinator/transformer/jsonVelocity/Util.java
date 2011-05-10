package au.edu.usq.fascinator.transformer.jsonVelocity;

import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonSimple;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for JsonVelocity Transformer
 * 
 * @author Linda Octalina
 * 
 */
public class Util {

    /** Logger */
    static Logger log = LoggerFactory.getLogger(JsonVelocityTransformer.class);

    /**
     * Getlist method to get the values of key from the sourceMap
     *
     * @param sourceMap Map container
     * @param baseKey field to search
     * @return list of value based on baseKey
     */
    public Map<String, Object> getList(Map<String, Object> sourceMap,
            String baseKey) {
        SortedMap<String, Object> valueMap = new TreeMap<String, Object>();
        Map<String, Object> data;

        if (baseKey == null) {
            log.error("NULL baseKey provided!");
            return valueMap;
        }
        if (!baseKey.endsWith(".")) {
            baseKey = baseKey + ".";
        }
        if (sourceMap == null) {
            log.error("NULL sourceMap provided!");
            return valueMap;
        }

        for (String key : sourceMap.keySet()) {
            if (key.startsWith(baseKey)) {

                String value = sourceMap.get(key).toString();
                String field = baseKey;
                if (key.length() >= baseKey.length()) {
                    field = key.substring(baseKey.length(), key.length());
                }

                String index = field;
                if (field.indexOf(".") > 0) {
                    index = field.substring(0, field.indexOf("."));
                }

                if (valueMap.containsKey(index)) {
                    data = (Map<String, Object>) valueMap.get(index);
                } else {
                    data = new LinkedHashMap<String, Object>();
                    valueMap.put(index, data);
                }

                if (value.length() == 1) {
                    value = String.valueOf(value.charAt(0));
                }

                data.put(field.substring(field.indexOf(".") + 1, field.length()), value);

            }
        }

        return valueMap;
    }

    /**
     * Getlist method to get the values of key from the sourceMap
     *
     * @param sourceMap Map container
     * @param baseKey field to search
     * @return list of value based on baseKey
     */
    public Map<String, Object> getList(JsonSimple json, String baseKey) {
        SortedMap<String, Object> valueMap = new TreeMap<String, Object>();
        Map<String, Object> data;

        if (baseKey == null) {
            log.error("NULL baseKey provided!");
            return valueMap;
        }
        if (!baseKey.endsWith(".")) {
            baseKey = baseKey + ".";
        }

        if (json == null) {
            log.error("NULL JSON object provided!");
            return valueMap;
        }

        // Look through the top level nodes
        for (Object oKey : json.getJsonObject().keySet()) {
            // If the key matches
            String key = (String) oKey;
            if (key.startsWith(baseKey)) {
                // Find our data
                String value = json.getString(null, key);
                String field = baseKey;

                if (key.length() >= baseKey.length()) {
                    field = key.substring(baseKey.length(), key.length());
                }

                String index = field;
                if (field.indexOf(".") > 0) {
                    index = field.substring(0, field.indexOf("."));
                }

                if (valueMap.containsKey(index)) {
                    data = (Map<String, Object>) valueMap.get(index);
                } else {
                    data = new LinkedHashMap<String, Object>();
                    valueMap.put(index, data);
                }

                if (value.length() == 1) {
                    value = String.valueOf(value.charAt(0));
                }

                data.put(field.substring(
                        field.indexOf(".") + 1, field.length()), value);

            }
        }

        //log.info("{}: {}", baseKey, valueMap);
        return valueMap;
    }

    /**
     * Trivial wrapper for call into JSON Library. Removes the difficulty of
     * dealing with a null argument and a vararg from Velocity.
     *
     * @param json: The JSON object to get from
     * @param field: The field in the JSON object to get
     * @return String: The data in the field, possibly NULL
     */
    public String get(JsonSimple json, Object... field) {
        if (json == null) {
            log.error("NULL JSON object provided!");
            return "";
        }

        return json.getString(null, field);
    }

    /**
     * Cleanup the supplied datetime value into a W3C format.
     *
     * @param dateTime Datetime to clean
     * @return String The cleaned value
     * @throws ParseException if and incorrect input is supplied
     */
    public String getW3CDateTime(String dateTime) throws ParseException {
        return getDateTime(dateTime, "yyyy-MM-dd'T'HH:mm:ssZ");
    }

    /**
     * Cleanup the supplied datetime value into a W3C format.
     *
     * @param dateTime Datetime to clean
     * @return String The cleaned value
     * @throws ParseException if and incorrect input is supplied
     */
    public String getDateTime(String dateTime, String format) throws ParseException {
        if (dateTime != null && !"".equals(dateTime)) {
            if (dateTime.indexOf("-") == -1) {
                dateTime = dateTime + "-01-01";
            } else {
                String[] part = dateTime.trim().split("-");
                if (part.length == 2) {
                    dateTime = dateTime + "-01";
                }
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat odf = new SimpleDateFormat(format);

            Date date = sdf.parse(dateTime);
            //log.info("ISO8601 Date:  {}", formatDate(date));
            //log.info("W3C Date:  {}", odf.format(date));
            return odf.format(date);
        }
        return "";
    }

    // ISO8601 Dates.   Lifted from this example:
    //  http://www.dpawson.co.uk/relaxng/schema/datetime.html
    private String formatDate(Date input) {
        // Base time
        SimpleDateFormat ISO8601Local = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss");
        TimeZone timeZone = TimeZone.getDefault();
        ISO8601Local.setTimeZone(timeZone);
        DecimalFormat twoDigits = new DecimalFormat("00");

        // Work out timezone offset
        int offset = ISO8601Local.getTimeZone().getOffset(input.getTime());
        String sign = "+";
        if (offset < 0) {
            offset = -offset;
            sign = "-";
        }
        int hours = offset / 3600000;
        int minutes = (offset - hours * 3600000) / 60000;

        // Put it all together
        return ISO8601Local.format(input) + sign
                + twoDigits.format(hours) + ":" + twoDigits.format(minutes);
    }

    /**
     * Utility method for accessing object properties. Since null testing
     * is awkward in velocity, an unset property is changed to en empty string
     * ie. ("").
     * 
     * @param object: The object to extract the property from
     * @param field: The field name of the property
     * @return String: The value of the property, or and empty string.
     */
    public String getMetadata(DigitalObject object, String field) {
        if (object == null) {
            log.error("NULL object provided!");
            return "";
        }

        try {
            Properties metadata = object.getMetadata();
            String result = metadata.getProperty(field);
            if (result == null) {
                return "";
            } else {
                return result;
            }
        } catch (StorageException ex) {
            log.error("Error accessing object metadata: ", ex);
            return "";
        }
    }

    /**
     * Safely escape the supplied string for use in XML.
     * 
     * @param value: The string to escape
     * @return String: The escaped string
     */
    public String encodeXml(String value) {
        return StringEscapeUtils.escapeXml(value);
    }
}
