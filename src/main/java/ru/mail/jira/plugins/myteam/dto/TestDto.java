/* (C)2022 */
package ru.mail.jira.plugins.myteam.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlRootElement
public class TestDto {
  @XmlElement private String field;
  //  @XmlElement private String field2;
}
