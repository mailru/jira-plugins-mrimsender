package ru.mail.jira.plugins.mrimsender.icq.dto;

public class Chat {
    private String chatId;
    private String title;
    private String type;

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Chat{" +
                "chatId='" + chatId + '\'' +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
