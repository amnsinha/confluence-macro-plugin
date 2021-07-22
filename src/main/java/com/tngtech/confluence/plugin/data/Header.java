package com.tngtech.confluence.plugin.data;

import com.atlassian.confluence.velocity.htmlsafe.HtmlSafe;
import org.apache.commons.lang.StringUtils;

public class Header {
    private String cssClass = "";
    private String text;

    public Header(String text) {
        this.text = text;
    }

    public Header(String text, String... cssClass) {
        this.text = text;
        this.cssClass = StringUtils.join(cssClass, " ");
    }

    @HtmlSafe
    public String getText() {
        return text;
    }

    @HtmlSafe
    public String getCssClass() {
        return cssClass;
    }
}
