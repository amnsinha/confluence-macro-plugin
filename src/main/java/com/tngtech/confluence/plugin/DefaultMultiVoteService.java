package com.tngtech.confluence.plugin;

import com.atlassian.confluence.cluster.ClusterManager;
import com.atlassian.confluence.cluster.ClusteredLock;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.core.ContentPropertyManager;
import com.atlassian.confluence.renderer.PageContext;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.velocity.htmlsafe.HtmlSafe;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.user.User;
import com.opensymphony.module.propertyset.PropertyException;
import com.tngtech.confluence.plugin.data.CurrentVotes;
import com.tngtech.confluence.plugin.data.DenyReason;
import com.tngtech.confluence.plugin.data.ItemKey;
import com.tngtech.confluence.plugin.data.VoteItem;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.xml.stream.XMLStreamException;

public class DefaultMultiVoteService implements MultiVoteService {
    private static final Logger log = Logger.getLogger(DefaultMultiVoteService.class);

    private final ContentPropertyManager contentPropertyManager;
    private final UserAccessor userAccessor;
    private final ClusterManager clusterManager;
    private final XhtmlContent xmlXhtmlContent;

    public DefaultMultiVoteService(
            ContentPropertyManager contentPropertyManager,
            UserAccessor userAccessor,
            ClusterManager clusterManager,
            XhtmlContent xmlXhtmlContent) {
        this.contentPropertyManager = contentPropertyManager;
        this.userAccessor = userAccessor;
        this.clusterManager = clusterManager;
        this.xmlXhtmlContent = xmlXhtmlContent;
    }

