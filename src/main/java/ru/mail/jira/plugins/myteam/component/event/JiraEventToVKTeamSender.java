/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event;

public interface JiraEventToVKTeamSender<T> {

  void send(T data);
}
