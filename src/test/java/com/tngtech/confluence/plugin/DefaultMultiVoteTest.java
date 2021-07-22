package com.tngtech.confluence.plugin;

import com.atlassian.confluence.cluster.ClusterManager;
import com.atlassian.confluence.cluster.ClusteredLock;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.core.ContentPropertyManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.tngtech.confluence.plugin.data.ItemKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultMultiVoteTest {

    private static final String USER = "user";
    private static final String TABLE_ID = "tableId";
    private static final String ITEM_ID = "itemId";
    private static final String KEY = "multivote.tableId.itemId";

    @Mock
    private ClusteredLock lock;

    @Mock
    private ClusterManager clusterManager;

    @Mock
    private ContentPropertyManager contentPropertyManager;

    @Mock
    private UserAccessor userAccessor;

    @Mock
    private ContentEntityObject page;

    private DefaultMultiVoteService multivote;
    private ItemKey itemKey;

    @Before
    public void setUp() {
        when(clusterManager.getClusteredLock(anyString())).thenReturn(lock);

        multivote = new DefaultMultiVoteService(contentPropertyManager, userAccessor, clusterManager, mock(XhtmlContent.class));

        when(page.getBodyAsString()).thenReturn("<p>");
        itemKey = new ItemKey(page, TABLE_ID, ITEM_ID);
    }

    @Test
    public void test_voting_locks() {
        multivote.recordInterest(USER, true, itemKey);
        InOrder inOrder = inOrder(lock, contentPropertyManager, lock);

        inOrder.verify(lock).lock();
        inOrder.verify(contentPropertyManager).setTextProperty(page, KEY, USER);
        inOrder.verify(lock).unlock();
    }

    @Test
    public void test_voting_for_item_persists_audience() {
        multivote.recordInterest(USER, true, itemKey);

        verify(contentPropertyManager).setTextProperty(page, KEY, USER);
    }

    @Test
    public void test_voting_when_already_voted_does_not_persist() {
        when(contentPropertyManager.getTextProperty(page, KEY)).thenReturn("otherUser1, " + USER + ", otherUser2");

        multivote.recordInterest(USER, true, itemKey);

        verify(contentPropertyManager, never()).setTextProperty(eq(page), eq(KEY), anyString());
    }

    @Test
    public void test_voting_against_when_voted_before_does_persist() {
        when(contentPropertyManager.getTextProperty(page, KEY)).thenReturn("otherUser1, " + USER + ", otherUser2");

        multivote.recordInterest(USER, false, itemKey);

        verify(contentPropertyManager).setTextProperty(page, KEY, "otherUser1, otherUser2");
    }

    @Test
    public void test_voting_against_item_that_was_not_voted_for_does_not_persist() {
        multivote.recordInterest(USER, false, itemKey);

        verify(contentPropertyManager, never()).setTextProperty(page, KEY, "");
    }

    @Test
    public void test_retrieveAudience_empty() {
        Set<String> audience = multivote.retrieveAudience(itemKey);
        verify(contentPropertyManager).getTextProperty(page, KEY);
        assertThat(audience, hasSize(0));
    }

    @Test
    public void test_retrieveAudience_users() {
        when(contentPropertyManager.getTextProperty(page, KEY)).thenReturn("user1, user2");
        Set<String> audience = multivote.retrieveAudience(itemKey);
        assertThat(audience, hasSize(2));
        assertThat(audience, hasItems("user1", "user2"));
    }

}
