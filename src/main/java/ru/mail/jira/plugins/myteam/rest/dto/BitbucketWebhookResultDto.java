/* (C)2021 */
package ru.mail.jira.plugins.myteam.rest.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@XmlRootElement
@Getter
@Setter
public class BitbucketWebhookResultDto {
  @XmlElement private String resultInfo;
}
