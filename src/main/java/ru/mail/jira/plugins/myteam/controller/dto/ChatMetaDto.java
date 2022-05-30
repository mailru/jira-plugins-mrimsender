/* (C)2020 */
package ru.mail.jira.plugins.myteam.controller.dto;

import java.util.List;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChannelChatInfo;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatInfoResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.GroupChatInfo;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.PrivateChatInfo;

@AllArgsConstructor
@XmlRootElement
public class ChatMetaDto {

  @NonNull @XmlElement @Getter @Setter private String link;
  @NonNull @XmlElement @Getter @Setter private String name;
  @XmlElement @Getter @Setter private List<ChatMemberDto> members;

  @Nullable
  public static ChatMetaDto buildChatInfo(
      ChatInfoResponse chatInfoResponse, List<ChatMemberDto> chatMemberDtos) {
    if (chatInfoResponse instanceof GroupChatInfo) {
      GroupChatInfo groupChatInfo = (GroupChatInfo) chatInfoResponse;
      return new ChatMetaDto(
          groupChatInfo.getInviteLink(), groupChatInfo.getTitle(), chatMemberDtos);
    } else if (chatInfoResponse instanceof ChannelChatInfo) {
      ChannelChatInfo channelChatInfo = (ChannelChatInfo) chatInfoResponse;
      return new ChatMetaDto(
          channelChatInfo.getInviteLink(), channelChatInfo.getTitle(), chatMemberDtos);
    } else if (chatInfoResponse instanceof PrivateChatInfo) {
      // private chat can't be created from Myteam bot createChat api method
      // so this is not very useful right now
      return null;
    } else {
      // some unresolved chat type passed
      return null;
    }
  }
}
