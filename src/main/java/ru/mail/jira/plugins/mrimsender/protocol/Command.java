package ru.mail.jira.plugins.mrimsender.protocol;

public class Command {
    private final String email;
    private final String message;

    public Command(String email, String message) {
        this.email = email;
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public String getMessage() {
        return message;
    }
}
