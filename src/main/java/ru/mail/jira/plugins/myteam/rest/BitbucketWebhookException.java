/* (C)2021 */
package ru.mail.jira.plugins.myteam.rest;

public class BitbucketWebhookException extends RuntimeException {
  public BitbucketWebhookException(String message) {
    super(message);
  }
}
