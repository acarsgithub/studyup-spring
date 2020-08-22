package io.study.studyup.models;

public class Request {

    String groupname;
    String groupadmin_username;
    String requester_username;

    public Request(){};

    public void setGroupname(String groupname) {
        this.groupname = groupname;
    }

    public void setGroupadmin_username(String groupadmin_username) {
        this.groupadmin_username = groupadmin_username;
    }

    public void setRequester_username(String requester_username) {
        this.requester_username = requester_username;
    }

    public String getGroupname() {
        return groupname;
    }

    public String getGroupadmin_username() {
        return groupadmin_username;
    }

    public String getRequester_username() {
        return requester_username;
    }

}
