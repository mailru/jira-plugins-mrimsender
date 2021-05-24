/* (C)2020 */
package ru.mail.jira.plugins.myteam.rest.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@XmlRootElement
public class ChatMemberDto {
  @XmlElement @Getter @Setter private String name;
  @XmlElement @Getter @Setter private Long id;
  @XmlElement @Getter @Setter private String src;
}
