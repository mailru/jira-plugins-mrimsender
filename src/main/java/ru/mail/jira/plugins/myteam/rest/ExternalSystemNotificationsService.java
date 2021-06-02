/* (C)2021 */
package ru.mail.jira.plugins.myteam.rest;

import com.atlassian.applinks.api.*;
import com.atlassian.fugue.Pair;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.stereotype.Controller;
import ru.mail.jira.plugins.myteam.bitbucket.dto.*;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.RepositoryDto;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.protocol.events.BitbucketNotifyEvent;
import ru.mail.jira.plugins.myteam.protocol.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.rest.dto.BitbucketRepoWatcherDto;

@Controller
@Path("/external/notifications/")
public class ExternalSystemNotificationsService {
  private static final Logger log = Logger.getLogger(ExternalSystemNotificationsService.class);
  private static final String bitbucketRepoWatchersRestStr =
      "/rest/additional/1.0/watchers/repository/%s/%s";
  private static final String JIRA_ADMIN_USERNAME_FOR_APP_LINK = "admin";

  private final ApplicationLinkService applicationLinkService;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final UserManager userManager;
  private final UserData userData;
  private final MessageFormatter messageFormatter;
  private final MyteamEventsListener myteamEventsListener;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public ExternalSystemNotificationsService(
      @ComponentImport ApplicationLinkService applicationLinkService,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport UserManager userManager,
      MessageFormatter messageFormatter,
      UserData userData,
      MyteamEventsListener myteamEventsListener) {
    this.applicationLinkService = applicationLinkService;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.userManager = userManager;
    this.messageFormatter = messageFormatter;
    this.userData = userData;
    this.myteamEventsListener = myteamEventsListener;
  }

  @Path("/bitbucket")
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @AnonymousAllowed
  public Void bitbucketProjectEventsWebHook(BitbucketEventDto event) {
    log.error("BITBUCKET JSON =" + event.toString());

    Pair<String, String> projectNameAndRepoSlug = getProjectNameAndRepoSlug(event);
    if (projectNameAndRepoSlug == null) {
      throw new RuntimeException("project name ad repo slug can't be parsed");
    }
    List<BitbucketRepoWatcherDto> allBitbucketRepositoryWatchers =
        getAllBitbucketRepositoryWatchers(
            projectNameAndRepoSlug.left(), projectNameAndRepoSlug.right());
    sendMyteamNotifications(
        allBitbucketRepositoryWatchers.stream()
            .map(watcher -> userData.getUserByMrimLogin(watcher.getEmail())),
        event);
    return null;
  }

