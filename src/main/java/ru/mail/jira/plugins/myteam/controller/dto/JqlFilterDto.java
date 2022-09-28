/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.dto;

import javax.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@AllArgsConstructor
public class JqlFilterDto {
  @Nullable @XmlElement private Long id;

  @Nullable @XmlElement private String name;

  @Nullable @XmlElement private String jql;
}
