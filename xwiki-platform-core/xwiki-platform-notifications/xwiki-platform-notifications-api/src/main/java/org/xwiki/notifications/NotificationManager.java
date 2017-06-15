/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.notifications;

import java.util.Date;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Get notifications for users.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Role
@Unstable
public interface NotificationManager
{
    /**
     * Return events to display as notifications concerning the specified user.
     *
     * @param userId id of the user
     * @param onlyUnread if only unread events should be returned
     * @param expectedCount the maximum events to return
     * @return the matching events for the user, could be less than expectedCount but not more
     * @throws NotificationException if error happens
     */
    List<CompositeEvent> getEvents(String userId, boolean onlyUnread, int expectedCount) throws NotificationException;

    /**
     * Return events to display as notifications concerning the specified user.
     *
     * @param userId id of the user
     * @param onlyUnread if only unread events should be returned
     * @param expectedCount the maximum events to return
     * @param untilDate do not return events happened after this date
     * @param blackList list of ids of blacklisted events to not return (to not get already known events again)
     * @return the matching events for the user, could be less than expectedCount but not more
     * @throws NotificationException if error happens
     */
    List<CompositeEvent> getEvents(String userId, boolean onlyUnread, int expectedCount, Date untilDate,
            List<String> blackList) throws NotificationException;

    /**
     * Return events to display as notifications concerning the specified user.
     *
     * @param userId id of the user
     * @param onlyUnread if only unread events should be returned
     * @param expectedCount the maximum events to return
     * @param untilDate do not return events happened after this date
     * @param fromDate do not return events happened before this date
     * @param blackList list of ids of blacklisted events to not return (to not get already known events again)
     * @return the matching events for the user, could be less than expectedCount but not more
     * @throws NotificationException if error happens
     */
    List<CompositeEvent> getEvents(String userId, boolean onlyUnread, int expectedCount, Date untilDate,
            Date fromDate, List<String> blackList) throws NotificationException;

    /**
     * Return events to display as notifications concerning the specified user.
     *
     * @param userId id of the user
     * @param format format of the notifications
     * @param onlyUnread if only unread events should be returned
     * @param expectedCount the maximum events to return
     * @param untilDate do not return events happened after this date
     * @param fromDate do not return events happened before this date
     * @param blackList list of ids of blacklisted events to not return (to not get already known events again)
     * @return the matching events for the user, could be less than expectedCount but not more
     * @throws NotificationException if error happens
     *
     * @since 9.5RC1
     */
    List<CompositeEvent> getEvents(String userId, NotificationFormat format, boolean onlyUnread, int expectedCount,
            Date untilDate, Date fromDate, List<String> blackList) throws NotificationException;

    /**
     * Return the number of events to display as notifications concerning the specified user.
     *
     * @param userId id of the user
     * @param onlyUnread either if only unread events should be counted or all events
     * @param maxCount maximum number of events to count
     * @return the list of events to display as notifications
     * @throws NotificationException if an error happens
     */
    long getEventsCount(String userId, boolean onlyUnread, int maxCount) throws NotificationException;

    /**
     * Set the start date for the given user.
     *
     * @param userId the id of the user
     * @param startDate the date before which we ignore notifications
     * @throws NotificationException if an error happens
     */
    void setStartDate(String userId, Date startDate) throws NotificationException;

    /**
     * @return the list of notifications preferences for the current user
     * @throws NotificationException if an error happens
     */
    List<NotificationPreference> getPreferences() throws NotificationException;

    /**
     * @param userId id of the user
     * @return the list of notifications preferences for a given user
     * @throws NotificationException if an error happens
     */
    List<NotificationPreference> getPreferences(String userId) throws NotificationException;
}