  public Pair<String, String> getProjectNameAndRepoSlug(BitbucketEventDto bitbucketEvent) {
    if (bitbucketEvent instanceof RepositoryPushEventDto) {
      RepositoryPushEventDto repositoryPushEventDto = (RepositoryPushEventDto) bitbucketEvent;
      RepositoryDto repo = repositoryPushEventDto.getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof RepositoryModifiedEventDto) {
      RepositoryModifiedEventDto repositoryModifiedEventDto =
          (RepositoryModifiedEventDto) bitbucketEvent;
      RepositoryDto repo = repositoryModifiedEventDto.getOldRepo();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof RepositoryForkedEventDto) {
      RepositoryForkedEventDto repositoryForkedEventDto = (RepositoryForkedEventDto) bitbucketEvent;
      RepositoryDto repo = repositoryForkedEventDto.getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof RepositoryMirrorSynchronizedEventDto) {
      RepositoryMirrorSynchronizedEventDto repositoryMirrorSynchronizedEventDto =
          (RepositoryMirrorSynchronizedEventDto) bitbucketEvent;
      RepositoryDto repo = repositoryMirrorSynchronizedEventDto.getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof RepositoryCommitCommentCreatedEventDto) {
      RepositoryCommitCommentCreatedEventDto repositoryCommitCommentCreatedEventDto =
          (RepositoryCommitCommentCreatedEventDto) bitbucketEvent;
      RepositoryDto repo = repositoryCommitCommentCreatedEventDto.getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof RepositoryCommitCommentEditedEventDto) {
      RepositoryCommitCommentEditedEventDto repositoryCommitCommentEditedEventDto =
          (RepositoryCommitCommentEditedEventDto) bitbucketEvent;
      RepositoryDto repo = repositoryCommitCommentEditedEventDto.getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof RepositoryCommitCommentDeletedEventDto) {
      RepositoryCommitCommentDeletedEventDto repositoryCommitCommentDeletedEventDto =
          (RepositoryCommitCommentDeletedEventDto) bitbucketEvent;
      RepositoryDto repo = repositoryCommitCommentDeletedEventDto.getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }

    // PR events
    if (bitbucketEvent instanceof PullRequestOpened) {
      PullRequestOpened pullRequestOpened = (PullRequestOpened) bitbucketEvent;
      RepositoryDto repo = pullRequestOpened.getPullRequest().getFromRef().getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof PullRequestModified) {
      PullRequestModified pullRequestModified = (PullRequestModified) bitbucketEvent;
      RepositoryDto repo = pullRequestModified.getPullRequest().getFromRef().getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof PullRequestReviewersUpdated) {
      PullRequestReviewersUpdated pullRequestReviewersUpdated =
          (PullRequestReviewersUpdated) bitbucketEvent;
      RepositoryDto repo =
          pullRequestReviewersUpdated.getPullRequest().getFromRef().getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof PullRequestApprovedByReviewer) {
      PullRequestApprovedByReviewer pullRequestApprovedByReviewer =
          (PullRequestApprovedByReviewer) bitbucketEvent;
      RepositoryDto repo =
          pullRequestApprovedByReviewer.getPullRequest().getFromRef().getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof PullRequestUnapprovedByReviewer) {
      PullRequestUnapprovedByReviewer pullRequestUnapprovedByReviewer =
          (PullRequestUnapprovedByReviewer) bitbucketEvent;
      RepositoryDto repo =
          pullRequestUnapprovedByReviewer.getPullRequest().getFromRef().getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof PullRequestNeedsWorkByReviewer) {
      PullRequestNeedsWorkByReviewer pullRequestNeedsWorkByReviewer =
          (PullRequestNeedsWorkByReviewer) bitbucketEvent;
      RepositoryDto repo =
          pullRequestNeedsWorkByReviewer.getPullRequest().getFromRef().getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof PullRequestMerged) {
      PullRequestMerged pullRequestMerged = (PullRequestMerged) bitbucketEvent;
      RepositoryDto repo = pullRequestMerged.getPullRequest().getFromRef().getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof PullRequestDeclined) {
      PullRequestDeclined pullRequestDeclined = (PullRequestDeclined) bitbucketEvent;
      RepositoryDto repo = pullRequestDeclined.getPullRequest().getFromRef().getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof PullRequestDeleted) {
      PullRequestDeleted pullRequestDeleted = (PullRequestDeleted) bitbucketEvent;
      RepositoryDto repo = pullRequestDeleted.getPullRequest().getFromRef().getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof PullRequestCommentAdded) {
      PullRequestCommentAdded pullRequestCommentAdded = (PullRequestCommentAdded) bitbucketEvent;
      RepositoryDto repo = pullRequestCommentAdded.getPullRequest().getFromRef().getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof PullRequestCommentEdited) {
      PullRequestCommentEdited pullRequestCommentEdited = (PullRequestCommentEdited) bitbucketEvent;
      RepositoryDto repo = pullRequestCommentEdited.getPullRequest().getFromRef().getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    if (bitbucketEvent instanceof PullRequestCommentDeleted) {
      PullRequestCommentDeleted pullRequestCommentDeleted =
          (PullRequestCommentDeleted) bitbucketEvent;
      RepositoryDto repo = pullRequestCommentDeleted.getPullRequest().getFromRef().getRepository();
      return new Pair<>(repo.getProject().getKey(), repo.getSlug());
    }
    return null;
  }

  public List<BitbucketRepoWatcherDto> getAllBitbucketRepositoryWatchers(
      String projectKey, String repositorySlug) {
    Optional<ApplicationLink> bitbucketAppLinkOptional =
        StreamSupport.stream(applicationLinkService.getApplicationLinks().spliterator(), false)
            .filter(link -> link.getName().equals("Bitbucket"))
            .findFirst();

    if (!bitbucketAppLinkOptional.isPresent()) {
      throw new RuntimeException("ALARM 1 !");
    }
    ApplicationLink bitbucketAppLink = bitbucketAppLinkOptional.get();

    // pretend to be a jira admin using bitbucket app link
    ApplicationUser currentUser = jiraAuthenticationContext.getLoggedInUser();
    jiraAuthenticationContext.setLoggedInUser(
        userManager.getUserByName(JIRA_ADMIN_USERNAME_FOR_APP_LINK));

    ApplicationLinkRequestFactory authenticatedRequestFactory =
        bitbucketAppLink.createImpersonatingAuthenticatedRequestFactory();
    try {
      ApplicationLinkRequest request =
          authenticatedRequestFactory.createRequest(
              Request.MethodType.GET,
              String.format(bitbucketRepoWatchersRestStr, projectKey, repositorySlug));
      List<BitbucketRepoWatcherDto> watchers =
          request.execute(
              new ApplicationLinkResponseHandler<List<BitbucketRepoWatcherDto>>() {
                @Override
                public List<BitbucketRepoWatcherDto> credentialsRequired(Response response)
                    throws ResponseException {
                  log.error("Bitbucket app-link credential required inside ResponseHandler called");
                  try {

                    return objectMapper.readValue(
                        response.getResponseBodyAsStream(),
                        new TypeReference<List<BitbucketRepoWatcherDto>>() {});
                  } catch (IOException ioException) {
                    log.error("IOException during ObjectMapper.readValue:", ioException);
                    return Collections.emptyList();
                  }
                }

                @Override
                public List<BitbucketRepoWatcherDto> handle(Response response)
                    throws ResponseException {
                  try {
                    return objectMapper.readValue(
                        response.getResponseBodyAsStream(),
                        new TypeReference<List<BitbucketRepoWatcherDto>>() {});
                  } catch (IOException ioException) {
                    log.error("IOException during ObjectMapper.readValue:", ioException);
                    return Collections.emptyList();
                  }
                }
              });
      log.error(
          "BITBUCKET RESPONSE BODY = "
              + watchers.stream()
                  .map(BitbucketRepoWatcherDto::getEmail)
                  .collect(Collectors.joining()));
      return watchers;
    } catch (CredentialsRequiredException e) {
      log.error("CredentialsRequiredException inside getAllBitbucketRepositoryWatchers", e);
      return Collections.emptyList();
    } catch (ResponseException e) {
      log.error("Response exception inside getAllBitbucketRepositoryWatchers", e);
      return Collections.emptyList();
    } finally {
      jiraAuthenticationContext.setLoggedInUser(currentUser);
    }
  }

  public void sendMyteamNotifications(Stream<ApplicationUser> recipients, BitbucketEventDto event) {
    recipients
        .filter(Objects::nonNull)
        .filter(recipient -> recipient.isActive() && userData.isEnabled(recipient))
        .forEach(
            recipient -> {
              String myteamLogin = userData.getMrimLogin(recipient);
              if (myteamLogin == null) {
                log.error("Myteam login not found");
              }
              String message = messageFormatter.formatBitbucketEvent(recipient, event);
              if (message == null) {
                log.error("Bitbucket notification message is null");
              }
              myteamEventsListener.publishEvent(
                  new BitbucketNotifyEvent(myteamLogin, message, null));
            });
  }
}
