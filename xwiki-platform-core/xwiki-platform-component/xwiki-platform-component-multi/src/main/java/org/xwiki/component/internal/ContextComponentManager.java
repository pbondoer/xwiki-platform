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
package org.xwiki.component.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.internal.multi.DelegateComponentManager;
import org.xwiki.component.manager.ComponentEventManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.context.Execution;
import org.xwiki.model.namespace.UserNamespace;

/**
 * Chains Component Managers to perform lookups based on the current execution context (current user, current wiki,
 * etc).
 * 
 * @version $Id$
 * @since 2.1RC1
 */
@Component
@Named("context")
@Singleton
public class ContextComponentManager extends DelegateComponentManager
{
    /**
     * The first Component Manager in the chain.
     */
    @Inject
    @Named(UserNamespace.TYPE)
    private ComponentManager userComponentManager;

    @Inject
    private ComponentManager rootComponentManager;

    @Inject
    private Execution execution;

    @Override
    public ComponentManager getComponentManager()
    {
        // If there is no context go directly to root CM
        if (this.execution.getContext() == null) {
            return this.rootComponentManager;
        } else {
            return this.userComponentManager;
        }
    }

    // Make the Context Component Manager "read-only". Writes should be done against specific Component Managers.

    @Override
    public <T> void registerComponent(ComponentDescriptor<T> componentDescriptor, T componentInstance)
        throws ComponentRepositoryException
    {
        throwException();
    }

    @Override
    public <T> void registerComponent(ComponentDescriptor<T> componentDescriptor) throws ComponentRepositoryException
    {
        throwException();
    }

    @Override
    public void setComponentEventManager(ComponentEventManager eventManager)
    {
        throwException();
    }

    @Override
    public void setParent(ComponentManager parentComponentManager)
    {
        throwException();
    }

    /**
     * Exception to throw when trying to access a write method since this Component Manager is a chaining Component
     * Manager and should only be used for read-only access.
     */
    private void throwException()
    {
        throw new RuntimeException("The Context Component Manager should only be used for read access. Write "
            + "operations should be done against specific Component Managers.");
    }
}
