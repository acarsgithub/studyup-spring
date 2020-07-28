package io.study.studyup.models;

public class Group {

    String groupname;
    String groupadmin_username;
    int groupadmin_id;
    int numusers;
    String subject;
    String description;

    public Group() {
    }

    public void setGroupname(String groupname) {
        this.groupname = groupname;
    }

    public void setGroupadmin_username(String groupadmin_username) {
        this.groupadmin_username = groupadmin_username;
    }

    public void setGroupadmin_id(int groupadmin_id) {
        this.groupadmin_id = groupadmin_id;
    }

    public void setNumusers(int numusers) {
        this.numusers = numusers;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroupname() {
        return groupname;
    }

    public String getGroupadmin_username() {
        return groupadmin_username;
    }

    public int getGroupadmin_id() {
        return groupadmin_id;
    }

    public int getNumusers() {
        return numusers;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }


}
