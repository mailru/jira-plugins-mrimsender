/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket;

public interface BitbucketWebhookEvent {
  String getProjectKey();

  String getRepoSlug();
}
