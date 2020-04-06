package ru.mail.jira.plugins.mrimsender.icq;


public class Consts {
    public static enum EventType {
        NEW_MESSAGE_TYPE("newMessage"),
        DELETED_MESSAGE_TYPE("deletedMessage"),
        EDITED_MESSAGE_TYPE("editedMessage"),
        PINNED_MESSAGE_TYPE("pinnedMessage"),
        UNPINNED_MESSAGE_TYPE("unpinnedMessage"),
        NEW_CHAT_MEMBERS_TYPE("newChatMembers"),
        LEFT_CHAT_MEMBERS_TYPE("leftChatMembers"),
        CALLBACK_QUERY_TYPE("callbackQuery");

        private String typeStrValue;

        EventType(String typeStrValue) {
            this.typeStrValue = typeStrValue;
        }

        public String getTypeStrValue() {
            return typeStrValue;
        }

    }

    public static enum PartType {
        STICKER_TYPE("sticker"),
        MENTION_TYPE("mention"),
        VOICE_TYPE("voice"),
        FILE_TYPE("file"),
        FORWARD_TYPE("forward"),
        REPLY_TYPE("reply");

        private String typeStrValue;

        PartType(String typeStrValue) {
            this.typeStrValue = typeStrValue;
        }

        public String getTypeStrValue() {
            return typeStrValue;
        }
    }
}
