package com.tngtech.confluence.plugin.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "interested")
@XmlAccessorType(XmlAccessType.FIELD)
public class VoteResponse {
    @XmlElement(name = "id")
    private String  id;
    @XmlAttribute
    private String  users;
    @XmlAttribute
    private String  htmlUsers;
    @XmlAttribute
    private int     userNo;
    @XmlAttribute
    private boolean interested;
    @XmlAttribute
    private boolean updated;
    @XmlAttribute
    private String  errorTitle;
    @XmlAttribute
    private String  errorMessage;

    public VoteResponse() {
    }

    public VoteResponse(String id, String users, String htmlUsers, int userNo, boolean interested, boolean updated,
            String errorTitle, String errorMessage) {
        this.id = id;
        this.users = users;
        this.userNo = userNo;
        this.htmlUsers = htmlUsers;
        this.interested = interested;
        this.updated = updated;
        this.errorMessage = errorMessage;
        this.errorTitle = errorTitle;
    }

    public String getId() {
        return id;
    }

    public String getUsers() {
        return users;
    }

    public String htmlUsers() {
        return htmlUsers;
    }

    public int getUserNo() {
        return userNo;
    }

    public boolean getInterested() {
        return interested;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorTitle() {
        return errorTitle;
    }

    public void setErrorTitle(String errorTitle) {
        this.errorTitle = errorTitle;
    }
}
