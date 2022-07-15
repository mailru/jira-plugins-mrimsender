/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes;

import lombok.Getter;

public enum StateActionRuleType implements RuleType {
  ShowCreatingIssueProgressMessage("showCreatingIssueProgressMessage"),
  SelectAdditionalField("selectAdditionalField"),
  SelectIssueType("selectIssueType"),
  SelectIssueCreationValue("selectIssueCreationValue"),
  EditIssueCreationValue("editIssueCreationValue"),
  ConfirmIssueCreation("confirmIssueCreation"),
  SelectIssueTransition("selectIssueTransition"),
  AddAdditionalFields("addAdditionalFields");

  @Getter(onMethod_ = {@Override})
  private final String name;

  StateActionRuleType(String name) {
    this.name = name;
  }

  @Override
  public boolean equalsName(String otherName) {
    return name.equals(otherName);
  }
}
