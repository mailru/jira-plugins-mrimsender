/* (C)2021 */
package ru.mail.jira.plugins.myteam.service.impl;

import static ru.mail.jira.plugins.myteam.commons.Utils.removeAllEmojis;

import com.atlassian.greenhopper.api.customfield.ManagedCustomFieldsService;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.link.RemoteIssueLinkService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.*;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.config.manager.PrioritySchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.issue.search.constants.SystemSearchConstants;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.thread.JiraThreadLocalUtils;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.bot.configuration.createissue.customfields.*;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.IncorrectIssueTypeException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.IssueCreationValidationException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.ProjectBannedException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.UnsupportedCustomFieldsException;
import ru.mail.jira.plugins.myteam.commons.IssueFieldsFilter;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.component.url.dto.LinksInMessage;
import ru.mail.jira.plugins.myteam.service.IssueCreationService;

@Service
@ExportAsService(LifecycleAware.class)
public class IssueCreationServiceImpl implements IssueCreationService, LifecycleAware {

  private final I18nResolver i18nResolver;
  private final IssueTypeScreenSchemeManager issueTypeScreenSchemeManager;
  private final IssueTypeManager issueTypeManager;
  private final FieldLayoutManager fieldLayoutManager;
  private final FieldManager fieldManager;
  private final IssueManager issueManager;
  private final IssueService issueService;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final OptionsManager optionsManager;
  private final PrioritySchemeManager prioritySchemeManager;
  private final SearchService searchService;
  private final RemoteIssueLinkService remoteIssueLinkService;
  private final ManagedCustomFieldsService managedCustomFieldsService;
  private final UserData userData;
  private final HashMap<String, CreateIssueFieldValueHandler> supportedIssueCreationCustomFields;
  private final CreateIssueFieldValueHandler defaultHandler;
  private final MessageFormatter messageFormatter;
  private final ru.mail.jira.plugins.myteam.service.IssueService myteamIssueService;

  public IssueCreationServiceImpl(
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport IssueTypeScreenSchemeManager issueTypeScreenSchemeManager,
      @ComponentImport IssueTypeManager issueTypeManager,
      @ComponentImport FieldLayoutManager fieldLayoutManager,
      @ComponentImport FieldManager fieldManager,
      @ComponentImport IssueManager issueManager,
      @ComponentImport IssueService issueService,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport OptionsManager optionsManager,
      @ComponentImport PrioritySchemeManager prioritySchemeManager,
      @ComponentImport SearchService searchService,
      @ComponentImport RemoteIssueLinkService remoteIssueLinkService,
      @ComponentImport ManagedCustomFieldsService managedCustomFieldsService,
      UserData userData,
      MessageFormatter messageFormatter,
      ru.mail.jira.plugins.myteam.service.IssueService myteamIssueService) {
    this.i18nResolver = i18nResolver;
    this.issueTypeScreenSchemeManager = issueTypeScreenSchemeManager;
    this.issueTypeManager = issueTypeManager;
    this.fieldLayoutManager = fieldLayoutManager;
    this.fieldManager = fieldManager;
    this.issueManager = issueManager;
    this.issueService = issueService;
    this.searchService = searchService;
    this.remoteIssueLinkService = remoteIssueLinkService;
    this.managedCustomFieldsService = managedCustomFieldsService;
    this.userData = userData;
    this.messageFormatter = messageFormatter;
    this.myteamIssueService = myteamIssueService;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.optionsManager = optionsManager;
    this.prioritySchemeManager = prioritySchemeManager;
    this.supportedIssueCreationCustomFields = new HashMap<>();
    this.defaultHandler = new DefaultFieldValueHandler(i18nResolver);
  }

  @Override
  public void onStart() {
    CheckboxValueHandler checkbox = new CheckboxValueHandler(optionsManager, i18nResolver);
    supportedIssueCreationCustomFields.put(checkbox.getClassName(), checkbox);

    PriorityValueHandler priority = new PriorityValueHandler(i18nResolver, prioritySchemeManager);
    supportedIssueCreationCustomFields.put(priority.getClassName(), priority);

    EpicLinkValueHandler epicLink =
        new EpicLinkValueHandler(
            searchService, i18nResolver, messageFormatter, managedCustomFieldsService);
    supportedIssueCreationCustomFields.put(epicLink.getClassName(), epicLink);

    AssigneeValueHandler assignee = new AssigneeValueHandler(userData, i18nResolver);
    supportedIssueCreationCustomFields.put(assignee.getClassName(), assignee);
  }

  @Override
  public void onStop() {}

