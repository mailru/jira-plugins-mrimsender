/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes;

import lombok.Getter;

public enum ButtonRuleType implements RuleType {
  NextPage("next"),
  PrevPage("previous"),
  Cancel("cancel"),
  SearchIssueByJqlInput("searchIssueByJqlInput"),
  SearchIssueByKeyInput("searchIssueByKeyInput"),
  ViewComments("viewComments"),
  CommentIssue("commentIssue"),
  CreateIssue("createIssue");

  @Getter(onMethod_ = {@Override})
  private final String name;

  ButtonRuleType(String name) {
    this.name = name;
  }

  @Override
  public boolean equalsName(String otherName) {
    return name.equals(otherName);
  }
}
