/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event;

public interface JiraEventToChatMessageConverter<T> {

  String convert(T data);
}
