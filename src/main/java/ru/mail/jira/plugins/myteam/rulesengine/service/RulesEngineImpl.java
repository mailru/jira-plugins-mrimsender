/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import org.jeasy.rules.api.Facts;
import org.springframework.stereotype.Component;

@Component
public class RulesEngineImpl implements RulesEngine {
  @Override
  public void fireCommandRule(Facts facts) {}

  @Override
  public void fireServiceRule(Facts facts) {}

  @Override
  public void fireStateActionRule(Facts facts) {}
}
