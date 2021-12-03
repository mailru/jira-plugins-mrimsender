/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.commands;

// import com.atlassian.jira.issue.Issue;
// import com.atlassian.jira.permission.ProjectPermissions;
// import com.atlassian.jira.user.ApplicationUser;
// import kong.unirest.HttpResponse;
// import kong.unirest.UnirestException;
// import org.apache.commons.lang3.StringUtils;
// import org.jeasy.rules.annotation.Action;
// import org.jeasy.rules.annotation.Condition;
// import org.jeasy.rules.annotation.Fact;
// import org.jeasy.rules.annotation.Rule;
// import ru.mail.jira.plugins.myteam.commons.Utils;
// import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
// import ru.mail.jira.plugins.myteam.myteam.dto.MessageResponse;
// import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;
// import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
// import ru.mail.jira.plugins.myteam.rulesengine.RuleEventType;
// import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
//
// import java.io.IOException;
// import java.net.URL;
// import java.util.List;
// import java.util.Locale;
// import java.util.stream.Collectors;
//
// import static com.atlassian.core.task.longrunning.AbstractLongRunningTask.log;
//
// @Rule(name = "hello message rule", description = "shows hello message")
// public class DefaultMessageRule extends MenuCommandRule {
//
//  public DefaultMessageRule(UserChatService userChatService) {
//    super(userChatService);
//  }
//
//  static final RuleEventType EVENT_TYPE = RuleEventType.DefaultMessage;
//
//  @Condition
//  public boolean isValid(@Fact("command") RuleEventType event) {
//    return event.equals(EVENT_TYPE);
//  }
//
//  @Action
//  public void execute(@Fact("event") ChatMessageEvent event)
//      throws MyteamServerErrorException, IOException {
////    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
////    if (user != null) {
////      Locale locale = userChatService.getUserLocale(user);
////      String chatId = event.getChatId();
////
////      List<Forward> forwards = getForwardList(event);
////      if (forwards != null) {
////        Forward forward = forwards.get(0);
////        String forwardMessageText = forward.getMessage().getText();
////        URL issueUrl = Utils.tryFindUrlByPrefixInStr(forwardMessageText,
// userChatService.getJiraBaseUrl());
////        if (issueUrl != null) {
////          String issueKey = StringUtils.substringAfterLast(issueUrl.getPath(), "/");
////          this.sendIssueViewToUser(issueKey, user, chatId);
////          log.debug("ShowDefaultMessageEvent handling finished");
////          return;
////        }
////      }
////      URL issueUrl =
////          Utils.tryFindUrlByPrefixInStr(event.getMessage(), JIRA_BASE_URL);
////      if (issueUrl != null) {
////        String issueKey = StringUtils.substringAfterLast(issueUrl.getPath(), "/");
////        this.sendIssueViewToUser(issueKey, user, chatId);
////        log.debug("ShowDefaultMessageEvent handling finished");
////        return;
////      }
////      myteamApiClient.sendMessageText(
////          event.getChatId(),
////          i18nResolver.getRawText(
////              locale, "ru.mail.jira.plugins.myteam.myteamEventsListener.defaultMessage.text"),
////          messageFormatter.getMenuButtons(user));
////
////    }
//  }
//
//  private List<Forward> getForwardList(ChatMessageEvent event) {
//    return event.isHasForwards()
//        ? event.getMessageParts().stream()
//        .filter(part -> part instanceof Forward)
//        .map(part -> (Forward) part)
//        .collect(Collectors.toList())
//        : null;
//  }
//
//
////  public void sendIssueViewToUser(String issueKey, ApplicationUser user, String chatId)
////      throws IOException, UnirestException, MyteamServerErrorException {
////    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
////    try {
////      jiraAuthenticationContext.setLoggedInUser(user);
////      Issue issueToShow = issueManager.getIssueByKeyIgnoreCase(issueKey);
////      if (issueToShow != null) {
////        if (permissionManager.hasPermission(
////            ProjectPermissions.BROWSE_PROJECTS, issueToShow, user)) {
////          HttpResponse<MessageResponse> response =
////              myteamApiClient.sendMessageText(
////                  chatId,
////                  messageFormatter.createIssueSummary(issueToShow, user),
////                  messageFormatter.getIssueButtons(
////                      issueToShow.getKey(), user, watcherManager.isWatching(user,
// issueToShow)));
////          if (response.getStatus() != 200
////              || (response.getBody() != null && !response.getBody().isOk())) {
////            log.warn(
////                "sendIssueViewToUser({}, {}, {}). Text={} Response={}",
////                issueKey,
////                user,
////                chatId,
////                messageFormatter.createIssueSummary(issueToShow, user),
////                response.getBody().toString());
////          }
////        } else {
////          myteamApiClient.sendMessageText(
////              chatId,
////              i18nResolver.getRawText(
////                  localeManager.getLocaleFor(user),
////
// "ru.mail.jira.plugins.myteam.messageQueueProcessor.quickViewButton.noPermissions"));
////        }
////      } else {
////        myteamApiClient.sendMessageText(
////            chatId,
////            i18nResolver.getRawText(
////                localeManager.getLocaleFor(user),
////
// "ru.mail.jira.plugins.myteam.myteamEventsListener.newIssueKeyMessage.error.issueNotFound"));
////      }
////    } catch (Exception e) {
////      log.error("sendIssueViewToUser({}, {}, {})", issueKey, user, chatId, e);
////    } finally {
////      jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
////    }
////  }
//
////  private void GetIssueByUser() {
////    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
////    try {
////      jiraAuthenticationContext.setLoggedInUser(user);
////      Issue issueToShow = issueManager.getIssueByKeyIgnoreCase(issueKey);
////      if (issueToShow != null) {
////        if (permissionManager.hasPermission(
////            ProjectPermissions.BROWSE_PROJECTS, issueToShow, user)) {
////          HttpResponse<MessageResponse> response =
////              myteamApiClient.sendMessageText(
////                  chatId,
////                  messageFormatter.createIssueSummary(issueToShow, user),
////                  messageFormatter.getIssueButtons(
////                      issueToShow.getKey(), user, watcherManager.isWatching(user,
// issueToShow)));
////          if (response.getStatus() != 200
////              || (response.getBody() != null && !response.getBody().isOk())) {
////            log.warn(
////                "sendIssueViewToUser({}, {}, {}). Text={} Response={}",
////                issueKey,
////                user,
////                chatId,
////                messageFormatter.createIssueSummary(issueToShow, user),
////                response.getBody().toString());
////          }
////        } else {
////          myteamApiClient.sendMessageText(
////              chatId,
////              i18nResolver.getRawText(
////                  localeManager.getLocaleFor(user),
////
// "ru.mail.jira.plugins.myteam.messageQueueProcessor.quickViewButton.noPermissions"));
////        }
////      } else {
////
////      }
////    } catch (Exception e) {
////      log.error("sendIssueViewToUser({}, {}, {})", issueKey, user, chatId, e);
////    } finally {
////      jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
////    }
////  }
// }
//
////  HttpResponse<MessageResponse> response =
////      myteamApiClient.sendMessageText(
////          chatId,
////          messageFormatter.createIssueSummary(issueToShow, user),
////          messageFormatter.getIssueButtons(
////              issueToShow.getKey(), user, watcherManager.isWatching(user, issueToShow)));
////          if (response.getStatus() != 200
////              || (response.getBody() != null && !response.getBody().isOk())) {
////              log.warn(
////              "sendIssueViewToUser({}, {}, {}). Text={} Response={}",
////              issueKey,
////              user,
////              chatId,
////              messageFormatter.createIssueSummary(issueToShow, user),
////              response.getBody().toString());
////              }
