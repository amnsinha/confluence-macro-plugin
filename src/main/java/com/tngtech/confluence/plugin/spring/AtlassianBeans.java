package com.tngtech.confluence.plugin.spring;

import com.atlassian.confluence.cluster.ClusterManager;
import com.atlassian.confluence.core.ContentEntityManager;
import com.atlassian.confluence.core.ContentPropertyManager;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.atlassian.plugins.osgi.javaconfig.OsgiServices.importOsgiService;

@Configuration
public class AtlassianBeans {

    @Bean
    public ContentEntityManager contentEntityManager() {
        return importOsgiService(ContentEntityManager.class);
    }

    @Bean
    public ContentPropertyManager contentPropertyManager() {
        return importOsgiService(ContentPropertyManager.class);
    }

    @Bean
    public UserAccessor userAccessor() {
        return importOsgiService(UserAccessor.class);
    }

    @Bean
    public ClusterManager clusterManager() {
        return importOsgiService(ClusterManager.class);
    }

    @Bean
    public XhtmlContent xhtmlContent() {
        return importOsgiService(XhtmlContent.class);
    }

    @Bean
    public PermissionManager permissionManager() {
        return importOsgiService(PermissionManager.class);
    }

}