  @Override
  public LinkedHashMap<Field, String> getIssueCreationFieldsValues(
      Project project,
      IssueType issueType,
      Set<String> includedFieldIds,
      Set<String> excludedFieldIds,
      IssueFieldsFilter issueFieldsFilter) {
    FieldLayout fieldLayout = fieldLayoutManager.getFieldLayout(project, issueType.getId());
    // getting (selectedProject, selectedIssueType, selectedIssueOperation) fields screen
    return issueTypeScreenSchemeManager
        .getIssueTypeScreenScheme(project)
        .getEffectiveFieldScreenScheme(issueType)
        .getFieldScreen(IssueOperations.CREATE_ISSUE_OPERATION)
        .getTabs()
        .stream()
        .flatMap(
            tab ->
                tab.getFieldScreenLayoutItems().stream()
                    .filter(
                        fieldScreenLayoutItem -> {
                          String fieldId = fieldScreenLayoutItem.getFieldId();
                          FieldLayoutItem fieldLayoutItem = fieldLayout.getFieldLayoutItem(fieldId);
                          if (fieldLayoutItem == null) return false;
                          boolean isRequired = fieldLayoutItem.isRequired();
                          boolean filterResult = false;
                          switch (issueFieldsFilter) {
                            case REQUIRED:
                              filterResult = isRequired;
                              break;
                            case NON_REQUIRED:
                              filterResult = !isRequired;
                              break;
                          }
                          return filterResult
                              || (includedFieldIds != null && includedFieldIds.contains(fieldId));
                        })
                    .filter(
                        layoutItem ->
                            excludedFieldIds == null
                                || !excludedFieldIds.contains(layoutItem.getFieldId()))
                    .filter(
                        fieldScreenLayoutItem -> {
                          // all custom fields must be in project and issue type context scope
                          if (fieldManager.isCustomFieldId(fieldScreenLayoutItem.getFieldId())) {
                            CustomField cf =
                                (CustomField) fieldScreenLayoutItem.getOrderableField();
                            return isCFInScopeOfProjectAndIssueType(
                                cf, project.getId(), issueType.getId());
                          }
                          // all fields which is not custom fields, are not filtered
                          return true;
                        })
                    .map(FieldScreenLayoutItem::getOrderableField))
        .collect(
            Collectors.toMap(
                Function.identity(), (field) -> "", (v1, v2) -> v1, LinkedHashMap::new));
  }

  private IssueService.CreateValidationResult validateIssueWithGivenFields(
      Project project,
      IssueType issueType,
      Map<Field, String> fields,
      @Nullable ApplicationUser user) {

    IssueInputParameters issueInputParameters =
        issueService.newIssueInputParameters(
            fields.entrySet().stream()
                .collect(
                    Collectors.toMap(
                        (e) -> e.getKey().getId(),
                        (e) -> {
                          CreateIssueFieldValueHandler cfConfig = getFieldValueHandler(e.getKey());
                          return cfConfig.getValueAsArray(
                              removeAllEmojis(e.getValue()), e.getKey(), project, issueType);
                        })));
    issueInputParameters.setRetainExistingValuesWhenParameterNotProvided(true, true);

    // manually setting current user as issue reporter and selected ProjectId and IssueTypeId
    issueInputParameters.setProjectId(project.getId());
    issueInputParameters.setIssueTypeId(issueType.getId());
    issueInputParameters.setReporterId(user != null ? user.getName() : null);

    return issueService.validateCreate(user, issueInputParameters);
  }

  @Override
  public CreateIssueFieldValueHandler getFieldValueHandler(Field field) {
    String className = getFieldClassName(field);
    if (supportedIssueCreationCustomFields.containsKey(className))
      return supportedIssueCreationCustomFields.get(className);
    return defaultHandler;
  }

