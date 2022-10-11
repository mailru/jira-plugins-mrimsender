/* (C)2021 */
package ru.mail.jira.plugins.myteam.myteam.dto.chats;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

@SuppressWarnings("NullAway")
@JsonTypeInfo(
    defaultImpl = ChatInfoResponse.class,
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = PrivateChatInfo.class, name = "private"),
  @JsonSubTypes.Type(value = GroupChatInfo.class, name = "group"),
  @JsonSubTypes.Type(value = ChannelChatInfo.class, name = "channel")
})
public class ChatInfoResponse {}
