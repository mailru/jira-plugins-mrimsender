/* (C)2021 */
package ru.mail.jira.plugins.myteam.commons.exceptions;

import org.jetbrains.annotations.Nullable;

public class MyteamServerErrorException extends Exception {
  public int status;

  public MyteamServerErrorException(int status, String message) {
    super(message);
    this.status = status;
  }

  public MyteamServerErrorException(int status, String message, @Nullable Throwable cause) {
    super(message, cause);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
