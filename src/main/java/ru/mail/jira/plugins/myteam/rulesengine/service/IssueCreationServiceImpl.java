/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.issue.search.constants.SystemSearchConstants;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.thread.JiraThreadLocalUtils;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.commons.IssueFieldsFilter;
import ru.mail.jira.plugins.myteam.configuration.createissue.customfields.CheckboxValueHandler;
import ru.mail.jira.plugins.myteam.configuration.createissue.customfields.CreateIssueFieldValueHandler;
import ru.mail.jira.plugins.myteam.configuration.createissue.customfields.DefaultFieldValueHandler;
import ru.mail.jira.plugins.myteam.configuration.createissue.customfields.PriorityValueHandler;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.IncorrectIssueTypeException;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.IssueCreationValidationException;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.UnsupportedCustomFieldsException;

@Service
public class IssueCreationServiceImpl implements IssueCreationService, InitializingBean {

  //  private final ConcurrentHashMap<String, ChatState> chatsStateMap;
  //  private final MyteamApiClient myteamApiClient;
  //  private final UserData userData;
  //  private final PluginData pluginData;
  //  private final MessageFormatter messageFormatter;
  private final I18nResolver i18nResolver;
  //  private final LocaleManager localeManager;
  //  private final PermissionManager permissionManager;
  //  private final ProjectManager projectManager;
  //  private final IssueTypeSchemeManager issueTypeSchemeManager;
  private final IssueTypeScreenSchemeManager issueTypeScreenSchemeManager;
  private final IssueTypeManager issueTypeManager;
  private final FieldLayoutManager fieldLayoutManager;
  private final FieldManager fieldManager;
  private final LocaleManager localeManager;
  //  private final ConstantsManager constantsManager;
  private final IssueService issueService;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final HashMap<String, CreateIssueFieldValueHandler> supportedIssueCreationCustomFields;
  private final CreateIssueFieldValueHandler defaultHandler;

  public IssueCreationServiceImpl(
      //                                        ChatStateMapping chatStateMapping,
      //                                        MyteamApiClient myteamApiClient,
      //                                        UserData userData,
      //                                        PluginData pluginData,
      //      MessageFormatter messageFormatter,
      @ComponentImport I18nResolver i18nResolver,
      //                                        @ComponentImport LocaleManager localeManager,
      //                                        @ComponentImport PermissionManager
      // permissionManager,
      //                                        @ComponentImport ProjectManager projectManager,
      //                                        @ComponentImport IssueTypeSchemeManager
      // issueTypeSchemeManager,
      @ComponentImport IssueTypeScreenSchemeManager issueTypeScreenSchemeManager,
      @ComponentImport IssueTypeManager issueTypeManager,
      @ComponentImport FieldLayoutManager fieldLayoutManager,
      @ComponentImport FieldManager fieldManager,
      //                                        @ComponentImport ConstantsManager constantsManager,
      @ComponentImport LocaleManager localeManager,
      @ComponentImport IssueService issueService,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext) {
    //    this.chatsStateMap = chatStateMapping.getChatsStateMap();
    //    this.myteamApiClient = myteamApiClient;
    //    this.userData = userData;
    //    this.pluginData = pluginData;
    //    this.messageFormatter = messageFormatter;
    this.i18nResolver = i18nResolver;
    //    this.localeManager = localeManager;
    //    this.permissionManager = permissionManager;
    //    this.projectManager = projectManager;
    //    this.issueTypeSchemeManager = issueTypeSchemeManager;
    this.issueTypeScreenSchemeManager = issueTypeScreenSchemeManager;
    this.issueTypeManager = issueTypeManager;
    this.fieldLayoutManager = fieldLayoutManager;
    this.fieldManager = fieldManager;
    this.localeManager = localeManager;
    //    this.constantsManager = constantsManager;
    this.issueService = issueService;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.supportedIssueCreationCustomFields = new HashMap<>();
    this.defaultHandler = new DefaultFieldValueHandler(i18nResolver);
  }

  @Override
  public void afterPropertiesSet() {
    CheckboxValueHandler checkbox = new CheckboxValueHandler(i18nResolver);
    supportedIssueCreationCustomFields.put(checkbox.getClassName(), checkbox);

    PriorityValueHandler priority = new PriorityValueHandler(i18nResolver);
    supportedIssueCreationCustomFields.put(priority.getClassName(), priority);
  }

