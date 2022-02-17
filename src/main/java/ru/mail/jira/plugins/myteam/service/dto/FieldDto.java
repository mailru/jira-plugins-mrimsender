/* (C)2022 */
package ru.mail.jira.plugins.myteam.service.dto;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@XmlRootElement
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldDto {
  @XmlElement private String id;
  @XmlElement private String name;
  @XmlElement private String value;
  @XmlElement private List<String> values;
  @XmlElement private String html;

  public FieldDto(String name, String value, String html) {
    this.name = name;
    this.value = value;
    this.html = html;
  }
}
