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
 *
 */

package com.xpn.xwiki.user.impl.xwiki;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameFactory;
import org.xwiki.bridge.DocumentNameSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.GroupsClass;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightNotFoundException;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

/**
 * Default implementation of {@link XWikiRightService}.
 * 
 * @version $Id$
 */
public class XWikiRightServiceImpl implements XWikiRightService
{
    private static final Log LOG = LogFactory.getLog(XWikiRightServiceImpl.class);

    private static final List<String> ALLLEVELS =
            Arrays.asList("admin", "view", "edit", "comment", "delete", "undelete", "register", "programming");

    private static Map<String, String> actionMap;

    /**
     * Used to convert a string into a proper Document Name.
     */
    private DocumentNameFactory documentNameFactory = Utils.getComponent(DocumentNameFactory.class);

    /**
     * Used to convert a proper Document Name to string.
     */
    private DocumentNameSerializer documentNameSerializer = Utils.getComponent(DocumentNameSerializer.class);

    protected void logAllow(String username, String page, String action, String info)
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Access has been granted for (" + username + "," + page + "," + action + "): " + info);
        }
    }

    protected void logDeny(String username, String page, String action, String info)
    {
        if (LOG.isInfoEnabled()) {
            LOG.info("Access has been denied for (" + username + "," + page + "," + action + "): " + info);
        }
    }

    protected void logDeny(String name, String resourceKey, String accessLevel, String info, Exception e)
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Access has been denied for (" + name + "," + resourceKey + "," + accessLevel + ") at " + info, e);
        }
    }

    public List<String> listAllLevels(XWikiContext context) throws XWikiException
    {
        return new ArrayList<String>(ALLLEVELS);
    }

    public String getRight(String action)
    {
        if (actionMap == null) {
            actionMap = new HashMap<String, String>();
            actionMap.put("login", "login");
            actionMap.put("logout", "login");
            actionMap.put("loginerror", "login");
            actionMap.put("loginsubmit", "login");
            actionMap.put("view", "view");
            actionMap.put("viewrev", "view");
            actionMap.put("downloadrev", "download");
            actionMap.put("plain", "view");
            actionMap.put("raw", "view");
            actionMap.put("attach", "view");
            actionMap.put("charting", "view");
            actionMap.put("skin", "view");
            actionMap.put("download", "view");
            actionMap.put("dot", "view");
            actionMap.put("svg", "view");
            actionMap.put("pdf", "view");
            actionMap.put("delete", "delete");
            actionMap.put("deleteversions", "admin");
            actionMap.put("undelete", "undelete");
            actionMap.put("reset", "delete");
            actionMap.put("commentadd", "comment");
            actionMap.put("register", "register");
            actionMap.put("redirect", "view");
            actionMap.put("admin", "admin");
            actionMap.put("export", "view");
            actionMap.put("import", "admin");
            actionMap.put("jsx", "view");
            actionMap.put("ssx", "view");
            actionMap.put("tex", "view");
            actionMap.put("unknown", "view");
        }

        String right = actionMap.get(action);
        if (right == null) {
            return "edit";
        } else {
            return right;
        }
    }

    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("checkAccess for " + action + ", " + doc.getFullName());
        }

        String username = null;
        XWikiUser user = null;
        boolean needsAuth = false;
        String right = getRight(action);

        if (right.equals("login")) {
            user = context.getWiki().checkAuth(context);
            if (user == null) {
                username = XWikiRightService.GUEST_USER_FULLNAME;
            } else {
                username = user.getUser();
            }

            // Save the user
            context.setUser(username);
            logAllow(username, doc.getFullName(), action, "login/logout pages");

            return true;
        }

        if (right.equals("delete")) {
            user = context.getWiki().checkAuth(context);
            String creator = doc.getCreator();
            if ((user != null) && (user.getUser() != null) && (creator != null)) {
                if (user.getUser().equals(creator)) {
                    context.setUser(user.getUser());

                    return true;
                }
            }
        }

        // We do not need to authenticate twice
        // This seems to cause a problem in virtual wikis
        user = context.getXWikiUser();
        if (user == null) {
            needsAuth = needsAuth(right, context);
            try {
                if (context.getMode() != XWikiContext.MODE_XMLRPC) {
                    user = context.getWiki().checkAuth(context);
                } else {
                    user = new XWikiUser(context.getUser());
                }

                if ((user == null) && (needsAuth)) {
                    logDeny("unauthentified", doc.getFullName(), action, "Authentication needed");
                    if (context.getRequest() != null) {
                        if (!context.getWiki().Param("xwiki.hidelogin", "false").equalsIgnoreCase("true")) {
                            context.getWiki().getAuthService().showLogin(context);
                        }
                    }

                    return false;
                }
            } catch (XWikiException e) {
                if (needsAuth) {
                    throw e;
                }
            }

            if (user == null) {
                username = XWikiRightService.GUEST_USER_FULLNAME;
            } else {
                username = user.getUser();
            }

            // Save the user
            context.setUser(username);
        } else {
            username = user.getUser();
        }

        // Check Rights
        try {
            // Verify access rights and return if ok
            String docname;
            if (context.getDatabase() != null) {
                docname = context.getDatabase() + ":" + doc.getFullName();
                if (username.indexOf(":") == -1) {
                    username = context.getDatabase() + ":" + username;
                }
            } else {
                docname = doc.getFullName();
            }

            if (context.getWiki().getRightService().hasAccessLevel(right, username, docname, context)) {
                logAllow(username, docname, action, "access manager granted right");

                return true;
            }
        } catch (Exception e) {
            // This should not happen..
            logDeny(username, doc.getFullName(), action, "access manager exception " + e.getMessage());
            e.printStackTrace();

            return false;
        }

        if (user == null) {
            // Denied Guest need to be authenticated
            logDeny("unauthentified", doc.getFullName(), action, "Guest has been denied");
            if (context.getRequest() != null
                && !context.getWiki().Param("xwiki.hidelogin", "false").equalsIgnoreCase("true")) {
                context.getWiki().getAuthService().showLogin(context);
            }

            return false;
        } else {
            logDeny(username, doc.getFullName(), action, "access manager denied right");

            return false;
        }
    }

    private boolean needsAuth(String right, XWikiContext context)
    {
        boolean needsAuth = false;

        try {
            needsAuth =
                    context.getWiki().getXWikiPreference("authenticate_" + right, "", context).toLowerCase().equals(
                        "yes");
        } catch (Exception e) {
        }

        try {
            needsAuth |= (context.getWiki().getXWikiPreferenceAsInt("authenticate_" + right, 0, context) == 1);
        } catch (Exception e) {
        }

        try {
            needsAuth |=
                    context.getWiki().getWebPreference("authenticate_" + right, "", context).toLowerCase().equals("yes");
        } catch (Exception e) {
        }

        try {
            needsAuth |= (context.getWiki().getWebPreferenceAsInt("authenticate_" + right, 0, context) == 1);
        } catch (Exception e) {
        }

        return needsAuth;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiRightService#hasAccessLevel(java.lang.String, java.lang.String,
     *      java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public boolean hasAccessLevel(String right, String username, String docname, XWikiContext context)
        throws XWikiException
    {
        try {
            return hasAccessLevel(right, username, docname, true, context);
        } catch (XWikiException e) {
            return false;
        }
    }

    public boolean checkRight(String name, XWikiDocument doc, String accessLevel, boolean user, boolean allow,
        boolean global, XWikiContext context) throws XWikiRightNotFoundException, XWikiException
    {
        String className = global ? "XWiki.XWikiGlobalRights" : "XWiki.XWikiRights";
        String fieldName = user ? "users" : "groups";
        boolean found = false;

        // Here entity is either a user or a group
        DocumentName entityDocumentName = this.documentNameFactory.createDocumentName(name);
        String prefixedFullName = this.documentNameSerializer.serialize(entityDocumentName);
        String shortname = name;
        int i0 = name.indexOf(":");
        if (i0 != -1) {
            shortname = name.substring(i0 + 1);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking right: " + name + "," + doc.getFullName() + "," + accessLevel + "," + user + ","
                + allow + "," + global);
        }

        Vector<BaseObject> vobj = doc.getObjects(className);
        if (vobj != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Checking objects " + vobj.size());
            }

            for (int i = 0; i < vobj.size(); i++) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Checking object " + i);
                }

                BaseObject bobj = vobj.get(i);

                if (bobj == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Bypass object " + i);
                    }

                    continue;
                }

                String users = bobj.getStringValue(fieldName);
                String levels = bobj.getStringValue("levels");
                boolean allowdeny = (bobj.getIntValue("allow") == 1);

                if (allowdeny == allow) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Checking match: " + accessLevel + " in " + levels);
                    }

                    String[] levelsarray = StringUtils.split(levels, " ,|");
                    if (ArrayUtils.contains(levelsarray, accessLevel)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Found a right for " + allow);
                        }
                        found = true;

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Checking match: " + name + " in " + users);
                        }

                        String[] userarray = GroupsClass.getListFromString(users).toArray(new String[0]);

                        for (int ii = 0; ii < userarray.length; ii++) {
                            String value = userarray[ii];
                            if (value.indexOf(".") == -1) {
                                userarray[ii] = "XWiki." + value;
                            }
                        }

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Checking match: " + name + " in " + StringUtils.join(userarray, ","));
                        }

                        // In the case where the document database and the user database is the same
                        // then we allow the usage of the short name, otherwise the fully qualified
                        // name is requested
                        if (context.getDatabase().equals(entityDocumentName.getWiki())) {
                            if (ArrayUtils.contains(userarray, shortname)) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Found matching right in " + users + " for " + shortname);
                                }

                                return true;
                            }

                            // We should also allow to skip "XWiki." from the usernames and group
                            // lists
                            String veryshortname = shortname.substring(shortname.indexOf(".") + 1);
                            if (ArrayUtils.contains(userarray, veryshortname)) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Found matching right in " + users + " for " + shortname);
                                }

                                return true;
                            }
                        }

                        if ((context.getDatabase() != null) && (ArrayUtils.contains(userarray, name))) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Found matching right in " + users + " for " + name);
                            }

                            return true;
                        }

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Failed match: " + name + " in " + users);
                        }
                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Bypass object " + i + " because wrong allow/deny");
                    }
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching for matching rights at group level");
        }

        // Didn't found right at this level.. Let's go to group level
        Map<String, Collection<String>> grouplistcache = (Map<String, Collection<String>>) context.get("grouplist");
        if (grouplistcache == null) {
            grouplistcache = new HashMap<String, Collection<String>>();
            context.put("grouplist", grouplistcache);
        }

        Collection<String> grouplist = new HashSet<String>();
        XWikiGroupService groupService = context.getWiki().getGroupService(context);

        // FIXME: it looks like this code was supposed to get entity groups in current wikis but listGroupsForUser
        // always return groups from entity wiki, maybe listGroupsForUser changed at some point
        {
            // the key is for the entity <code>prefixedFullName</code> in current wiki
            String key = context.getDatabase() + ":" + prefixedFullName;

            Collection<String> tmpGroupList = grouplistcache.get(key);
            if (tmpGroupList == null) {
                try {
                    Collection<String> glist = groupService.listGroupsForUser(name, context);

                    tmpGroupList = new ArrayList<String>(glist.size());
                    for (String groupName : glist) {
                        tmpGroupList.add(entityDocumentName.getWiki() + ":" + groupName);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to get groups for user or group [" + name + "]", e);

                    tmpGroupList = Collections.emptyList();
                }

                grouplistcache.put(key, tmpGroupList);
            }

            grouplist.addAll(tmpGroupList);
        }

        // Get entity groups in entity wiki
        if (context.getWiki().isVirtualMode() && !context.getDatabase().equalsIgnoreCase(entityDocumentName.getWiki())) {
            String database = context.getDatabase();
            try {
                context.setDatabase(entityDocumentName.getWiki());

                // the key is for the entity <code>prefixedFullName</code> in entity wiki
                String key = context.getDatabase() + ":" + prefixedFullName;
                Collection<String> tmpGroupList = grouplistcache.get(key);

                if (tmpGroupList == null) {
                    Collection<String> glist = groupService.listGroupsForUser(shortname, context);

                    tmpGroupList = new ArrayList<String>(glist.size());
                    for (String groupName : glist) {
                        tmpGroupList.add(context.getDatabase() + ":" + groupName);
                    }
                    grouplistcache.put(key, tmpGroupList);
                }

                grouplist.addAll(tmpGroupList);
            } catch (Exception e) {
                LOG.error("Failed to get groups for user or group [" + name + "]", e);
            } finally {
                context.setDatabase(database);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching for matching rights for [" + grouplist.size() + "] groups: " + grouplist);
        }

        for (String group : grouplist) {
            try {
                // We need to construct the full group name to make sure the groups are
                // handled separately
                boolean result = checkRight(group, doc, accessLevel, false, allow, global, context);
                if (result) {
                    return true;
                }
            } catch (XWikiRightNotFoundException e) {
            } catch (Exception e) {
                LOG.error("Failed to chech right [" + accessLevel + "] for group [" + group + "] on document ["
                    + doc.getPrefixedFullName() + "]", e);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Finished searching for rights for " + name + ": " + found);
        }

        if (found) {
            return false;
        } else {
            throw new XWikiRightNotFoundException();
        }
    }

    public boolean hasAccessLevel(String accessLevel, String name, String resourceKey, boolean user,
        XWikiContext context) throws XWikiException
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("hasAccessLevel for " + accessLevel + ", " + name + ", " + resourceKey);
        }

        boolean deny = false;
        boolean allow = false;
        boolean allow_found = false;
        boolean deny_found = false;
        boolean isReadOnly = context.getWiki().isReadOnly();
        String database = context.getDatabase();
        XWikiDocument currentdoc = null;

        if (isReadOnly) {
            if ("edit".equals(accessLevel) || "delete".equals(accessLevel) || "undelete".equals(accessLevel)
                || "comment".equals(accessLevel) || "register".equals(accessLevel)) {
                logDeny(name, resourceKey, accessLevel, "server in read-only mode");

                return false;
            }
        }

        if (name.equals(XWikiRightService.GUEST_USER_FULLNAME)
            || name.endsWith(":" + XWikiRightService.GUEST_USER_FULLNAME)) {
            if (needsAuth(accessLevel, context)) {
                return false;
            }
        }

        // Fast return for delete right: allow the creator to delete the document
        if (accessLevel.equals("delete") && user) {
            currentdoc = context.getWiki().getDocument(resourceKey, context);
            String creator = currentdoc.getCreator();
            if (creator != null) {
                if (name.equals(creator)) {
                    logAllow(name, resourceKey, accessLevel, "delete right from document ownership");

                    return true;
                }
            }
        }

        allow = isSuperAdminOrProgramming(name, resourceKey, accessLevel, user, context);
        if ((allow == true) || (accessLevel.equals("programming"))) {
            return allow;
        }

        try {
            // Verify Wiki Owner
            String wikiOwner = context.getWiki().getWikiOwner(database, context);
            if (wikiOwner != null) {
                if (wikiOwner.equals(name)) {
                    logAllow(name, resourceKey, accessLevel, "admin level from wiki ownership");

                    return true;
                }
            }

            XWikiDocument xwikidoc = context.getWiki().getDocument("XWiki.XWikiPreferences", context);

            // Verify XWiki register right
            if (accessLevel.equals("register")) {
                try {
                    allow = checkRight(name, xwikidoc, "register", user, true, true, context);
                    if (allow) {
                        logAllow(name, resourceKey, accessLevel, "register level");

                        return true;
                    } else {
                        logDeny(name, resourceKey, accessLevel, "register level");

                        return false;
                    }
                } catch (XWikiRightNotFoundException e) {
                    try {
                        deny = checkRight(name, xwikidoc, "register", user, false, true, context);
                        if (deny) {
                            return false;
                        }
                    } catch (XWikiRightNotFoundException e1) {
                    }
                }

                logAllow(name, resourceKey, accessLevel, "register level (no right found)");

                return true;
            }

            int maxRecursiveSpaceChecks = context.getWiki().getMaxRecursiveSpaceChecks(context);
            boolean isSuperUser =
                    isSuperUser(accessLevel, name, resourceKey, user, xwikidoc, maxRecursiveSpaceChecks, context);
            if (isSuperUser) {
                logAllow(name, resourceKey, accessLevel, "admin level");

                return true;
            }

            // check has deny rights
            if (hasDenyRights()) {
                // First check if this document is denied to the specific user
                resourceKey = Util.getName(resourceKey, context);
                try {
                    currentdoc =
                            (currentdoc == null) ? context.getWiki().getDocument(resourceKey, context) : currentdoc;
                    deny = checkRight(name, currentdoc, accessLevel, user, false, false, context);
                    deny_found = true;
                    if (deny) {
                        logDeny(name, resourceKey, accessLevel, "document level");
                        return false;
                    }
                } catch (XWikiRightNotFoundException e) {
                }
            }

            try {
                currentdoc = (currentdoc == null) ? context.getWiki().getDocument(resourceKey, context) : currentdoc;
                allow = checkRight(name, currentdoc, accessLevel, user, true, false, context);
                allow_found = true;
                if (allow) {
                    logAllow(name, resourceKey, accessLevel, "document level");

                    return true;
                }
            } catch (XWikiRightNotFoundException e) {
            }

            // Check if this document is denied/allowed
            // through the web WebPreferences Global Rights

            String space = currentdoc.getSpace();
            ArrayList<String> spacesChecked = new ArrayList<String>();
            int recursiveSpaceChecks = 0;
            while ((space != null) && (recursiveSpaceChecks <= maxRecursiveSpaceChecks)) {
                // Add one to the recursive space checks
                recursiveSpaceChecks++;
                // add to list of spaces already checked
                spacesChecked.add(space);
                XWikiDocument webdoc = context.getWiki().getDocument(space, "WebPreferences", context);
                if (!webdoc.isNew()) {
                    if (hasDenyRights()) {
                        try {
                            deny = checkRight(name, webdoc, accessLevel, user, false, true, context);
                            deny_found = true;
                            if (deny) {
                                logDeny(name, resourceKey, accessLevel, "web level");

                                return false;
                            }
                        } catch (XWikiRightNotFoundException e) {
                        }
                    }

                    // If a right was found at the previous level
                    // then we cannot check the web rights anymore
                    if (!allow_found) {
                        try {
                            allow = checkRight(name, webdoc, accessLevel, user, true, true, context);
                            allow_found = true;
                            if (allow) {
                                logAllow(name, resourceKey, accessLevel, "web level");

                                return true;
                            }
                        } catch (XWikiRightNotFoundException e) {
                        }
                    }

                    // find the parent web to check rights on it
                    space = webdoc.getStringValue("XWiki.XWikiPreferences", "parent");
                    if ((space == null) || (space.trim().equals("")) || spacesChecked.contains(space)) {
                        // no parent space or space already checked (recursive loop). let's finish
                        // the loop
                        space = null;
                    }
                } else {
                    // let's finish the loop
                    space = null;
                }
            }

            // Check if this document is denied/allowed
            // through the XWiki.XWikiPreferences Global Rights
            if (hasDenyRights()) {
                try {
                    deny = checkRight(name, xwikidoc, accessLevel, user, false, true, context);
                    deny_found = true;
                    if (deny) {
                        logDeny(name, resourceKey, accessLevel, "xwiki level");

                        return false;
                    }
                } catch (XWikiRightNotFoundException e) {
                }
            }

            // If a right was found at the document or web level
            // then we cannot check the web rights anymore
            if (!allow_found) {
                try {
                    allow = checkRight(name, xwikidoc, accessLevel, user, true, true, context);
                    allow_found = true;
                    if (allow) {
                        logAllow(name, resourceKey, accessLevel, "xwiki level");

                        return true;
                    }
                } catch (XWikiRightNotFoundException e) {
                }
            }

            // If neither doc, web or topic had any allowed ACL
            // and that all users that were not denied
            // should be allowed.
            if (!allow_found) {
                // Should these rights be denied only if no deny rights were found?
                if (accessLevel.equals("register") || accessLevel.equals("delete")) {
                    logDeny(name, resourceKey, accessLevel, "global level (" + accessLevel + " right must be explicit)");

                    return false;
                } else {
                    logAllow(name, resourceKey, accessLevel, "global level (no restricting right)");

                    return true;
                }
            } else {
                logDeny(name, resourceKey, accessLevel, "global level (restricting right was found)");

                return false;
            }

        } catch (XWikiException e) {
            logDeny(name, resourceKey, accessLevel, "global level (exception)", e);
            e.printStackTrace();

            return false;
        } finally {
            context.setDatabase(database);
        }
    }

    private boolean hasDenyRights()
    {
        return true;
    }

    /**
     * Check is the given user is a superadmin.
     * 
     * @param username Any flavor of username. Examples: "xwiki:XWiki.superadmin", "XWiki.superAdmin", "superadmin", etc
     * @return True if the user is a superadmin, false otherwise
     */
    // TODO: this method is a candidate for the the XWikiRightService API.
    private boolean isSuperAdmin(String username)
    {
        DocumentName documentName = Utils.getComponent(DocumentNameFactory.class).createDocumentName(username);
        return documentName.getPage().equalsIgnoreCase(SUPERADMIN_USER);
    }

    private boolean isSuperAdminOrProgramming(String name, String resourceKey, String accessLevel, boolean user,
        XWikiContext context) throws XWikiException
    {
        String database = context.getDatabase();
        boolean allow;

        if (isSuperAdmin(name)) {
            logAllow(name, resourceKey, accessLevel, "super admin level");
            return true;
        }

        try {
            // The master user and programming rights are checked in the main wiki
            context.setDatabase(context.getMainXWiki());
            XWikiDocument xwikimasterdoc = context.getWiki().getDocument("XWiki.XWikiPreferences", context);
            // Verify XWiki Master super user
            try {
                allow = checkRight(name, xwikimasterdoc, "admin", true, true, true, context);
                if (allow) {
                    logAllow(name, resourceKey, accessLevel, "master admin level");
                    return true;
                }
            } catch (XWikiRightNotFoundException e) {
            }

            // Verify XWiki programming right
            if (accessLevel.equals("programming")) {
                // Programming right can only been given if user is from main wiki
                if (!name.startsWith(context.getWiki().getDatabase() + ":")) {
                    return false;
                }

                try {
                    allow = checkRight(name, xwikimasterdoc, "programming", user, true, true, context);
                    if (allow) {
                        logAllow(name, resourceKey, accessLevel, "programming level");

                        return true;
                    } else {
                        logDeny(name, resourceKey, accessLevel, "programming level");

                        return false;
                    }
                } catch (XWikiRightNotFoundException e) {
                }

                logDeny(name, resourceKey, accessLevel, "programming level (no right found)");

                return false;
            }
        } finally {
            // The next rights are checked in the virtual wiki
            context.setDatabase(database);
        }

        return false;
    }

    private boolean isSuperUser(String accessLevel, String name, String resourceKey, boolean user,
        XWikiDocument xwikidoc, int maxRecursiveSpaceChecks, XWikiContext context) throws XWikiException
    {
        boolean allow;

        // Verify XWiki super user
        try {
            allow = checkRight(name, xwikidoc, "admin", user, true, true, context);
            if (allow) {
                logAllow(name, resourceKey, accessLevel, "admin level");

                return true;
            }
        } catch (XWikiRightNotFoundException e) {
        }

        XWikiDocument documentName = new XWikiDocument();
        documentName.setFullName(resourceKey);

        // Verify Web super user
        String space = documentName.getSpace();
        ArrayList<String> spacesChecked = new ArrayList<String>();
        int recursiveSpaceChecks = 0;
        while ((space != null) && (recursiveSpaceChecks <= maxRecursiveSpaceChecks)) {
            // Add one to the recursive space checks
            recursiveSpaceChecks++;
            // add to list of spaces already checked
            spacesChecked.add(space);
            XWikiDocument webdoc = context.getWiki().getDocument(space, "WebPreferences", context);
            if (!webdoc.isNew()) {
                try {
                    allow = checkRight(name, webdoc, "admin", user, true, true, context);
                    if (allow) {
                        logAllow(name, resourceKey, accessLevel, "web admin level");
                        return true;
                    }
                } catch (XWikiRightNotFoundException e) {
                }

                // find the parent web to check rights on it
                space = webdoc.getStringValue("XWiki.XWikiPreferences", "parent");
                if ((space == null) || (space.trim().equals("")) || spacesChecked.contains(space)) {
                    // no parent space or space already checked (recursive loop). let's finish the
                    // loop
                    space = null;
                }
            } else {
                space = null;
            }
        }

        return false;
    }

    public boolean hasProgrammingRights(XWikiContext context)
    {
        XWikiDocument sdoc = (XWikiDocument) context.get("sdoc");
        if (sdoc == null) {
            sdoc = context.getDoc();
        }

        return hasProgrammingRights(sdoc, context);
    }

    public boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context)
    {
        try {
            if (doc == null) {
                // If no context document is set, then check the rights of the current user
                return isSuperAdminOrProgramming(context.getUser(), null, "programming", true, context);
            }

            String username = doc.getContentAuthor();

            if (username == null) {
                return false;
            }

            String docname;
            if (doc.getDatabase() != null) {
                docname = doc.getDatabase() + ":" + doc.getFullName();
                if (username.indexOf(":") == -1) {
                    username = doc.getDatabase() + ":" + username;
                }
            } else {
                docname = doc.getFullName();
            }

            // programming rights can only been given for user of the main wiki
            if (context.getWiki().isVirtualMode()) {
                String maindb = context.getWiki().getDatabase();
                if ((maindb == null) || (!username.startsWith(maindb))) {
                    return false;
                }
            }

            return hasAccessLevel("programming", username, docname, context);
        } catch (Exception e) {
            LOG.error("Faile to check programming right for document [" + doc.getPrefixedFullName() + "]", e);

            return false;
        }
    }

    public boolean hasAdminRights(XWikiContext context)
    {
        boolean hasAdmin = false;
        try {
            hasAdmin = hasAccessLevel("admin", context.getUser(), "XWiki.XWikiPreferences", context);
        } catch (Exception e) {
            LOG.error("Failed to check admin right for user [" + context.getUser() + "]", e);
        }

        if (!hasAdmin) {
            try {
                hasAdmin =
                        hasAccessLevel("admin", context.getUser(), context.getDoc().getSpace() + ".WebPreferences",
                            context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return hasAdmin;
    }

}
