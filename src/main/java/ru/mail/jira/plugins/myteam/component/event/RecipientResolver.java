/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event;

public interface RecipientResolver<T, R> {

  R resolve(T data);
}
