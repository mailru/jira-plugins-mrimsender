/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

@JsonTypeInfo(
    defaultImpl = BitbucketEventDto.class,
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventKey")
@JsonSubTypes({
  // repo events
  @JsonSubTypes.Type(value = RepositoryPush.class, name = "repo:refs_changed"),
  @JsonSubTypes.Type(value = RepositoryModified.class, name = "repo:modified"),
  @JsonSubTypes.Type(value = RepositoryForked.class, name = "repo:forked"),
  @JsonSubTypes.Type(
      value = RepositoryCommitCommentCreated.class,
      name = "repo:comment:added"),
  @JsonSubTypes.Type(
      value = RepositoryCommitCommentEdited.class,
      name = "repo:comment:edited"),
  @JsonSubTypes.Type(
      value = RepositoryCommitCommentDeleted.class,
      name = "repo:comment:deleted"),
  @JsonSubTypes.Type(
      value = RepositoryMirrorSynchronized.class,
      name = "mirror:repo_synchronized"),
  // pr events
  @JsonSubTypes.Type(value = PullRequestOpened.class, name = "pr:opened"),
  @JsonSubTypes.Type(value = PullRequestModified.class, name = "pr:modified"),
  @JsonSubTypes.Type(value = PullRequestReviewersUpdated.class, name = "pr:reviewer:updated"),
  @JsonSubTypes.Type(value = PullRequestApprovedByReviewer.class, name = "pr:reviewer:approved"),
  @JsonSubTypes.Type(
      value = PullRequestUnapprovedByReviewer.class,
      name = "pr:reviewer:unapproved"),
  @JsonSubTypes.Type(value = PullRequestNeedsWorkByReviewer.class, name = "pr:reviewer:needs_work"),
  @JsonSubTypes.Type(value = PullRequestMerged.class, name = "pr:merged"),
  @JsonSubTypes.Type(value = PullRequestDeclined.class, name = "pr:declined"),
  @JsonSubTypes.Type(value = PullRequestDeleted.class, name = "pr:deleted"),
  @JsonSubTypes.Type(value = PullRequestCommentAdded.class, name = "pr:comment:added"),
  @JsonSubTypes.Type(value = PullRequestCommentEdited.class, name = "pr:comment:edited"),
  @JsonSubTypes.Type(value = PullRequestCommentDeleted.class, name = "pr:comment:deleted"),
})
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketEventDto {
  private String date;
}
