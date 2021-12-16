/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.exception.ParseException;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.search.constants.SystemSearchConstants;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.thread.JiraThreadLocalUtils;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.Query;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.naming.NoPermissionException;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.commons.IssueFieldsFilter;
import ru.mail.jira.plugins.myteam.model.PluginData;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.rulesengine.core.Utils;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.IncorrectIssueTypeException;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.IssueWatchingException;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.ProjectBannedException;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.UnsupportedCustomFieldsException;

@Service
public class IssueServiceImpl implements IssueService {

  private final IssueManager issueManager;
  private final PermissionManager permissionManager;
  private final WatcherManager watcherManager;
  private final SearchService searchService;
  private final CommentManager commentManager;
  private final ProjectManager projectManager;
  private final IssueTypeSchemeManager issueTypeSchemeManager;
  private final IssueTypeManager issueTypeManager;
  private final FieldManager fieldManager;
  //  private final com.atlassian.jira.bc.issue.IssueService issueService;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final Utils utils;
  private final IssueCreationFieldsService issueCreationFieldsService;
  private final PluginData pluginData;
  private final String JIRA_BASE_URL;

  public IssueServiceImpl(
      Utils utils,
      @ComponentImport IssueManager issueManager,
      @ComponentImport PermissionManager permissionManager,
      @ComponentImport WatcherManager watcherManager,
      @ComponentImport SearchService searchService,
      @ComponentImport CommentManager commentManager,
      @ComponentImport ProjectManager projectManager,
      @ComponentImport IssueTypeSchemeManager issueTypeSchemeManager,
      @ComponentImport IssueTypeManager issueTypeManager,
      @ComponentImport FieldManager fieldManager,
      //      @ComponentImport com.atlassian.jira.bc.issue.IssueService issueService,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport ApplicationProperties applicationProperties,
      IssueCreationFieldsService issueCreationFieldsService,
      PluginData pluginData) {
    this.issueManager = issueManager;
    this.permissionManager = permissionManager;
    this.watcherManager = watcherManager;
    this.searchService = searchService;
    this.commentManager = commentManager;
    this.projectManager = projectManager;
    this.issueTypeSchemeManager = issueTypeSchemeManager;
    this.issueTypeManager = issueTypeManager;
    this.fieldManager = fieldManager;
    //    this.issueService = issueService;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.utils = utils;
    this.issueCreationFieldsService = issueCreationFieldsService;
    this.pluginData = pluginData;
    this.JIRA_BASE_URL = applicationProperties.getString(APKeys.JIRA_BASEURL);
  }

