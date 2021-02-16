/* (C)2020 */
package ru.mail.jira.plugins.myteam.rest.dto;

import java.net.MalformedURLException;
import java.net.URL;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChannelChatInfo;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatInfoResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.GroupChatInfo;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.PrivateChatInfo;

@AllArgsConstructor
@XmlRootElement
public class ChatMetaDto {
  @XmlElement @Getter @Setter private String link;
  @XmlElement @Getter @Setter private String name;

  private static final String MYTEAM_INVITE_LINK_HOST = "u.internal.myteam.mail.ru";
  /**
   * Validate that link string is is a correct chat invite link
   *
   * @param inviteLink link string to validate is a correct chat invite link
   * @return correct invite link
   */
  private static String clarifyChatInviteLink(String inviteLink) {
    try {
      URL url = new URL(inviteLink);
      if (!url.getHost().equals(MYTEAM_INVITE_LINK_HOST)) {
        String someId = StringUtils.substringAfterLast(url.getPath(), "/");
        return String.join("/", "https:/", MYTEAM_INVITE_LINK_HOST, "profile", someId);
      }
    } catch (MalformedURLException ignored) {
      // inviteLink is not a correct URL at all... just leave it as it is
    }
    return inviteLink;
  }

  @Nullable
  public static ChatMetaDto buildChatInfo(ChatInfoResponse chatInfoResponse) {
    if (chatInfoResponse instanceof GroupChatInfo) {
      GroupChatInfo groupChatInfo = (GroupChatInfo) chatInfoResponse;
      return new ChatMetaDto(
          clarifyChatInviteLink(groupChatInfo.getInviteLink()), groupChatInfo.getTitle());
    } else if (chatInfoResponse instanceof ChannelChatInfo) {
      ChannelChatInfo channelChatInfo = (ChannelChatInfo) chatInfoResponse;
      return new ChatMetaDto(
          clarifyChatInviteLink(channelChatInfo.getInviteLink()), channelChatInfo.getTitle());
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
