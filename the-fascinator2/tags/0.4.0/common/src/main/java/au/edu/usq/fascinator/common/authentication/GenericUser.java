/*
 * The Fascinator - Generic User
 * Copyright (C) 2008-2010 University of Southern Queensland
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

package au.edu.usq.fascinator.common.authentication;

import au.edu.usq.fascinator.common.JsonConfigHelper;
import au.edu.usq.fascinator.api.authentication.User;

import java.lang.reflect.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic user object, does not define its metadata schema, that is left
 * to extending classes, but creates access methods against an unknown
 * schema.
 *
 * @author Greg Pendlebury
 */
public class GenericUser implements User {
    private Logger log = LoggerFactory.getLogger(GenericUser.class);
    private JsonConfigHelper response;
    private String username;
    private String authenticationSource;

    /**
     * Will return a JSON string description of an extending classes'
     * fields.
     *
     * @param property The class field to retrieve
     * @return The value of the property
     */
    @Override
    public final String describeMetadata() {
        // Grab the class name which is extending
        String class_name = this.getClass().getCanonicalName();
        try {
            Class ref_class = Class.forName(class_name);
            Field field_list[] = ref_class.getDeclaredFields();
            response = new JsonConfigHelper();

            // Arbitrarily set username first, as its a private field
            // of GenericUser inheriting classes can't access it.
            response.set("username", "String");

            for (int i = 0; i < field_list.length; i++) {
                response.set(field_list[i].getName(), field_list[i].getType().getSimpleName());
            }
            return response.toString();
        } catch (ClassNotFoundException ex) {
            log.error(ex.getMessage());
            return "Error retrieving user specification";
        }
    }

    /**
     * Retrieves a given property for this user object.
     *
     * @param property The class field to retrieve
     * @return The value of the property
     */
    @Override
    public final String get(String property) {
        Field field_list[] = this.getClass().getFields();
        for (int i = 0; i < field_list.length; i++) {
            if (property.equals(field_list[i].getName())) {
                try {
                    return field_list[i].get(this).toString();
                } catch (IllegalArgumentException ex) {
                    log.error("User Object, Illegal argument : {}", ex);
                    return null;
                } catch (IllegalAccessException ex) {
                    log.error("User Object, Illegal access : {}", ex);
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public String realName() {
        return username;
    }

    public void setUsername(String newName) {
        username = newName;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void setSource(String plugin) {
        authenticationSource = plugin;
    }

    @Override
    public String getSource() {
        return authenticationSource;
    }
}