  @Override
  public Issue getIssueByUser(String issueKey, ApplicationUser user) {
    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
    try {
      jiraAuthenticationContext.setLoggedInUser(user);
      Issue issue = issueManager.getIssueByKeyIgnoreCase(issueKey);
      if (issue != null) {
        if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user)) {
          return issue;
        } else
          throw new IssuePermissionException(
              String.format("User has no permissions to view issue %s", issueKey));
      } else throw new IssueNotFoundException(String.format("Issue %s not found", issueKey));
    } finally {
      jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
    }
  }

  @Override
  public boolean isUserWatching(Issue issue, ApplicationUser user) {
    return watcherManager.isWatching(user, issue);
  }

  @Override
  public String getJiraBaseUrl() {
    return JIRA_BASE_URL;
  }

  @Override
  public SearchResults<Issue> SearchByJql(String jql, ApplicationUser user, int page, int pageSize)
      throws SearchException, ParseException {
    JiraThreadLocalUtils.preCall();

    SearchService.ParseResult parseResult = searchService.parseQuery(user, jql);
    if (parseResult.isValid()) {

      Query jqlQuery = parseResult.getQuery();
      Query sanitizedJql = searchService.sanitiseSearchQuery(user, jqlQuery);
      PagerFilter<Issue> pagerFilter = new PagerFilter<>(page * pageSize, pageSize);
      SearchResults<Issue> res = searchService.search(user, sanitizedJql, pagerFilter);

      JiraThreadLocalUtils.postCall();
      return res;
    } else {
      JiraThreadLocalUtils.postCall();
      throw new ParseException("Incorrect jql expression");
    }
  }

  @Override
  public void watchIssue(String issueKey, ApplicationUser user)
      throws IssuePermissionException, IssueNotFoundException, IssueWatchingException {
    Issue issue = getIssueByUser(issueKey, user);
    if (watcherManager.isWatching(user, issue)) {
      throw new IssueWatchingException(
          String.format("Issue with key %s already watched", issueKey));
    } else {
      watcherManager.startWatching(user, issue);
    }
  }

  @Override
  public void unwatchIssue(String issueKey, ApplicationUser user)
      throws IssuePermissionException, IssueNotFoundException, IssueWatchingException {
    Issue issue = getIssueByUser(issueKey, user);
    if (!watcherManager.isWatching(user, issue)) {
      throw new IssueWatchingException(
          String.format("Issue with key %s already unwatched", issueKey));
    } else {
      watcherManager.stopWatching(user, issue);
    }
  }

  @Override
  public void commentIssue(String issueKey, ApplicationUser user, ChatMessageEvent event)
      throws NoPermissionException {
    JiraThreadLocalUtils.preCall();
    try {
      Issue commentedIssue = issueManager.getIssueByCurrentKey(issueKey);
      if (user != null && commentedIssue != null) {
        if (permissionManager.hasPermission(
            ProjectPermissions.ADD_COMMENTS, commentedIssue, user)) {
          commentManager.create(
              commentedIssue,
              user,
              utils.convertToJiraCommentStyle(event, user, commentedIssue),
              true);
        } else {
          throw new NoPermissionException();
        }
      }
    } finally {
      JiraThreadLocalUtils.postCall();
    }
  }

  @Override
  public List<Project> getAllowedProjects() {
    return projectManager.getProjects().stream()
        .filter(proj -> !isProjectExcluded(proj.getId()))
        .collect(Collectors.toList());
  }

  @Override
  public Project getProject(String projectKey, ApplicationUser user)
      throws PermissionException, ProjectBannedException {
    Project selectedProject = projectManager.getProjectByCurrentKeyIgnoreCase(projectKey);
    if (selectedProject == null) {
      return null;
    }
    if (isProjectExcluded(selectedProject.getId())) {
      throw new ProjectBannedException(String.format("Project with key %s is banned", projectKey));
    }

    if (!permissionManager.hasPermission(ProjectPermissions.CREATE_ISSUES, selectedProject, user)) {
      throw new PermissionException();
    }
    return selectedProject;
  }

  @Override
  public Collection<IssueType> getProjectIssueTypes(Project project, ApplicationUser user) {
    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
    try {
      jiraAuthenticationContext.setLoggedInUser(user);
      return issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(project);
    } finally {
      jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
    }
  }

  @Override
  public List<Field> getIssueFields(Project project, ApplicationUser user, String issueTypeId)
      throws UnsupportedCustomFieldsException, IncorrectIssueTypeException {

    Optional<IssueType> maybeCorrectIssueType =
        Optional.ofNullable(issueTypeManager.getIssueType(issueTypeId));
    if (!maybeCorrectIssueType.isPresent()) {
      throw new IncorrectIssueTypeException(
          String.format(
              "inserted issue type isn't correct for project with key %s", project.getKey()));
      // inserted issue type position isn't correct
      //      myteamApiClient.answerCallbackQuery(queryId);
      //      myteamApiClient.sendMessageText(
      //          chatId,
      //          i18nResolver.getRawText(
      //              locale,
      //
      // "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectedIssueTypeNotValid"));
    }
    IssueType issueType = maybeCorrectIssueType.get();

    // description and priority fields must be included no matter required it or not
    Set<String> includedFieldIds =
        Stream.of(
                fieldManager.getField(IssueFieldConstants.DESCRIPTION).getId(),
                fieldManager.getField(IssueFieldConstants.PRIORITY).getId())
            .collect(Collectors.toSet());
    // project and issueType fields should be excluded no matter required them or not
    Set<String> excludedFieldIds =
        Stream.of(fieldManager.getProjectField().getId(), fieldManager.getIssueTypeField().getId())
            .collect(Collectors.toSet());
    LinkedHashMap<Field, String> issueCreationRequiredFieldsValues =
        issueCreationFieldsService.getIssueCreationFieldsValues(
            project, issueType, includedFieldIds, excludedFieldIds, IssueFieldsFilter.REQUIRED);

    // We don't need allow user to fill reporter field
    issueCreationRequiredFieldsValues.remove(
        fieldManager.getOrderableField(SystemSearchConstants.forReporter().getFieldId()));

    // setting selected IssueType and mapped requiredIssueCreationFields
    //    currentIssueCreationDto.setIssueTypeId(selectedIssueType.getId());
    //
    // currentIssueCreationDto.setRequiredIssueCreationFieldValues(issueCreationRequiredFieldsValues);

    // order saved here because LinkedHashMap keySet() method  in reality returns LinkedHashSet
    List<Field> requiredFields = new ArrayList<>(issueCreationRequiredFieldsValues.keySet());

    List<Field> requiredCustomFieldsInScope =
        requiredFields.stream()
            .filter(
                field ->
                    fieldManager.isCustomFieldId(field.getId())
                        && !issueCreationFieldsService.isFieldSupported(field.getId()))
            .collect(Collectors.toList());

    if (!requiredCustomFieldsInScope.isEmpty()) {

      throw new UnsupportedCustomFieldsException(
          String.format(
              "Issue type %s in project with key %s has unsupported required custom field",
              issueType.getNameTranslation(), project.getKey()),
          requiredCustomFieldsInScope);
      // send user message that we can't create issue with required customFields
      //      myteamApiClient.answerCallbackQuery(queryId);
      //      myteamApiClient.sendMessageText(
      //          chatId,
      //          String.join(
      //              "\n",
      //              i18nResolver.getRawText(
      //                  locale,
      //
      // "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.requiredCFError"),
      //              messageFormatter.stringifyFieldsCollection(locale,
      // requiredCustomFieldsInScope)));
      //      return requiredFields;
    }

    //    if (!requiredFields.isEmpty()) {
    //      return requiredFields;
    //      // here sending new issue filling fields message to user
    //      //      Field currentField = requiredFields.get(0);
    //      //      myteamApiClient.answerCallbackQuery(queryId);
    //      //      myteamApiClient.sendMessageText(
    //      //          chatId,
    //      //          messageFormatter.createInsertFieldMessage(locale, currentField,
    //      // currentIssueCreationDto),
    //      //          messageFormatter.buildButtonsWithCancel(
    //      //              null,
    //      //              i18nResolver.getRawText(
    //      //                  locale,
    //      //
    //      // "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text")));
    //    } else {
    //      // well, all required issue fields are filled, then just create issue
    //      com.atlassian.jira.bc.issue.IssueService.CreateValidationResult issueValidationResult =
    //          issueCreationFieldsService.validateIssueWithGivenFields(
    //              user,
    //              IssueCreationDto.builder()
    //                  .issueTypeId(issueTypeId)
    //                  .projectId(project.getId())
    //                  .requiredIssueCreationFieldValues(issueCreationRequiredFieldsValues)
    //                  .build());
    //      if (issueValidationResult.isValid()) {
    //        JiraThreadLocalUtils.preCall();
    //        try {
    //          issueService.create(user, issueValidationResult);
    //          //          myteamApiClient.answerCallbackQuery(queryId);
    //          //          myteamApiClient.sendMessageText(chatId, "Congratulations, issue was
    // created
    //          // =)");
    //        } finally {
    //          JiraThreadLocalUtils.postCall();
    //        }
    //      } else {
    //        //        myteamApiClient.answerCallbackQuery(queryId);
    //        //        myteamApiClient.sendMessageText(
    //        //            chatId,
    //        //            String.join(
    //        //                "\n",
    //        //                i18nResolver.getText(
    //        //                    locale,
    //        //
    //        // "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.validationError"),
    //        //                messageFormatter.stringifyMap(
    //        //                    issueValidationResult.getErrorCollection().getErrors()),
    //        //                messageFormatter.stringifyCollection(
    //        //                    locale,
    //        // issueValidationResult.getErrorCollection().getErrorMessages())));
    //      }
    //    }
    return requiredFields;
  }

  @Override
  public IssueType getIssueType(String id) {
    return issueTypeManager.getIssueType(id);
  }

  @Override
  public Issue getIssue(String issueKey) throws IssueNotFoundException {
    Issue issue = issueManager.getIssueByKeyIgnoreCase(issueKey);
    if (issue == null) {
      throw new IssueNotFoundException(String.format("Issue with key %s was not found", issueKey));
    }
    return issue;
  }

  @Override
  public List<Comment> getIssueComments(String issueKey, ApplicationUser user)
      throws IssuePermissionException, IssueNotFoundException {
    JiraThreadLocalUtils.preCall();
    try {
      return commentManager.getComments(getIssueByUser(issueKey, user));
    } finally {
      JiraThreadLocalUtils.postCall();
    }
  }

  private boolean isProjectExcluded(Long projectId) {
    return pluginData.getExcludingProjectIds().contains(projectId);
  }
}
