/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.core;

import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.RuleListener;
import ru.mail.jira.plugins.commons.SentryClient;

@Slf4j
public class MyRuleListener implements RuleListener {
  @Override
  public void onEvaluationError(Rule rule, Facts facts, Exception exception) {
    SentryClient.capture(exception);
    log.error(
        "Rule evaluation {} was failed with facts: {}\nMessage: {}",
        rule.getName(),
        facts.toString(),
        exception.getLocalizedMessage(),
        exception);
  }

  @Override
  public void onFailure(Rule rule, Facts facts, Exception exception) {
    SentryClient.capture(exception);
    log.error(
        "Rule {} was failed with facts: {}\nMessage: {}",
        rule.getName(),
        facts.toString(),
        exception.getLocalizedMessage(),
        exception);
  }
}
