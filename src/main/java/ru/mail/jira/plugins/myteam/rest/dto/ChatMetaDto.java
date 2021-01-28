/* (C)2020 */
package ru.mail.jira.plugins.myteam.rest.dto;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChannelChatInfo;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatInfoResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.GroupChatInfo;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.PrivateChatInfo;

@AllArgsConstructor
@XmlRootElement
public class ChatMetaDto {
  @XmlElement @Getter @Setter private String link;
  @XmlElement @Getter @Setter private String name;
  @XmlElement @Getter @Setter private String about;
  @XmlElement @Getter @Setter private String rules;

  @Nullable
  public static ChatMetaDto buildChatInfo(ChatInfoResponse chatInfoResponse) {
    if (chatInfoResponse instanceof GroupChatInfo) {
      GroupChatInfo groupChatInfo = (GroupChatInfo) chatInfoResponse;
      return new ChatMetaDto(
          groupChatInfo.getInviteLink(),
          groupChatInfo.getTitle(),
          groupChatInfo.getAbout(),
          groupChatInfo.getRules());
    } else if (chatInfoResponse instanceof ChannelChatInfo) {
      ChannelChatInfo channelChatInfo = (ChannelChatInfo) chatInfoResponse;
      return new ChatMetaDto(
          channelChatInfo.getInviteLink(),
          channelChatInfo.getTitle(),
          channelChatInfo.getAbout(),
          channelChatInfo.getRules());
    } else if (chatInfoResponse instanceof PrivateChatInfo) {
      // private chat can't be crated from Myteam bot createChat api method
      // so this is not very useful right now
      return null;
    } else {
      // some unresolved chat type passed
      return null;
    }
  }
}
