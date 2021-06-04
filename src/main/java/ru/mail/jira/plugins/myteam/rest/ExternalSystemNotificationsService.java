/* (C)2021 */
package ru.mail.jira.plugins.myteam.rest;

import com.atlassian.applinks.api.*;
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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import ru.mail.jira.plugins.myteam.bitbucket.BitbucketWebhookEvent;
import ru.mail.jira.plugins.myteam.bitbucket.dto.*;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.protocol.events.BitbucketNotifyEvent;
import ru.mail.jira.plugins.myteam.protocol.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.rest.dto.BitbucketRepoWatcherDto;
import ru.mail.jira.plugins.myteam.rest.dto.BitbucketWebhookResultDto;

@Controller
@Path("/external/notifications/")
public class ExternalSystemNotificationsService {
  private static final Logger log =
      LoggerFactory.getLogger(ExternalSystemNotificationsService.class);
  private static final String bitbucketRepoWatchersRestStr =
      "/rest/additional/1.0/watchers/repository/%s/%s";
  private static final String JIRA_ADMIN_USERNAME_FOR_APP_LINK = "jelly runner";
  // for localhost tests
  // private static final String JIRA_ADMIN_USERNAME_FOR_APP_LINK = "admin";

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
  @Produces({MediaType.APPLICATION_JSON})
  @AnonymousAllowed
  public BitbucketWebhookResultDto bitbucketProjectEventsWebHook(BitbucketEventDto event) {
    if (!(event instanceof BitbucketWebhookEvent)) {
      String errorInfo =
          String.format(
              "Bitbucket webhook event project name ad repo slug can't be parsed event = %s",
              event.toString());
      log.error(errorInfo);
      throw new BitbucketWebhookException(errorInfo);
    }
    BitbucketWebhookEvent bitbucketWebhookEvent = (BitbucketWebhookEvent) event;
    List<BitbucketRepoWatcherDto> allBitbucketRepositoryWatchers =
        getAllBitbucketRepositoryWatchers(
            bitbucketWebhookEvent.getProjectKey(), bitbucketWebhookEvent.getRepoSlug());
    sendMyteamNotifications(
        allBitbucketRepositoryWatchers.stream()
            .map(watcher -> userData.getUserByMrimLogin(watcher.getEmail())),
        event);
    if (allBitbucketRepositoryWatchers.size() == 0) {
      return new BitbucketWebhookResultDto("no watchers found");
    }
    return new BitbucketWebhookResultDto("success");
  }

  public List<BitbucketRepoWatcherDto> getAllBitbucketRepositoryWatchers(
      String projectKey, String repositorySlug) {
    Optional<ApplicationLink> bitbucketAppLinkOptional =
        StreamSupport.stream(applicationLinkService.getApplicationLinks().spliterator(), false)
            .filter(link -> link.getName().equals("Bitbucket"))
            .findFirst();

    if (!bitbucketAppLinkOptional.isPresent()) {
      log.error("Bitbucket application link not found in method getAllBitbucketRepositoryWatchers");
      return Collections.emptyList();
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
      return request.execute(
          new ApplicationLinkResponseHandler<List<BitbucketRepoWatcherDto>>() {
            @Override
            public List<BitbucketRepoWatcherDto> credentialsRequired(Response response)
                throws ResponseException {
              log.error(
                  "Bitbucket app-link credential required inside ResponseHandler called, bitbucket server response = {}",
                  response.getResponseBodyAsString());
              return Collections.emptyList();
            }

            @Override
            public List<BitbucketRepoWatcherDto> handle(Response response)
                throws ResponseException {
              try {
                return objectMapper.readValue(
                    response.getResponseBodyAsStream(),
                    new TypeReference<List<BitbucketRepoWatcherDto>>() {});
              } catch (IOException ioException) {
                log.error(
                    "IOException during ObjectMapper.readValue input entity string was: {}",
                    response.getResponseBodyAsString(),
                    ioException);
                return Collections.emptyList();
              }
            }
          });
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
                return;
              }
              String message = messageFormatter.formatBitbucketEvent(recipient, event);
              if (message == null) {
                log.error("Bitbucket notification message is null");
                return;
              }
              myteamEventsListener.publishEvent(
                  new BitbucketNotifyEvent(myteamLogin, message, null));
            });
  }
}
