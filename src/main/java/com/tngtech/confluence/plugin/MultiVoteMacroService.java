package com.tngtech.confluence.plugin;

import com.atlassian.confluence.core.ContentEntityManager;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.renderer.PageContext;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.macro.MacroException;
import com.opensymphony.webwork.ServletActionContext;
import com.tngtech.confluence.plugin.data.Header;
import com.tngtech.confluence.plugin.data.ItemKey;
import com.tngtech.confluence.plugin.data.VoteItem;
import jodd.jerry.Jerry;
import jodd.jerry.JerryFunction;
import jodd.lagarto.dom.Node;
import org.apache.commons.lang.StringEscapeUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.atlassian.confluence.core.ConfluenceActionSupport.getTextStatic;
import static jodd.jerry.Jerry.jerry;

public class MultiVoteMacroService {
    private static final String VALID_ID_PATTERN = "^\\p{Alpha}(\\p{Alnum}|\\.|_|:)+$";
    private static final String TEMPLATE = "templates/extra/multivote.vm";

    private final MultiVoteService multiVote;
    private final ContentEntityManager contentEntityManager;

    public MultiVoteMacroService(MultiVoteService multiVoteService, ContentEntityManager contentEntityManager) {
        this.multiVote = multiVoteService;
        this.contentEntityManager = contentEntityManager;
    }

    public String execute(Map params, String body, RenderContext renderContext) throws MacroException {
        ContentEntityObject page = contentEntityManager.getById(((PageContext) renderContext).getEntity().getLatestVersionId());

        String tableId = (String) params.get("0");
        if (tableId == null) {
            tableId = (String) params.get("id");
        }
        String shouldSort = (String) params.get("sort");
        checkValidityOf(tableId);

        final HttpServletRequest request = ServletActionContext.getRequest();
        recordVote(page, tableId, request);
        final Jerry xhtml = jerry(body);
        List<VoteItem> items = buildItemsFromBody(page, tableId, xhtml);
        sortIf(shouldSort, items);
        List<Header> headers = buildHeadersFromBody(xhtml, params, request);
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();

        contextMap.put("items", items);
        contextMap.put("tableId", tableId);
        contextMap.put("headers", headers);
        contextMap.put("content", page);
        contextMap.put("multiVote", multiVote);
        contextMap.put("context", renderContext);
        return VelocityUtils.getRenderedTemplate(TEMPLATE, contextMap);
    }

    /**
     * parse the table-body of the macro. It assumes that the format is:
     * <pre>
     * |  ID    | header_1 | ( header_n | )+
     * | idName | column_1 | ( column_n | )+
     * </pre>
     *
     * @param page    the Macro is on
     * @param tableId id of the table the Macro will generate
     * @param xhtml   of the macro body
     * @return list of {@link com.tngtech.confluence.plugin.data.VoteItem}
     */
    List<VoteItem> buildItemsFromBody(final ContentEntityObject page, final String tableId, Jerry xhtml) {
        final List<VoteItem> items = new ArrayList<>();
        final Jerry lines = xhtml.$("table").find("tr");

        lines.gt(0).each((JerryFunction) (me, index) -> {
            Jerry children = me.children();
            final List<String> fields = new ArrayList<>();

            String itemId = children.get(0).getTextContent().trim();
            checkItemId(itemId);

            for (Node node : children.gt(0).get()) {
                fields.add(node.getInnerHtml().trim());
            }

            VoteItem item = new VoteItem(itemId, fields, multiVote.retrieveAudience(new ItemKey(page, tableId, itemId)));
            items.add(item);
            return true;
        });
        return items;
    }

    private static void checkItemId(String itemId) {
        if (!itemId.matches("^[-0-9a-zA-Z_:.]+$")) {
            throw new MultivoteMacroException("id is only allowed to contain the following characters: [-0-9a-zA-Z_:.], but was '" + itemId + '"');
        }
    }

    /**
     * parse the table-header of the macro. It assumes that the format is:
     * <pre>
     * |  ID    | header_1 | ( header_n | )+
     * </pre>
     *
     * @param xhtml   of the Macro
     * @param params
     * @param request
     * @return list of {@link com.tngtech.confluence.plugin.data.Header;}
     */
    List<Header> buildHeadersFromBody(Jerry xhtml, Map params, HttpServletRequest request) {
        final List<Header> columns = new ArrayList<>();

        final Jerry lines = xhtml.$("table").find("tr");

        for (Node node : lines.first().children().gt(0).get()) {
            columns.add(new Header(node.getInnerHtml().trim()));
        }

        columns.add(getHeader(params, "resultHeaderName", "multivote.result"));
        columns.add(getHeader(params, "audienceHeaderName", "multivote.audience", "multivoteAudienceColumn"));

        if (request != null && request.getRemoteUser() != null) {
            columns.add(getHeader(params, "voteHeaderName", "multivote.vote"));
        }

        return columns;
    }

    private Header getHeader(Map params, String headerName, String defaultI18nKey, String... classes) {
        String resultHeader = (String) params.get(headerName);
        if (resultHeader == null) {
            resultHeader = getTextStatic(defaultI18nKey);
        } else {
            resultHeader = StringEscapeUtils.escapeHtml(resultHeader);
        }
        return new Header(resultHeader, classes);
    }

    private void recordVote(ContentEntityObject contentObject, String tableId, HttpServletRequest request) throws MacroException {
        if (request != null && request.getMethod().equals("POST")) {
            String remoteUser = request.getRemoteUser();
            String idName = request.getParameter("multivote.idname");
            String interested = request.getParameter("multivote.interested");

            if (remoteUser == null || idName == null || interested == null) {
                return;
            }

            try {
                String requestItem = URLDecoder.decode(idName, "UTF-8");
                String requestUse = URLDecoder.decode(interested, "UTF-8");
                if (tableId.equals(request.getParameter("multivote.tableId"))) {
                    multiVote.recordInterest(remoteUser, Boolean.parseBoolean(requestUse), new ItemKey(contentObject, tableId, requestItem));
                }
            } catch (UnsupportedEncodingException e) {
                throw new MacroException(e);
            }
        }
    }

    private void sortIf(String shouldSort, List<VoteItem> items) {
        if ("true".equals(shouldSort)) {
            Collections.sort(items);
        }
    }

    private void checkValidityOf(String tableId) throws MacroException {
        if (tableId == null) {
            throw new MacroException("id is mandatory");
        } else if (!tableId.matches(VALID_ID_PATTERN)) {
            throw new MacroException("id is only allowed to contain alphanumeric characters and has to start with a letter, but was '" + tableId + "'");
        }
    }
}
