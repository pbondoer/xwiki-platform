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
package org.xwiki.platform.notifications.test.ui;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.mail.test.po.SendMailAdministrationSectionPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.scheduler.test.po.SchedulerHomePage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;
import org.xwiki.user.test.po.ProfileEditPage;
import org.xwiki.user.test.po.ProfileUserProfilePage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Perform tests on the notifications module.
 * 
 * @version $Id$
 * @since 9.4RC1
 */
public class NotificationsTest extends AbstractTest
{
    private static final String FIRST_USER_NAME = "user1";
    private static final String SECOND_USER_NAME = "user2";
    private static final String SUPERADMIN_USER_NAME = "superadmin";

    private static final String FIRST_USER_PASSWORD = "notificationsUser1";
    private static final String SECOND_USER_PASSWORD = "notificationsUser2";
    private static final String SUPERADMIN_PASSWORD = "pass";

    // Number of pages that have to be created in order for the notifications badge to show «X+»
    private static final int PAGES_TOP_CREATION_COUNT = 21;

    private static GreenMail mail;

    @Before
    public void setUpUsers() throws Exception
    {
        if (getUtil().rest().exists(new DocumentReference("xwiki", "XWiki", FIRST_USER_NAME))) {
            // Already done
            return;
        }

        // Create the two users we will be using
        getUtil().createUser(FIRST_USER_NAME, FIRST_USER_PASSWORD, "", "");
        getUtil().createUser(SECOND_USER_NAME, SECOND_USER_PASSWORD, "", "");

        NotificationsUserProfilePage p;
        NotificationsTrayPage trayPage;

        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        p.disableAllParameters();
        trayPage = new NotificationsTrayPage();
        trayPage.clearAllNotifications();

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.disableAllParameters();
        ProfileEditPage profileEditPage = ProfileUserProfilePage.gotoPage(SECOND_USER_NAME).editProfile();
        profileEditPage.setUserEmail("test@xwiki.org");
        profileEditPage.clickSaveAndView(true);
    }

    @Before
    public void cleanPages() throws Exception
    {
        getUtil().login(SUPERADMIN_USER_NAME, SUPERADMIN_PASSWORD);
        getUtil().rest().deletePage(getTestClassName(), "NotificationDisplayerClassTest");
        getUtil().rest().deletePage(getTestClassName(), "ARandomPageThatShouldBeModified");
    }

    @Before
    public void setUpEmails() throws Exception
    {
        if (this.mail != null) {
            // Already done
            return;
        }

        getUtil().login(SUPERADMIN_USER_NAME, SUPERADMIN_PASSWORD);
        AdministrationPage wikiAdministrationPage = AdministrationPage.gotoPage();
        wikiAdministrationPage.clickSection("Mail", "Mail Sending");
        SendMailAdministrationSectionPage sendMailPage = new SendMailAdministrationSectionPage();
        sendMailPage.setHost("localhost");
        sendMailPage.setPort(String.valueOf(ServerSetupTest.SMTP.getPort()));

        // Make sure we don't wait between email sending in order to speed up the test (and not incur timeouts when
        // we wait to receive the mails)
        sendMailPage.setSendWaitTime("0");
        sendMailPage.clickSave();

        this.mail = new GreenMail(ServerSetupTest.SMTP);
        this.mail.start();
    }

    @AfterClass
    public static void stopMail()
    {
        if (mail != null) {
            mail.stop();
            mail = null;
        }
    }

    @Test
    public void testNotifications() throws Exception
    {
        NotificationsUserProfilePage p;
        NotificationsTrayPage tray;

        // The user 1 creates a new page, the user 2 shouldn’t receive any notification
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().createPage(getTestClassName(), "WebHome", "Content from " + FIRST_USER_NAME, "Page title");
        getUtil().gotoPage(getTestClassName(), "WebHome");

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "WebHome");

        tray = new NotificationsTrayPage();
        assertFalse(tray.areNotificationsAvailable());

        // The user 2 will now enable his notifications for new pages
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.setPageCreated(true);
        p.setPageCreated(true);

