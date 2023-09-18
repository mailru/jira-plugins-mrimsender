/* (C)2023 */
package ru.mail.jira.plugins.myteam.component.url;

public interface UrlFinder<R, P> {

  R findUrls(P source);
}
