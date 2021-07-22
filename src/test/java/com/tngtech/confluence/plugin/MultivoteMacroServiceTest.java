package com.tngtech.confluence.plugin;

import static jodd.jerry.Jerry.jerry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;

import com.atlassian.confluence.core.ContentEntityManager;
import jodd.jerry.Jerry;
import org.junit.Before;
import org.junit.Test;

import com.atlassian.confluence.core.ContentEntityObject;
import com.tngtech.confluence.plugin.data.Header;
import com.tngtech.confluence.plugin.data.ItemKey;
import com.tngtech.confluence.plugin.data.VoteItem;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.swing.text.AbstractDocument;

@RunWith(MockitoJUnitRunner.class)
public class MultivoteMacroServiceTest {

    private static final String USER_IN_AUDIENCE = "userInAudience";

    private final Jerry body =
            jerry(
            "<div class='table-wrap'>"
            +"<table class='confluenceTable'><tbody>"
            +"<tr>"
            +"<th class='confluenceTh'> ID </th>"
            +"<th class='confluenceTh'> header1 </th>"
            +"<th class='confluenceTh'> header2 </th>"
            +"</tr>"
            +"<tr>"
            +"<td class='confluenceTd'> 4711 </td>"
            +"<td class='confluenceTd'> bla </td>"
            +"<td class='confluenceTd'> blubb </td>"
            +"</tr>"
            +"<tr>"
            +"<td class='confluenceTd'> 0815 </td>"
            +"<td class='confluenceTd'> bloerk </td>"
            +"<td class='confluenceTd'> plopp </td>"
            +"</tr>"
            +"</tbody></table>"
            +"</div>");

    @Mock
    private MultiVoteService multiVoteService;

    @Mock
    private ContentEntityManager contentEntityManager;

    private MultiVoteMacroService macroService;
    private ContentEntityObject page;
    private HttpServletRequest request;

    @Before
    public void setUp() {
        macroService = new MultiVoteMacroService(multiVoteService, contentEntityManager);

        request = mock(HttpServletRequest.class);
        when(request.getRemoteUser()).thenReturn("admin");

        Set<String> audience = new TreeSet<String>();
        audience.add(USER_IN_AUDIENCE);
        when(multiVoteService.retrieveAudience(anyObject())).thenReturn(audience);

        page = mock(ContentEntityObject.class);
    }

    @Test
    public void test_header_parsing() {
        List<Header> headers = macroService.buildHeadersFromBody(body, Collections.emptyMap(), request);
        assertThat(headers.get(0).getText(), equalTo("header1"));
        assertThat(headers.get(1).getText(), equalTo("header2"));
        assertThat(headers, hasSize(5));
    }

    @Test
    public void test_default_standard_headers() {
        List<Header> headers = macroService.buildHeadersFromBody(body, Collections.emptyMap(), request);
        assertThat(headers.get(2).getText(), equalTo("Error retrieving text key: multivote.result"));
        assertThat(headers.get(3).getText(), equalTo("Error retrieving text key: multivote.audience"));
        assertThat(headers.get(4).getText(), equalTo("Error retrieving text key: multivote.vote"));
        assertThat(headers, hasSize(5));
    }

    @Test
    public void test_configurable_standard_headers() {
        Map<String, String> params = new HashMap<String, String>() {{
            put("resultHeaderName", "result header");
            put("audienceHeaderName", "audience header");
            put("voteHeaderName", "vote header");
        }};
        List<Header> headers = macroService.buildHeadersFromBody(body, params, request);
        assertThat(headers.get(2).getText(), equalTo("result header"));
        assertThat(headers.get(3).getText(), equalTo("audience header"));
        assertThat(headers.get(4).getText(), equalTo("vote header"));
        assertThat(headers, hasSize(5));
    }

    @Test
    public void test_body_parsing() {
        List<VoteItem> items = macroService.buildItemsFromBody(page, "tableId", body);

        VoteItem item = items.get(0);
        assertThat(items, hasSize(2));
        assertThat(item.getIdName(), equalTo("4711"));

        String user = item.getAudience().iterator().next();

        assertThat(user, equalTo(USER_IN_AUDIENCE));
        assertThat(item.getAudienceCount(), equalTo(1));

        List<String> fields = item.getFields();
        assertThat(fields.get(0), equalTo("bla"));
        assertThat(fields.get(1), equalTo("blubb"));
        assertThat(fields, hasSize(2));

        item = items.get(1);
        assertThat(items, hasSize(2));
        assertThat(item.getIdName(), equalTo("0815"));

        user = item.getAudience().iterator().next();

        assertThat(user, equalTo(USER_IN_AUDIENCE));
        assertThat(item.getAudienceCount(), equalTo(1));

        fields = item.getFields();
        assertThat(fields.get(0), equalTo("bloerk"));
        assertThat(fields.get(1), equalTo("plopp"));
        assertThat(fields, hasSize(2));
    }

}