        // We create a lot of pages in order to test the notification badge
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        for (int i = 1; i < PAGES_TOP_CREATION_COUNT; i++) {
            getUtil().createPage(getTestClassName(), "Page" + i, "Simple content", "Simple title");
        }
        getUtil().createPage(getTestClassName(), "DTP", "Deletion test page", "Deletion test content");

        // Check that the badge is showing «20+»
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "WebHome");
        tray = new NotificationsTrayPage();
        assertEquals(Integer.MAX_VALUE, tray.getNotificationsCount());

        // Ensure that the notification list is displaying the correct amount of unread notifications
        // (max 5 notifications by default)
        assertEquals(5, tray.getUnreadNotificationsCount());
        assertEquals(0, tray.getReadNotificationsCount());
        tray.markAsRead(0);
        assertEquals(4, tray.getUnreadNotificationsCount());
        assertEquals(1, tray.getReadNotificationsCount());

        // Ensure that a notification has a correct type
        assertEquals("[create]", tray.getNotificationType(0));

        // Reset the notifications count of the user 2
        tray.clearAllNotifications();
        assertEquals(0, tray.getNotificationsCount());
        assertFalse(tray.areNotificationsAvailable());

        // The user 2 will get notifications only for pages deletions
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.setPageCreated(false);
        p.setPageDeleted(true);

        // Delete the "Deletion test page" and test the notification
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().deletePage(getTestClassName(), "DTP");

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "WebHome");
        tray = new NotificationsTrayPage();
        assertEquals(1, tray.getNotificationsCount());
    }

    @Test
    public void testNotificationsEmails() throws Exception
    {

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        NotificationsUserProfilePage p;
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.setPageCreatedEmail(true);

        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        DocumentReference page1 = new DocumentReference("xwiki", getTestClassName(), "Page1");
        DocumentReference page2 = new DocumentReference("xwiki", getTestClassName(), "Page2");

        getUtil().createPage(getTestClassName(), "Page1", "Content 1", "Title 1");
        getUtil().createPage(getTestClassName(), "Page2", "Content 2", "Title 2");

        // Trigger the notification email job
        getUtil().login(SUPERADMIN_USER_NAME, SUPERADMIN_PASSWORD);
        SchedulerHomePage schedulerHomePage = SchedulerHomePage.gotoPage();
        schedulerHomePage.clickJobActionTrigger("Notifications daily email");
        this.mail.waitForIncomingEmail(1);

        assertEquals(1, this.mail.getReceivedMessages().length);
        MimeMessage message = this.mail.getReceivedMessages()[0];
        assertTrue(message.getSubject().endsWith("event(s) on the wiki"));
        Multipart content = (Multipart) message.getContent();
        assertTrue(content.getContentType().startsWith("multipart/mixed;"));
        assertEquals(1, content.getCount());
        MimeBodyPart mimeBodyPart1 = (MimeBodyPart) content.getBodyPart(0);
        Multipart multipart1 = (Multipart) mimeBodyPart1.getContent();
        assertEquals(2, multipart1.getCount());
        assertEquals("text/plain; charset=UTF-8", multipart1.getBodyPart(0).getContentType());
        assertEquals("text/html; charset=UTF-8", multipart1.getBodyPart(1).getContentType());

        // Events inside an email comes in random order, so we just verify that all the expected content is there
        String email = prepareMail(multipart1.getBodyPart(0).getContent().toString());
        assertTrue(email.contains(
                prepareMail(IOUtils.toString(getClass().getResourceAsStream("/expectedMail1.txt")))
            )
        );
        assertTrue(email.contains(
                prepareMail(IOUtils.toString(getClass().getResourceAsStream("/expectedMail2.txt")))
                )
        );
        assertTrue(email.contains(
                prepareMail(IOUtils.toString(getClass().getResourceAsStream("/expectedMail3.txt")))
                )
        );

        getUtil().rest().delete(page1);
        getUtil().rest().delete(page2);
    }

    private String prepareMail(String email) {
        StringBuilder stringBuilder = new StringBuilder();
        // Some part of the email is unique (dates), so we remove them before comparing emails
        Scanner scanner = new Scanner(email);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.startsWith(String.format("  %d", Calendar.getInstance().get(Calendar.YEAR))) &&
                    !line.startsWith("  2017/06/27")) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
        }
        scanner.close();
        return stringBuilder.toString();
    }

    @Test
    public void testCompositeNotifications() throws Exception
    {
        NotificationsUserProfilePage p;
        NotificationsTrayPage tray;
        // Now we enable "create", "update" and "comment" for user 2
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.setPageCreated(false);
        p.setPageUpdated(true);
        p.setPageCommented(true);
        getUtil().gotoPage("Main", "WebHome");
        tray = new NotificationsTrayPage();
        tray.clearAllNotifications();

        // Create a page, edit it twice, and finally add a comment
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().createPage(getTestClassName(), "Linux", "Simple content", "Linux as a title");
        ViewPage page = getUtil().gotoPage(getTestClassName(), "Linux");
        page.edit();
        WikiEditPage edit = new WikiEditPage();
        edit.setContent("Linux is a part of GNU/Linux");
        edit.clickSaveAndContinue(true);
        edit.setContent("Linux is a part of GNU/Linux - it's the kernel");
        edit.clickSaveAndView(true);
        page = getUtil().gotoPage(getTestClassName(), "Linux");
        CommentsTab commentsTab = page.openCommentsDocExtraPane();
        commentsTab.postComment("Linux is a great OS", true);

        // Check that events have been grouped together (see: https://jira.xwiki.org/browse/XWIKI-14114)
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "WebHome");
        tray = new NotificationsTrayPage();
        assertEquals(2, tray.getNotificationsCount());
        assertEquals("The document Linux as a title has been commented by user1.",
                tray.getNotificationContent(0));
        assertEquals("[update]", tray.getNotificationType(1));
        assertEquals("[update] Linux as a title", tray.getNotificationContent(1));
        tray.clearAllNotifications();
    }

    @Test
    public void testNotificationDisplayerClass() throws Exception
    {
        // Create the pages and a custom displayer for "update" events
        getUtil().login(SUPERADMIN_USER_NAME, SUPERADMIN_PASSWORD);

        getUtil().gotoPage(getTestClassName(), "WebHome");
        getUtil().createPage(getTestClassName(), "ARandomPageThatShouldBeModified",
                "Page used for the tests of the NotificationDisplayerClass XObject.", "Test page");

        getUtil().createPage(getTestClassName(), "NotificationDisplayerClassTest",
                "Page used for the tests of the NotificationDisplayerClass XObject.", "Test page 2");

        Map<String, String> notificationDisplayerParameters = new HashMap<String, String>()  {{
            put("XWiki.Notifications.Code.NotificationDisplayerClass_0_eventType", "update");
            put("XWiki.Notifications.Code.NotificationDisplayerClass_0_notificationTemplate",
                    "This is a test template");
        }};

        ObjectEditPage editObjects = getUtil().editObjects(getTestClassName(), "NotificationDisplayerClassTest");
        editObjects.addObject("XWiki.Notifications.Code.NotificationDisplayerClass");
        editObjects.getObjectsOfClass("XWiki.Notifications.Code.NotificationDisplayerClass")
                .get(0).fillFieldsByName(notificationDisplayerParameters);
        editObjects.clickSaveAndContinue(true);

        // Login as first user, and enable notifications on document updates
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

        NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        p.setPageUpdated(true);

        // Login as second user and modify ARandomPageThatShouldBeModified
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);

        ViewPage viewPage = getUtil().gotoPage(getTestClassName(), "ARandomPageThatShouldBeModified");
        viewPage.edit();
        WikiEditPage editPage = new WikiEditPage();
        editPage.setContent("Something");
        editPage.clickSaveAndView(true);

        // Login as the first user, ensure that the notification is displayed with a custom template
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "WebHome");

        NotificationsTrayPage tray = new NotificationsTrayPage();
        assertEquals("This is a test template", tray.getNotificationRawContent(0));
    }
}

