package ru.mail.jira.plugins.mrimsender.icq;


public class Consts {
    public enum EventType {
        NEW_MESSAGE_TYPE("newMessage"),
        CALLBACK_QUERY_TYPE("callbackQuery");

        private String typeStrValue;

        EventType(String typeStrValue) {
            this.typeStrValue = typeStrValue;
        }

        public String getTypeStrValue() {
            return typeStrValue;
        }

    }

    public enum PartType {
        MENTION_TYPE("mention"),
        FILE_TYPE("file");

        private String typeStrValue;

        PartType(String typeStrValue) {
            this.typeStrValue = typeStrValue;
        }

        public String getTypeStrValue() {
            return typeStrValue;
        }
    }
}
