/* (C)2021 */
package ru.mail.jira.plugins.myteam.rest;

import com.atlassian.applinks.api.*;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import java.util.Optional;
import java.util.stream.StreamSupport;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import ru.mail.jira.plugins.myteam.bitbucket.dto.BitbucketEventDto;

@Controller
@Path("/external/notifications/")
public class ExternalSystemNotificationsService {
  private static final Logger log = Logger.getLogger(ExternalSystemNotificationsService.class);
  private static final String bitbucketRepoWatchersRestStr =
      "/rest/additional/1.0/watchers/repository/%s/%s";

  private final ApplicationLinkService applicationLinkService;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final UserManager userManager;

  public ExternalSystemNotificationsService(
      @ComponentImport ApplicationLinkService applicationLinkService,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport UserManager userManager) {
    this.applicationLinkService = applicationLinkService;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.userManager = userManager;
  }

  @Path("/bitbucket")
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @AnonymousAllowed
  public Void bitbucketProjectEventsWebHook(BitbucketEventDto eventDto) {
    log.error("BITBUCKET JSON =" + eventDto.toString());
    Optional<ApplicationLink> bitbucketAppLinkOptional =
        StreamSupport.stream(applicationLinkService.getApplicationLinks().spliterator(), false)
            .filter(link -> link.getName().equals("Bitbucket"))
            .findFirst();
    jiraAuthenticationContext.setLoggedInUser(userManager.getUserByName("admin"));
    if (bitbucketAppLinkOptional.isPresent()) {
      ApplicationLink bitbucketAppLink = bitbucketAppLinkOptional.get();
      ApplicationLinkRequestFactory authenticatedRequestFactory =
          bitbucketAppLink.createImpersonatingAuthenticatedRequestFactory();

      try {
        ApplicationLinkRequest request =
            authenticatedRequestFactory.createRequest(
                Request.MethodType.GET,
                String.format(bitbucketRepoWatchersRestStr, "projectKey", "repositorySlug"));
        String responseBody =
            request.execute(
                new ApplicationLinkResponseHandler<String>() {
                  @Override
                  public String credentialsRequired(Response response) throws ResponseException {
                    log.error("CREDENTIALS REQUIRED CALLED!!!");
                    return response.getResponseBodyAsString();
                  }

                  @Override
                  public String handle(Response response) throws ResponseException {
                    return response.getResponseBodyAsString();
                  }
                });
        log.error("BITBUCKET RESPONSE BODY = " + responseBody);
      } catch (CredentialsRequiredException e) {
        log.error("ALARM!!!", e);
      } catch (ResponseException e) {
        log.error("ALARM 2 !!!", e);
      }
    }
    return null;
  }
}
