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
        "\n> 123$\n",
        StringUtils.replace(DEFAULT_ISSUE_QUOTE_MESSAGE_TEMPLATE, "{{message}}", "123$"));
    assertEquals(
        "> 123$\n123$", StringUtils.replace("> {{message}}\n{{message}}", "{{message}}", "123$"));
  }
}