  @Override
  public List<Field> getIssueFields(
      Project project, @Nullable ApplicationUser user, String issueTypeId)
      throws UnsupportedCustomFieldsException, IncorrectIssueTypeException {

    Optional<IssueType> maybeCorrectIssueType =
        Optional.ofNullable(issueTypeManager.getIssueType(issueTypeId));

    if (!maybeCorrectIssueType.isPresent()) {
      throw new IncorrectIssueTypeException(
          String.format(
              "inserted issue type isn't correct for project with key %s", project.getKey()));
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
        getIssueCreationFieldsValues(
            project, issueType, includedFieldIds, excludedFieldIds, IssueFieldsFilter.REQUIRED);

    // We don't need allow user to fill reporter field
    issueCreationRequiredFieldsValues.remove(
        fieldManager.getOrderableField(SystemSearchConstants.forReporter().getFieldId()));

    // order saved here because LinkedHashMap keySet() method  in reality returns LinkedHashSet
    List<Field> requiredFields = new ArrayList<>(issueCreationRequiredFieldsValues.keySet());

    List<Field> requiredCustomFieldsInScope =
        requiredFields.stream()
            .filter(
                field ->
                    fieldManager.isCustomFieldId(field.getId()) && !isFieldSupported(field.getId()))
            .collect(Collectors.toList());

    if (!requiredCustomFieldsInScope.isEmpty()) {

      throw new UnsupportedCustomFieldsException(
          String.format(
              "Issue type %s in project with key %s has unsupported required custom field",
              issueType.getNameTranslation(), project.getKey()),
          requiredCustomFieldsInScope);
    }

    return requiredFields;
  }

  @Override
  public MutableIssue createIssue(
      Project project,
      IssueType issueType,
      Map<Field, String> fields,
      @Nullable ApplicationUser user)
      throws IssueCreationValidationException {
    JiraThreadLocalUtils.preCall();
    // need here to because issueService use authenticationContext
    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
    try {
      jiraAuthenticationContext.setLoggedInUser(user);
      IssueService.CreateValidationResult issueValidationResult =
          validateIssueWithGivenFields(project, issueType, fields, user);
      if (issueValidationResult.isValid()) {
        IssueService.IssueResult issueResult = issueService.create(user, issueValidationResult);
        if (issueResult.isValid()) {
          return issueResult.getIssue();
        } else {
          throw new IssueCreationValidationException(
              "Unable to create issue with provided fields", issueResult.getErrorCollection());
        }
      } else {
        throw new IssueCreationValidationException(
            "Unable to create issue with provided fields",
            issueValidationResult.getErrorCollection());
      }
    } finally {
      jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
      JiraThreadLocalUtils.postCall();
    }
  }

  @Override
  public MutableIssue createIssue(
      String projectKey, String issueTypeId, Map<Field, String> fields, ApplicationUser user)
      throws IssueCreationValidationException, PermissionException, ProjectBannedException {
    Project project = myteamIssueService.getProject(projectKey, user);
    IssueType issueType = myteamIssueService.getIssueType(issueTypeId);

    return createIssue(project, issueType, fields, user);
  }

  @Override
  public void addLinksToIssueFromMessage(
      final Issue issue, final List<LinksInMessage> linksInMessages, final ApplicationUser user) {
    linksInMessages.stream()
        .flatMap(linksInMessage -> linksInMessage.getLinks().stream())
        .distinct()
        .forEach(
            link ->
                createRemoteIssueLink(
                    new RemoteIssueLinkBuilder()
                        .url(link.getLink())
                        .title(link.isMasked() ? link.getMask() : link.getLink())
                        .issueId(issue.getId()),
                    user));
  }

  @Override
  public void addIssueChatLink(
      Issue issue, @Nullable String title, @Nullable String link, ApplicationUser user) {
    createRemoteIssueLink(
        new RemoteIssueLinkBuilder()
            .url(link)
            .title(
                i18nResolver.getText(
                    "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.createdInChat",
                    removeAllEmojis(title)))
            .issueId(issue.getId()),
        user);
  }

  private void createRemoteIssueLink(
      final RemoteIssueLinkBuilder remoteIssueLinkBuilder, final ApplicationUser user) {
    RemoteIssueLinkService.CreateValidationResult createValidationResult =
        remoteIssueLinkService.validateCreate(user, remoteIssueLinkBuilder.build());

    if (createValidationResult.isValid()) {
      remoteIssueLinkService.create(user, createValidationResult);
    }
  }

  @Override
  public Issue updateIssueDescription(
      String description, MutableIssue issue, ApplicationUser user) {
    issue.setDescription(removeAllEmojis(description));
    return issueManager.updateIssue(user, issue, EventDispatchOption.DO_NOT_DISPATCH, false);
  }

  @Override
  public boolean isFieldSupported(String fieldId) {
    CustomField field = fieldManager.getCustomField(fieldId);
    return field != null && !getFieldValueHandler(field).equals(defaultHandler);
  }

  @Override
  public Field getField(String fieldId) {
    return fieldManager.getField(fieldId);
  }

  private String getFieldClassName(Field field) {
    String className = field.getClass().getName();
    if (fieldManager.isCustomFieldId(field.getId())) {
      CustomField cf = fieldManager.getCustomField(field.getId());
      if (cf != null) className = cf.getCustomFieldType().getClass().getName();
    }
    return className;
  }

  private boolean isCFInScopeOfProjectAndIssueType(
      CustomField customField, Long projectId, String issueTypeId) {
    return customField.getConfigurationSchemes().stream()
        .anyMatch(
            scheme -> {
              if (scheme.isAllProjects() && scheme.isAllIssueTypes()) return true;
              return (scheme.isAllProjects()
                      || scheme.getAssociatedProjectIds().contains(projectId))
                  && (scheme.isAllIssueTypes()
                      || scheme.getAssociatedIssueTypeIds().contains(issueTypeId));
            });
  }
}
