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

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import javax.script.ScriptContext;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationDisplayer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * This class is meant to be instanciated and then registered to the Component Manager by the
 * {@link WikiNotificationDisplayerComponentBuilder} component every time a document containing a
 * NotificationDisplayerClass is added, updated or deleted.
 *
 * @version $Id$
 * @since 9.5RC1
 */
public class WikiNotificationDisplayer implements WikiComponent, NotificationDisplayer
{
    private static final String EVENT_BINDING_NAME = "event";

    private TemplateManager templateManager;

    private ScriptContextManager scriptContextManager;

    private ComponentManager componentManager;

    private BaseObjectReference objectReference;

    private DocumentReference authorReference;

    private String eventType;

    private Template notificationTemplate;

    private List<String> supportedEvents;

    /**
     * Constructs a new {@link WikiNotificationDisplayer}.
     *
     * @param authorReference the author reference of the document
     * @param templateManager the {@link TemplateManager} to use
     * @param scriptContextManager the {@link ScriptContextManager} to use
     * @param componentManager the {@link ComponentManager} to use
     * @param baseObject the XObject which has the required properties to instantiate the component
     * @throws NotificationException if the properties of the given BaseObject could not be loaded
     */
    public WikiNotificationDisplayer(DocumentReference authorReference, TemplateManager templateManager,
            ScriptContextManager scriptContextManager, ComponentManager componentManager, BaseObject baseObject)
            throws NotificationException
    {
        this.objectReference = baseObject.getReference();
        this.authorReference = authorReference;
        this.templateManager = templateManager;
        this.scriptContextManager = scriptContextManager;
        this.componentManager = componentManager;

        this.eventType = this.extractProperty(baseObject, WikiNotificationDisplayerDocumentInitializer.EVENT_TYPE);
        this.supportedEvents = Arrays.asList(this.eventType);

        // Create the template from the given BaseObject property
        try {
            String xObjectTemplate = this.extractProperty(baseObject,
                    WikiNotificationDisplayerDocumentInitializer.NOTIFICATION_TEMPLATE);
            if (!StringUtils.isBlank(xObjectTemplate)) {
                this.notificationTemplate = templateManager.createStringTemplate(
                        xObjectTemplate, this.getAuthorReference());
            } else {
                this.notificationTemplate = null;
            }

        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Unable to render the template provided in the base object [%s]",
                            baseObject), e);
        }
    }

    /**
     * Extract the the given property value from the given XObject.
     *
     * @param baseObject the XObject that should contain the parameters
     * @param propertyName the value of the property that should be extracted
     * @throws NotificationException if an error occurred while extracting the parameter from the base object
     */
    private String extractProperty(BaseObject baseObject, String propertyName) throws NotificationException
    {
        try {
            return baseObject.getStringValue(propertyName);
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Unable to extract the parameter [%s] from the [%s] NotificationDisplayerClass.",
                            propertyName, baseObject), e);
        }
    }

    @Override
    public Block renderNotification(CompositeEvent eventNotification) throws NotificationException
    {
        // Save the old value in the context that refers to EVENT_BINDING_NAME
        Object oldContextAttribute = scriptContextManager.getCurrentScriptContext().getAttribute(EVENT_BINDING_NAME,
                ScriptContext.ENGINE_SCOPE);

        try {
            // Allow the template to access the event during its execution
            scriptContextManager.getCurrentScriptContext().setAttribute(EVENT_BINDING_NAME, eventNotification,
                    ScriptContext.ENGINE_SCOPE);

            // If we have no template defined, fallback on the default displayer
            if (this.notificationTemplate == null) {
                return ((NotificationDisplayer) this.componentManager.getInstance(NotificationDisplayer.class))
                        .renderNotification(eventNotification);
            }

            return templateManager.execute(notificationTemplate);

        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Unable to render notification template for the [%s].", this.eventType), e);
        } finally {
            // Restore the old object associated with EVENT_BINDING_NAME
            scriptContextManager.getCurrentScriptContext().setAttribute(EVENT_BINDING_NAME, oldContextAttribute,
                    ScriptContext.ENGINE_SCOPE);
        }
    }

    @Override
    public List<String> getSupportedEvents()
    {
        return this.supportedEvents;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return (DocumentReference) this.objectReference.getParent();
    }

    @Override
    public EntityReference getEntityReference()
    {
        return this.objectReference;
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    @Override
    public Type getRoleType()
    {
        return NotificationDisplayer.class;
    }

    @Override
    public String getRoleHint()
    {
        return this.eventType;
    }

    @Override
    public WikiComponentScope getScope()
    {
        return WikiComponentScope.WIKI;
    }
}