  @Override
  public LinkedHashMap<Field, String> getIssueCreationFieldsValues(
      Project project,
      IssueType issueType,
      Set<String> includedFieldIds,
      Set<String> excludedFieldIds,
      IssueFieldsFilter issueFieldsFilter) {
    FieldLayout fieldLayout = fieldLayoutManager.getFieldLayout(project, issueType.getId());
    // getting (selectedProject, selectedIssueType, selectedIssueOperation) fields screen
    return issueTypeScreenSchemeManager.getIssueTypeScreenScheme(project)
        .getEffectiveFieldScreenScheme(issueType)
        .getFieldScreen(IssueOperations.CREATE_ISSUE_OPERATION).getTabs().stream()
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
      Project project, IssueType issueType, Map<Field, String> fields, ApplicationUser user) {
    //    Long projectId = issueCreationDto.getProjectId();

    // need here to because issueService use authenticationContext
    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
    try {
      jiraAuthenticationContext.setLoggedInUser(user);
      IssueInputParameters issueInputParameters =
          issueService.newIssueInputParameters(
              fields.entrySet().stream()
                  .collect(
                      Collectors.toMap(
                          (e) -> e.getKey().getId(),
                          (e) -> {
                            CreateIssueFieldValueHandler cfConfig =
                                getFieldValueHandler(e.getKey());
                            return cfConfig.getValueAsArray(
                                e.getValue(),
                                e.getKey(),
                                project,
                                localeManager.getLocaleFor(user));
                            //                            if
                            // (fieldManager.isCustomFieldId(e.getKey().getId())) {
                            //                              CustomField cf =
                            // fieldManager.getCustomField(e.getKey().getId());
                            //
                            //                              if (cf != null
                            //                                  &&
                            // supportedIssueCreationCustomFields.containsKey(
                            //                                  cf.getCustomFieldType().getClass()))
                            // {
                            //                                CreateIssueFieldValueHandler cfConfig
                            // =
                            //
                            // supportedIssueCreationCustomFields.get(
                            //
                            // cf.getCustomFieldType().getClass());
                            //
                            //                                return
                            // cfConfig.getValueAsArray(e.getValue(), cf);
                            //                              }
                            //                            }
                            //                            return
                            // messageFormatter.mapUserInputStringToFieldValue(
                            //                                projectId, e.getKey(), e.getValue());
                          })));
      issueInputParameters.setRetainExistingValuesWhenParameterNotProvided(true, true);

      // manually setting current user as issue reporter and selected ProjectId and IssueTypeId
      issueInputParameters.setProjectId(project.getId());
      issueInputParameters.setIssueTypeId(issueType.getId());
      issueInputParameters.setReporterId(user.getName());

      return issueService.validateCreate(user, issueInputParameters);
    } finally {
      jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
    }
  }

  @Override
  public CreateIssueFieldValueHandler getFieldValueHandler(Field field) {
    String className = getFieldClassName(field);
    if (supportedIssueCreationCustomFields.containsKey(className))
      return supportedIssueCreationCustomFields.get(className);
    return defaultHandler;
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
        getIssueCreationFieldsValues(
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
                    fieldManager.isCustomFieldId(field.getId()) && !isFieldSupported(field.getId()))
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
  public Issue createIssue(
      Project project, IssueType issueType, Map<Field, String> fields, ApplicationUser user)
      throws IssueCreationValidationException {
    JiraThreadLocalUtils.preCall();
    try {
      // then user filled all new issue fields which are required

      IssueService.CreateValidationResult issueValidationResult =
          validateIssueWithGivenFields(project, issueType, fields, user);
      //      myteamApiClient.answerCallbackQuery(event.getQueryId());
      if (issueValidationResult.isValid()) {
        return issueService.create(user, issueValidationResult).getIssue();
        //        String createdIssueLink = messageFormatter.createIssueLink(createdIssue.getKey());
        //        myteamApiClient.sendMessageText(
        //            chatId,
        //            i18nResolver.getText(
        //                locale,
        //                "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.issueCreated",
        //                createdIssueLink));
      } else {
        throw new IssueCreationValidationException(
            "Unable to create issue with provided fields",
            issueValidationResult.getErrorCollection());
        //        myteamApiClient.sendMessageText(
        //            chatId,
        //            messageFormatter.shieldText(
        //                String.join(
        //                    "\n",
        //                    i18nResolver.getText(
        //                        locale,
        //
        // "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.validationError"),
        //                    messageFormatter.stringifyMap(
        //                        issueValidationResult.getErrorCollection().getErrors()),
        //                    messageFormatter.stringifyCollection(
        //                        locale,
        // issueValidationResult.getErrorCollection().getErrorMessages()))));
      }
    } finally {
      JiraThreadLocalUtils.postCall();
    }
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
    // if any of configuration scheme of
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
