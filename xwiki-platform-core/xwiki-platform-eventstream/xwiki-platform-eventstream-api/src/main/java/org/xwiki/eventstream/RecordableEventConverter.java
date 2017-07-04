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
package org.xwiki.eventstream;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Convert a {@link RecordableEvent} to an {@link Event} that could be stored in the event stream.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Role
@Unstable
public interface RecordableEventConverter
{
    /**
     * Convert a notification event to a stream event.
     *
     * @param recordableEvent the event to convert
     * @param source the source received with the event
     * @param data the data received with the event
     * @return the converted stream event, ready to be stored
     * @throws Exception if error happens
     */
    Event convert(RecordableEvent recordableEvent, String source, Object data) throws Exception;

    /**
     * @return the list of events supported by this converter
     */
    List<RecordableEvent> getSupportedEvents();
}
