package com.tngtech.confluence.plugin.spring;

import com.atlassian.confluence.cluster.ClusterManager;
import com.atlassian.confluence.core.ContentEntityManager;
import com.atlassian.confluence.core.ContentPropertyManager;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.tngtech.confluence.plugin.DefaultMultiVoteService;
import com.tngtech.confluence.plugin.MultiVoteMacroService;
import com.tngtech.confluence.plugin.MultiVoteService;
import com.tngtech.confluence.plugin.MultivoteMacro;
import com.tngtech.confluence.plugin.MultivoteMacro3x;
import com.tngtech.confluence.plugin.MultivoteRestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppBeans {

    @Bean
    public DefaultMultiVoteService multiVoteService(ContentPropertyManager contentPropertyManager,
                                                    UserAccessor userAccessor,
                                                    ClusterManager clusterManager,
                                                    XhtmlContent xmlXhtmlContent) {
        return new DefaultMultiVoteService(contentPropertyManager, userAccessor, clusterManager, xmlXhtmlContent);
    }

    @Bean
    public MultiVoteMacroService multiVoteMacroService(MultiVoteService multiVoteService,
                                                       ContentEntityManager contentEntityManager) {
        return new MultiVoteMacroService(multiVoteService, contentEntityManager);
    }

    @Bean
    public MultivoteRestService multivoteRestService(ContentEntityManager contentEntityManager,
                                                      UserAccessor userAccessor,
                                                      PermissionManager permissionManager,
                                                      MultiVoteService multiVote) {
        return new MultivoteRestService(contentEntityManager, userAccessor, permissionManager, multiVote);
    }

    @Bean
    public MultivoteMacro multivoteMacro(MultiVoteMacroService multiVoteMacroService) {
        return new MultivoteMacro(multiVoteMacroService);
    }

    @Bean
    public MultivoteMacro3x multivoteMacro3x(MultiVoteMacroService multiVoteMacroService) {
        return new MultivoteMacro3x(multiVoteMacroService);
    }

}
