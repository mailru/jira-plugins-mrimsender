/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam.dto;

import javax.annotation.Nullable;

public enum ChatType {
  PRIVATE("private"),
  GROUP("group"),
  CHANNEL("channel");

  private final String apiValue;

  ChatType(final String apiValue) {
    this.apiValue = apiValue;
  }

  @Nullable
  public static ChatType fromApiValue(final String value) {
    if (PRIVATE.apiValue.equalsIgnoreCase(value)) {
      return PRIVATE;
    } else if (GROUP.apiValue.equalsIgnoreCase(value)) {
      return GROUP;
    } else if (CHANNEL.apiValue.equalsIgnoreCase(value)) {
      return CHANNEL;
    } else {
      return null;
    }
  }

  public String getApiValue() {
    return apiValue;
  }
}
