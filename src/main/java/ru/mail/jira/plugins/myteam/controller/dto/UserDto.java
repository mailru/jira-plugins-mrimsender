/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.dto;

import com.atlassian.jira.user.ApplicationUser;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("NullAway")
public class UserDto {
  @NotNull @XmlElement private String userKey;

  @NotNull @XmlElement private String displayName;

  public UserDto(ApplicationUser user) {
    this.userKey = user.getKey();
    this.displayName = user.getDisplayName();
  }
}
