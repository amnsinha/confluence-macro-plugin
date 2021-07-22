package com.tngtech.confluence.plugin;

import java.util.List;
import java.util.Set;

import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.renderer.PageContext;
import com.atlassian.renderer.v2.macro.MacroException;
import com.tngtech.confluence.plugin.data.ItemKey;
import com.tngtech.confluence.plugin.data.VoteItem;

public interface MultiVoteService {
    /**
     * Vote for a specific Item
     * @param user that votes
     * @param interest
     * @param key identifying the vote Item
     * @return the Item that was voted for
     */
    VoteItem recordInterest(String user, boolean interest, ItemKey key);

    /**
     * retrieve the Audience of an item
     * @param key identifying the vote Item
     */
    Set<String> retrieveAudience(ItemKey key);

    /**
     * Get the full names of a set of users
     * @param audience Set of usernames
     */
    String getUserFullNamesAsHtml(Set<String> audience, PageContext context) throws MacroException;
    String getUserFullNamesAsString(Set<String> audience);


    /**
     * Delete all votes for the multivote given by
     * @param page the multivote is contained in
     * @param tableId of the multivote
     * @param itemIds of the multivote
     */
    void reset(ContentEntityObject page, String tableId, List<String> itemIds);
}