    @Override
    public VoteItem recordInterest(String remoteUser, boolean requestUse, ItemKey key) {
        ClusteredLock lock = getLock(key);
        VoteItem vote;
        try {
            lock.lock();
            vote = doRecordInterestIfPossible(remoteUser, requestUse, key);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
        return vote;
    }

    @Override
    public void reset(ContentEntityObject page, String tableId, List<String> itemIds) {
        for (String itemId : itemIds) {
            ItemKey key = new ItemKey(page, tableId, itemId);

            ClusteredLock lock = getLock(key);

            try {
                lock.lock();
                String property = buildPropertyString(key);
                contentPropertyManager.removeProperty(key.getPage(), property);
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
    }

    private ClusteredLock getLock(ItemKey key) {
        return clusterManager.getClusteredLock("multivote.lock." + key.getTableId() + "." + key.getItemId());
    }

    private VoteItem doRecordInterestIfPossible(String user, Boolean requestUse, ItemKey key) {
        boolean changed;
        ContentEntityObject page = key.getPage();
        Document doc = Jsoup.parse(page.getBodyAsString());

        Set<String> users = retrieveAudience(key);
        VoteItem voteItem = new VoteItem(key.getItemId(), users);

        CurrentVotes voteCounts = getUserVoteCount(user, key, page, doc);
        boolean canUserStillVote = canUserStillVote(key, voteCounts, doc, voteItem);
        boolean canItemStillBeVotedFor = canItemStillBeVotedFor(key, voteCounts, doc, voteItem);

        log.debug("Max allowed votes for user: " + voteItem.getMaxUserVotes());
        log.debug("Max allowed votes for item: " + voteItem.getMaxItemVotes());

        if (requestUse && canUserStillVote && canItemStillBeVotedFor) {
            changed = users.add(user);
        } else {
            changed = users.remove(user);
            if (!canUserStillVote) {
                voteItem.setDenyReason(DenyReason.NUMBER_OF_USER_VOTES);
            }
            if (!canItemStillBeVotedFor) {
                voteItem.setDenyReason(DenyReason.NUMBER_OF_ROW_VOTES);
            }
        }
        if (changed) {
            persistAudience(key, users);
        }
        voteItem.setChanged(changed);
        return voteItem;
    }

    private boolean canUserStillVote(ItemKey key, CurrentVotes voteCount, Document doc,
                                     VoteItem voteItem) {
        int maxVotes = getMacroProperty(key, "maxUserVotes", doc);
        voteItem.setMaxUserVotes(maxVotes);
        return maxVotes <= 0 || voteCount.getUserVotes() < maxVotes;
    }

    private boolean canItemStillBeVotedFor(ItemKey key, CurrentVotes voteCount, Document doc, VoteItem voteItem) {
        int maxVotes = getMacroProperty(key, "maxItemVotes", doc);
        voteItem.setMaxItemVotes(maxVotes);
        return maxVotes <= 0 || voteCount.getItemVotes() < maxVotes;
    }

    private CurrentVotes getUserVoteCount(String user, ItemKey key, ContentEntityObject page, Document doc) {
        int userVoteCount = 0, itemVoteCount = 0;
        String tableId = key.getTableId();
        Set<String> itemIds = parseItemIdsFromContentBody(key, doc);
        for (String itemId : itemIds) {
            ItemKey choice = new ItemKey(page, tableId, itemId);
            Set<String> audience = retrieveAudience(choice);
            if (audience.contains(user)) {
                userVoteCount++;
            }
            if (itemId.contains(key.getItemId().trim())) {
                itemVoteCount = audience.size();
            }
        }
        log.debug("Found user to have voted " + userVoteCount + " times");
        log.debug("Found current item to have " + itemVoteCount + " votes");
        return new CurrentVotes(userVoteCount, itemVoteCount);
    }

    private Set<String> parseItemIdsFromContentBody(ItemKey key, Document doc) {
        Set<String> itemIds = new TreeSet<>();
        try {
            Elements tables = doc.select(
                    "ac|parameter[ac:name=\"id\"]:matches(^" + key.getTableId() + "$) ~ ac|rich-text-body > table");
            if (tables.size() == 0) {
                log.warn("Problem finding my Table. Something is seriously wrong! tableid=" + key.getTableId());
                return itemIds;
            }
            log.debug("Found " + tables.size() + " interesting tables");

            Element table = tables.get(0);
            Elements rows = table.select("tr");
            for (int i = 1; i < rows.size(); i++) { // skip header row
                Element firstCol = rows.get(i).select("td").get(0);
                itemIds.add(firstCol.text());
            }
            return itemIds;
        } catch (IllegalArgumentException e) {
            return itemIds;
        }
    }

    private int getMacroProperty(ItemKey key, String propertyKey, Document doc) {
        int maxVotes = 0;
        try {
            Elements maxVotesCssQueryResult = doc.select("ac|parameter[ac:name=\"id\"]:matches(^" + key.getTableId()
                    + "$) ~ ac|parameter[ac:name=\"" + propertyKey + "\"]");

            if (!maxVotesCssQueryResult.isEmpty()) {
                maxVotes = new Integer(maxVotesCssQueryResult.get(0).text());
            } else {
                log.debug("Parameter " + propertyKey + " is not set. Working with " + propertyKey + " = " + maxVotes + " instead.");
            }
        } catch (Exception e) {
            log.error("Problem reading macro config", e);
        }

        return maxVotes;
    }

    @Override
    public Set<String> retrieveAudience(ItemKey key) {
        String usersAsString;
        try {
            usersAsString = contentPropertyManager.getTextProperty(key.getPage(), buildPropertyString(key));
        } catch (PropertyException e) {
            // Additionally check .getStringProperty() as workaround for CONFSERVER-37166
            usersAsString = contentPropertyManager.getStringProperty(key.getPage(), buildPropertyString(key));
        }
        if (usersAsString == null) {
            usersAsString = "";
        }
        Set<String> users = new TreeSet<>();
        StringTokenizer userTokenizer = new StringTokenizer(usersAsString, ",");
        while (userTokenizer.hasMoreTokens()) {
            users.add(userTokenizer.nextToken().trim());
        }
        return users;
    }

    private void persistAudience(ItemKey key, Set<String> users) {
        String property = buildPropertyString(key);
        contentPropertyManager.setTextProperty(key.getPage(), property, StringUtils.join(users, ", "));
    }

    private String buildPropertyString(ItemKey key) {
        return "multivote." + key.getTableId() + "." + key.getItemId();
    }

    @Override
    public String getUserFullNamesAsString(Set<String> audience) {
        List<String> fullNames = new ArrayList<>();

        for (String userName : audience) {
            fullNames.add(getFullName(userName));
        }
        return StringUtils.join(fullNames, ", ");
    }

    @HtmlSafe
    @Override
    public String getUserFullNamesAsHtml(Set<String> audience, PageContext context) throws MacroException {
        List<String> userLinks = new ArrayList<>(audience.size());

        for (String userName : audience) {
            userLinks.add(getUserLink(userName));
        }

        String joinedLinks = String.join(", ", userLinks);
        try {
            return xmlXhtmlContent.convertStorageToView(joinedLinks, new DefaultConversionContext(context));
        } catch (XMLStreamException | XhtmlException e) {
            throw new MacroException(e);
        }
    }

    private String getUserLink(String userName) {
        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put("userName", userName);
        return VelocityUtils.getRenderedTemplate("templates/extra/userlink.vm", contextMap);
    }

    private String getFullName(String userName) {
        String fullName = userName;
        User user = userAccessor.getUser(userName);
        if (user != null) {
            fullName = user.getFullName();
        }
        if (fullName == null) {
            fullName = userName;
        }
        return fullName;
    }

}
