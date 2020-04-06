package ru.mail.jira.plugins.mrimsender.icq.dto;

public class User {
    private String firstName;
    private String lastName;
    private String nick;
    private String userId;

    public String getFirstName() {
        return firstName;
    }

    public String getNick() {
        return nick;
    }

    public String getUserId() {
        return userId;
    }

    public String getLastName() {
        return lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "User{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", nick='" + nick + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
