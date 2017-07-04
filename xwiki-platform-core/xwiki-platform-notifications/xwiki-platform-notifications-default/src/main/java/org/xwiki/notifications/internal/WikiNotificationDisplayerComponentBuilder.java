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
package org.xwiki.notifications.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.WikiBaseObjectComponentBuilder;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * This component allows the definition of a {@link org.xwiki.notifications.NotificationDisplayer} in wiki pages.
 * It uses {@link UntypedRecordableEvent#getEventType} to be bound to a specific event type.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Named(WikiNotificationDisplayerDocumentInitializer.XCLASS_NAME)
@Singleton
public class WikiNotificationDisplayerComponentBuilder implements WikiBaseObjectComponentBuilder
{
    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Override
    public EntityReference getClassReference()
    {
        return new EntityReference(
                WikiNotificationDisplayerDocumentInitializer.XCLASS_NAME,
                EntityType.OBJECT);
    }

    @Override
    public List<WikiComponent> buildComponents(BaseObject baseObject) throws WikiComponentException
    {
        try {
            // Check that the document owner is allowed to build the components
            XWikiDocument parentDocument = baseObject.getOwnerDocument();
            this.checkRights(parentDocument.getDocumentReference(), parentDocument.getAuthorReference());

            // Instantiate the component
            return Arrays.asList(
                    new WikiNotificationDisplayer(parentDocument.getAuthorReference(),
                            this.templateManager, this.scriptContextManager, this.componentManager, baseObject));
        } catch (Exception e) {
            throw new WikiComponentException(String.format(
                    "Unable to build the WikiNotificationDisplayer wiki component "
                            + "for [%s].", baseObject), e);
        }
    }

    /**
     * Ensure that the given author has the administrative rights in the current context.
     *
     * @param documentReference the working entity
     * @param authorReference the author that should have its rights checked
     * @throws NotificationException if the author rights are not sufficient
     */
    private void checkRights(DocumentReference documentReference, DocumentReference authorReference)
            throws NotificationException
    {
        if (!this.authorizationManager.hasAccess(Right.ADMIN, authorReference, documentReference.getWikiReference())) {
            throw new NotificationException(
                    "Registering custom Notification Displayers requires wiki administration rights.");
        }
    }
}
