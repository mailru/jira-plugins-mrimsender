package ru.mail.jira.plugins.myteam.bitbucket;

public interface BitbucketWebhookEvent {
    String getProjectName();
    String getRepoSlug();
}
