/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto.utils;

import java.util.List;
import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class PullRequestDto {
  private long id;
  private long version;
  private String title;

  @JsonProperty("open")
  private boolean isOpen;

  @JsonProperty("closed")
  private boolean isClosed;

  private long createDate;
  private long updateDate;
  private RefDto fromRef;
  private RefDto toRef;

  @JsonProperty("locked")
  private boolean isLocked;

  private PullRequestParticipantDto author;
  private List<PullRequestParticipantDto> reviewers;
  private List<PullRequestParticipantDto> participants;
  private LinksDto links;
  private PullRequestPropertiesDto properties;
}
