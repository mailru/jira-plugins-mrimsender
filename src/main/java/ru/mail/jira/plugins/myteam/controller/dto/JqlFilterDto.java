/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.dto;

import com.atlassian.jira.issue.search.SearchRequest;
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
public class JqlFilterDto {
  @NotNull @XmlElement private Long id;

  @NotNull @XmlElement private String name;

  @XmlElement private boolean owner;

  public JqlFilterDto(SearchRequest searchRequest, ApplicationUser user) {
    this.id = searchRequest.getId();
    this.name = searchRequest.getName();
    this.owner = searchRequest.getOwner().equals(user);
  }
}
