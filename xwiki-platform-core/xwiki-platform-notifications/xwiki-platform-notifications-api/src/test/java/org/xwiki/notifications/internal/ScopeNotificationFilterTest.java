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
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class ScopeNotificationFilterTest
{
    public static final WikiReference SCOPE_REFERENCE_1 = new WikiReference("wiki1");

    public static final SpaceReference SCOPE_REFERENCE_2 = new SpaceReference("space2", new WikiReference("wiki2"));

    public static final DocumentReference SCOPE_REFERENCE_3 = new DocumentReference("wiki3", "space3", "page3");

    @Rule
    public final MockitoComponentMockingRule<ScopeNotificationFilter> mocker =
            new MockitoComponentMockingRule<>(ScopeNotificationFilter.class);

    private ModelBridge modelBridge;
    private EntityReferenceSerializer<String> serializer;
    @Before
    public void setUp() throws Exception
    {
        modelBridge = mocker.getInstance(ModelBridge.class, "cached");
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
    }

    @Test
    public void filterEvent() throws Exception
    {
        createPreferenceScopeMocks();

        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");

        Event event1 = mock(Event.class);
        when(event1.getType()).thenReturn("event1");
        when(event1.getDocument()).thenReturn(
                new DocumentReference("wiki1", "Main", "WebHome"));
        assertFalse(mocker.getComponentUnderTest().filterEvent(event1, user, NotificationFormat.ALERT));

        Event event2 = mock(Event.class);
        when(event2.getType()).thenReturn("event1");
        when(event2.getDocument()).thenReturn(
                new DocumentReference("someOtherWiki", "Main", "WebHome"));
        assertTrue(mocker.getComponentUnderTest().filterEvent(event2, user, NotificationFormat.ALERT));

        Event event3 = mock(Event.class);
        when(event3.getType()).thenReturn("event2");
        when(event3.getDocument()).thenReturn(
                new DocumentReference("wiki2", "space2", "WebHome"));
        assertFalse(mocker.getComponentUnderTest().filterEvent(event3, user, NotificationFormat.ALERT));

        Event event3bis = mock(Event.class);
        when(event3bis.getType()).thenReturn("event2");
        when(event3bis.getDocument()).thenReturn(
                new DocumentReference("wiki2", Arrays.asList("space2", "subspace"), "WebHome"));
        assertFalse(mocker.getComponentUnderTest().filterEvent(event3bis, user, NotificationFormat.ALERT));

        Event event4 = mock(Event.class);
        when(event4.getType()).thenReturn("event2");
        when(event4.getDocument()).thenReturn(
                new DocumentReference("wiki2", "otherSpace", "WebHome"));
        assertTrue(mocker.getComponentUnderTest().filterEvent(event4, user, NotificationFormat.ALERT));

        Event event5 = mock(Event.class);
        when(event5.getType()).thenReturn("event3");
        when(event5.getDocument()).thenReturn(
                new DocumentReference("wiki3", "space3", "page3"));
        assertFalse(mocker.getComponentUnderTest().filterEvent(event5, user, NotificationFormat.ALERT));

        Event event6 = mock(Event.class);
        when(event6.getType()).thenReturn("event3");
        when(event6.getDocument()).thenReturn(
                new DocumentReference("wiki3", "space3", "otherPage"));
        assertTrue(mocker.getComponentUnderTest().filterEvent(event6, user, NotificationFormat.ALERT));

        Event event7 = mock(Event.class);
        when(event7.getType()).thenReturn("eventWeDontCare");
        assertFalse(mocker.getComponentUnderTest().filterEvent(event7, user, NotificationFormat.ALERT));
    }

    @Test
    public void queryFilterOR() throws Exception
    {
        // Mocks
        createPreferenceScopeMocks();

        // Verify
        assertEquals(
                String.format("(event.wiki = :wiki_scopeNotifFilter_%s)",
                    Integer.toHexString("event1".hashCode())),
                mocker.getComponentUnderTest().queryFilterOR(
                    new DocumentReference("xwiki", "XWiki", "User"),
                    NotificationFormat.ALERT,
                "event1"
        ));

        assertEquals(
                String.format("(event.wiki = :wiki_scopeNotifFilter_%s AND event.space LIKE :space_scopeNotifFilter_%s)",
                    Integer.toHexString("event2".hashCode()), Integer.toHexString("event2".hashCode())),
                mocker.getComponentUnderTest().queryFilterOR(
                    new DocumentReference("xwiki", "XWiki", "User"),
                    NotificationFormat.ALERT,
                "event2"
        ));

        assertEquals(
                String.format("(event.wiki = :wiki_scopeNotifFilter_%s AND event.page = :page_scopeNotifFilter_%s)",
                    Integer.toHexString("event3".hashCode()), Integer.toHexString("event3".hashCode())),
                mocker.getComponentUnderTest().queryFilterOR(
                    new DocumentReference("xwiki", "XWiki", "User"),
                    NotificationFormat.ALERT,
                "event3"
        ));
    }

    private void createPreferenceScopeMocks() throws NotificationException
    {
        NotificationPreferenceScope scope1 = mock(NotificationPreferenceScope.class);
        when(scope1.getScopeReference()).thenReturn(
                SCOPE_REFERENCE_1
        );
        when(scope1.getEventType()).thenReturn("event1");

        NotificationPreferenceScope scope2 = mock(NotificationPreferenceScope.class);
        when(scope2.getScopeReference()).thenReturn(
                SCOPE_REFERENCE_2
        );
        when(scope2.getEventType()).thenReturn("event2");

        NotificationPreferenceScope scope3 = mock(NotificationPreferenceScope.class);
        when(scope3.getScopeReference()).thenReturn(
                SCOPE_REFERENCE_3
        );
        when(scope3.getEventType()).thenReturn("event3");

        when(modelBridge.getNotificationPreferenceScopes(any(DocumentReference.class),
                any(NotificationFormat.class))).thenReturn(Arrays.asList(scope1, scope2, scope3)
        );
    }

    @Test
    public void queryFilterAND() throws Exception
    {
        assertNull(
                mocker.getComponentUnderTest().queryFilterAND(
                        new DocumentReference("xwiki", "XWiki", "User"),
                        NotificationFormat.ALERT,
                        "type1"
                )
        );
    }

    @Test
    public void queryFilterParams() throws Exception
    {
        // Mocks
        createPreferenceScopeMocks();
        when(serializer.serialize(SCOPE_REFERENCE_1)).thenReturn("wiki1");
        when(serializer.serialize(SCOPE_REFERENCE_2)).thenReturn("space2");
        when(serializer.serialize(SCOPE_REFERENCE_3)).thenReturn("space3.page3");

        // Test
        Map<String, Object> results = mocker.getComponentUnderTest().queryFilterParams(
                new DocumentReference("xwiki", "XWiki", "User"),
                NotificationFormat.ALERT
        );

        // Verify
        assertEquals("wiki1", results.get(
                String.format("wiki_scopeNotifFilter_%s", Integer.toHexString("event1".hashCode()))));
        assertEquals("wiki2", results.get(
                String.format("wiki_scopeNotifFilter_%s", Integer.toHexString("event2".hashCode()))));
        assertEquals("space2.", results.get(
                String.format("space_scopeNotifFilter_%s", Integer.toHexString("event2".hashCode()))));
        assertEquals("wiki3", results.get(
                String.format("wiki_scopeNotifFilter_%s", Integer.toHexString("event3".hashCode()))));
        assertEquals("space3.page3", results.get(
                String.format("page_scopeNotifFilter_%s", Integer.toHexString("event3".hashCode()))));
        assertEquals(5, results.size());
    }
}
