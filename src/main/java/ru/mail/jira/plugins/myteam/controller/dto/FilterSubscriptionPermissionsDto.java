/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.dto;

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
public class FilterSubscriptionPermissionsDto {
  @XmlElement private boolean jiraAdmin;
}
