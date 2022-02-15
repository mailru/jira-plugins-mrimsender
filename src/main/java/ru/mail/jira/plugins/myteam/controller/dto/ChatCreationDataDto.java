/* (C)2020 */
package ru.mail.jira.plugins.myteam.controller.dto;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@XmlRootElement
public class ChatCreationDataDto {
  @XmlElement @Getter @Setter private String name;

  @XmlElement @Getter @Setter private List<ChatMemberDto> members;
}
