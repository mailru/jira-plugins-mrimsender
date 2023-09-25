/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.states;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;

@Getter
public class CommentingIssueFromGroupChatState extends BotState {

  private final ChatMessageEvent chatMessageEvent;

  public CommentingIssueFromGroupChatState(final ChatMessageEvent chatMessageEvent) {
    this.chatMessageEvent = chatMessageEvent;
  }
}
