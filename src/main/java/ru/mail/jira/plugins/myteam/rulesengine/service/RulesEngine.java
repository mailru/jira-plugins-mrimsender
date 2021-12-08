/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import org.jeasy.rules.api.Facts;

public interface RulesEngine {
  void fireCommandRule(Facts facts);

  void fireServiceRule(Facts facts);

  void fireStateActionRule(Facts facts);
}
