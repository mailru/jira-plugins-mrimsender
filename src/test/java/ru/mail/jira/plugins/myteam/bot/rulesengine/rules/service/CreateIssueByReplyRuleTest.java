/* (C)2022 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.mail.jira.plugins.myteam.commons.Const.DEFAULT_ISSUE_QUOTE_MESSAGE_TEMPLATE;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

public class CreateIssueByReplyRuleTest {
  @Test
  public void testReplaceIssueDescriptionPattern() {
    assertEquals(
        "{quote} 123$ {quote}",
        StringUtils.replace(DEFAULT_ISSUE_QUOTE_MESSAGE_TEMPLATE, "{{message}}", "123$"));
    assertEquals(
        "{quote} 123$ {quote} 123$",
        StringUtils.replace("{quote} {{message}} {quote} {{message}}", "{{message}}", "123$"));
  }
}
