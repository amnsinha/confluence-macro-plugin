package com.tngtech.confluence.plugin;

import com.atlassian.confluence.core.ContentEntityManager;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.renderer.PageContext;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.plugins.rest.common.security.AuthenticationContext;
import com.atlassian.renderer.v2.macro.MacroException;
import com.tngtech.confluence.plugin.data.DenyReason;
import com.tngtech.confluence.plugin.data.ItemKey;
import com.tngtech.confluence.plugin.data.VoteItem;
import com.tngtech.confluence.plugin.data.VoteResponse;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.Principal;
import java.util.List;
import java.util.Set;

import static com.atlassian.confluence.core.ConfluenceActionSupport.getTextStatic;

@Path("/")
public class MultivoteRestService {
    private static final Logger log = Logger.getLogger(MultivoteRestService.class);

    private static final String ERROR_TITLE = getTextStatic(
            "com.tngtech.confluence.plugin.multivote.voting.error.technical.title");
    private static final String ERROR_MESSAGE = getTextStatic(
            "com.tngtech.confluence.plugin.multivote.voting.error.technical.message");

    private final ContentEntityManager contentEntityManager;
    private final UserAccessor userAccessor;
    private final PermissionManager permissionManager;
    private final MultiVoteService multiVote;

    public MultivoteRestService(ContentEntityManager contentEntityManager,
                                UserAccessor userAccessor,
                                PermissionManager permissionManager,
                                MultiVoteService multiVote) {
        this.contentEntityManager = contentEntityManager;
        this.userAccessor = userAccessor;
        this.permissionManager = permissionManager;
        this.multiVote = multiVote;
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/page/{pageId}/table/{tableId}/item/{itemId}")
    public Response voteInterested(@PathParam("pageId") String pageId, @PathParam("tableId") String tableId,
                                   @PathParam("itemId") String itemId, @QueryParam("interested") Boolean interested,
                                   @Context AuthenticationContext authenticationContext) {
        String user = getUser(authenticationContext);
        ContentEntityObject abstractPage = contentEntityManager.getById(Long.parseLong(pageId));

        if (illegalAccess(user, abstractPage)) {
            log.warn("Request from unauthenticated/unauthorized user");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        ItemKey itemKey = new ItemKey(abstractPage, tableId, itemId);

        VoteItem item = multiVote.recordInterest(user, interested, itemKey);
        Set<String> audience = item.getAudience();
        String userFullNamesAsString = multiVote.getUserFullNamesAsString(audience);
        boolean interestedUpdated = audience.contains(user);

        String userFullNamesAsHtml;
        try {
            userFullNamesAsHtml = multiVote.getUserFullNamesAsHtml(audience, new PageContext(abstractPage));
        } catch (MacroException e) {
            log.error("failed to create user list", e);
            VoteResponse entity = new VoteResponse(itemId, userFullNamesAsString, userFullNamesAsString,
                    item.getAudienceCount(), interestedUpdated, item.getChanged(), ERROR_TITLE, ERROR_MESSAGE);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(entity).build();
        }

        VoteResponse entity = new VoteResponse(itemId, userFullNamesAsString, userFullNamesAsHtml,
                item.getAudienceCount(), interestedUpdated, item.getChanged(), getErrorTitle(item.getDenyReason()),
                getErrorMessage(item));
        return Response.ok(entity).build();
    }

    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/page/{pageId}/table/{tableId}")
    public Response reset(@PathParam("pageId") String pageId, @PathParam("tableId") String tableId,
                          @QueryParam("itemId") List<String> itemIds, @Context AuthenticationContext authenticationContext) {
        String user = getUser(authenticationContext);
        ContentEntityObject abstractPage = contentEntityManager.getById(Long.parseLong(pageId));

        if (illegalModification(user, abstractPage)) {
            log.warn("Request from unauthenticated/unauthorized user");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        multiVote.reset(abstractPage, tableId, itemIds);

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private String getErrorMessage(VoteItem item) {
        switch (item.getDenyReason()) {
            case NUMBER_OF_USER_VOTES:
                return getTextStatic("com.tngtech.confluence.plugin.multivote.voting.error.too-many-user-votes.message",
                        new Object[]{item.getMaxUserVotes()});
            case NUMBER_OF_ROW_VOTES:
                return getTextStatic("com.tngtech.confluence.plugin.multivote.voting.error.too-many-item-votes.message",
                        new Object[]{item.getMaxItemVotes()});
            default:
                return "";
        }
    }

    private String getErrorTitle(DenyReason denyReason) {
        switch (denyReason) {
            case NUMBER_OF_USER_VOTES:
                return getTextStatic("com.tngtech.confluence.plugin.multivote.voting.error.too-many-user-votes.title");
            case NUMBER_OF_ROW_VOTES:
                return getTextStatic("com.tngtech.confluence.plugin.multivote.voting.error.too-many-item-votes.title");
            default:
                return "";
        }
    }

    private boolean illegalAccess(String user, ContentEntityObject abstractPage) {
        return user == null
                || !permissionManager.hasPermission(userAccessor.getUser(user), Permission.VIEW, abstractPage);
    }

    private boolean illegalModification(String user, ContentEntityObject abstractPage) {
        return user == null
                || !permissionManager.hasPermission(userAccessor.getUser(user), Permission.EDIT, abstractPage);
    }

    private String getUser(AuthenticationContext context) {
        final Principal principal = context.getPrincipal();
        if (principal != null) {
            return principal.getName();
        }
        return null;
    }

}
