package com.tngtech.confluence.plugin.data;

import com.atlassian.confluence.core.ContentEntityObject;

public class ItemKey {
    private String itemId;
    private String tableId;
    private ContentEntityObject page;

    public ItemKey(ContentEntityObject page, String tableId, String itemId) {
        this.page = page;
        this.tableId = tableId;
        this.itemId = itemId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getTableId() {
        return tableId;
    }

    public ContentEntityObject getPage() {
        return page;
    }
}
