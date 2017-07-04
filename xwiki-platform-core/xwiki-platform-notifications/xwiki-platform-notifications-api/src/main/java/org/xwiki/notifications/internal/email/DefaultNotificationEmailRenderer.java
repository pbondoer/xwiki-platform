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
package org.xwiki.notifications.internal.email;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.email.NotificationEmailRenderer;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Default implementation of {@link NotificationEmailRenderer}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Singleton
public class DefaultNotificationEmailRenderer extends AbstractNotificationEmailRenderer
{
    @Inject
    private ComponentManager componentManager;

    private NotificationEmailRenderer getRenderer(CompositeEvent event)
    {
        try {
            return componentManager.getInstance(NotificationEmailRenderer.class, event.getType());
        } catch (ComponentLookupException e) {
            return null;
        }
    }

    @Override
    public String renderHTML(CompositeEvent event) throws NotificationException
    {
        NotificationEmailRenderer renderer = getRenderer(event);
        if (renderer != null) {
            return renderer.renderHTML(event);
        }

        return renderHTML(executeTemplate(event, "notification/email/%s.html.vm",
                Syntax.XHTML_1_0));
    }


    @Override
    public String renderPlainText(CompositeEvent event) throws NotificationException
    {
        NotificationEmailRenderer renderer = getRenderer(event);
        if (renderer != null) {
            return renderer.renderPlainText(event);
        }

        return renderPlainText(executeTemplate(event, "notification/email/%s.plain.vm",
                Syntax.PLAIN_1_0));
    }

    @Override
    public String generateEmailSubject(CompositeEvent event) throws NotificationException
    {
        NotificationEmailRenderer renderer = getRenderer(event);
        if (renderer != null) {
            return renderer.generateEmailSubject(event);
        }

        return renderPlainText(executeTemplate(event, "notification/email/%s.subject.vm",
                Syntax.PLAIN_1_0));
    }
}
