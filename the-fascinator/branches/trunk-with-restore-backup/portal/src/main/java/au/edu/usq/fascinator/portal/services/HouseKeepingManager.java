/*
 * The Fascinator - Portal
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
package au.edu.usq.fascinator.portal.services;

import java.util.Map;
import org.apache.tapestry5.ioc.services.RegistryShutdownListener;

/**
 * Provides an interface for the Portal to access House Keeping.
 *
 * @author Greg Pendlebury
 */
public interface HouseKeepingManager extends RegistryShutdownListener {

    /**
     * Are there any House Keeping actions requiring attention from the user.
     *
     * @returns boolean flag
     */
    public boolean requiresAction();

    /**
     * Get the message House Keeping wants to display to the user.
     *
     * @returns String containing the message to display.
     */
    public String getMessage();

    /**
     * Get the template House Keeping wants to frame its message in.
     *
     * @returns String containing the template to use.
     */
    public String getTemplate();

    /**
     * Send a confirmation of the message receipt to HouseKeeping.
     *
     */
    public void confirmMessage();

    /**
     * Send a message to HouseKeeping.
     *
     */
    public void sendMessage(String message);

    /**
     * Request a low priority restart from HouseKeeping.
     *
     */
    public void requestRestart();

    /**
     * Request a high priority restart from HouseKeeping.
     * High priority will stop all user actions until the restart
     * occurs.
     *
     */
    public void requestUrgentRestart();

    /**
     * Get the latest statistics on message queues.
     *
     * @return Map<String, Map<String, String>> of all queues and their statistics
     */
    public Map<String, Map<String, String>> getQueueStats();
}
