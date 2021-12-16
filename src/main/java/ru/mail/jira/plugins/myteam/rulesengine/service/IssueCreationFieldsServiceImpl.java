/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.customfields.impl.AbstractCustomFieldType;
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
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.commons.IssueFieldsFilter;
import ru.mail.jira.plugins.myteam.configuration.createissue.customfields.Checkbox;
import ru.mail.jira.plugins.myteam.configuration.createissue.customfields.CreateIssueBaseCF;
import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;

@Service
public class IssueCreationFieldsServiceImpl
    implements IssueCreationFieldsService, InitializingBean {

  //  private final ConcurrentHashMap<String, ChatState> chatsStateMap;
  //  private final MyteamApiClient myteamApiClient;
  //  private final UserData userData;
  //  private final PluginData pluginData;
  private final MessageFormatter messageFormatter;
  private final I18nResolver i18nResolver;
  //  private final LocaleManager localeManager;
  //  private final PermissionManager permissionManager;
  //  private final ProjectManager projectManager;
  //  private final IssueTypeSchemeManager issueTypeSchemeManager;
  private final IssueTypeScreenSchemeManager issueTypeScreenSchemeManager;
  //  private final IssueTypeManager issueTypeManager;
  private final FieldLayoutManager fieldLayoutManager;
  private final FieldManager fieldManager;
  //  private final ConstantsManager constantsManager;
  private final IssueService issueService;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final HashMap<Class<? extends AbstractCustomFieldType>, CreateIssueBaseCF>
      supportedIssueCreationCustomFields;

  public IssueCreationFieldsServiceImpl(
      //                                        ChatStateMapping chatStateMapping,
      //                                        MyteamApiClient myteamApiClient,
      //                                        UserData userData,
      //                                        PluginData pluginData,
      MessageFormatter messageFormatter,
      @ComponentImport I18nResolver i18nResolver,
      //                                        @ComponentImport LocaleManager localeManager,
      //                                        @ComponentImport PermissionManager
      // permissionManager,
      //                                        @ComponentImport ProjectManager projectManager,
      //                                        @ComponentImport IssueTypeSchemeManager
      // issueTypeSchemeManager,
      @ComponentImport IssueTypeScreenSchemeManager issueTypeScreenSchemeManager,
      //                                        @ComponentImport IssueTypeManager issueTypeManager,
      @ComponentImport FieldLayoutManager fieldLayoutManager,
      @ComponentImport FieldManager fieldManager,
      //                                        @ComponentImport ConstantsManager constantsManager,
      @ComponentImport IssueService issueService,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext) {
    //    this.chatsStateMap = chatStateMapping.getChatsStateMap();
    //    this.myteamApiClient = myteamApiClient;
    //    this.userData = userData;
    //    this.pluginData = pluginData;
    this.messageFormatter = messageFormatter;
    this.i18nResolver = i18nResolver;
    //    this.localeManager = localeManager;
    //    this.permissionManager = permissionManager;
    //    this.projectManager = projectManager;
    //    this.issueTypeSchemeManager = issueTypeSchemeManager;
    this.issueTypeScreenSchemeManager = issueTypeScreenSchemeManager;
    //    this.issueTypeManager = issueTypeManager;
    this.fieldLayoutManager = fieldLayoutManager;
    this.fieldManager = fieldManager;
    //    this.constantsManager = constantsManager;
    this.issueService = issueService;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.supportedIssueCreationCustomFields = new HashMap<>();
  }

  @Override
  public void afterPropertiesSet() {
    Checkbox checkbox = new Checkbox(i18nResolver, messageFormatter);
    supportedIssueCreationCustomFields.put(checkbox.getCFTypeClass(), checkbox);
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

  @Override
  public IssueService.CreateValidationResult validateIssueWithGivenFields(
      ApplicationUser currentUser, IssueCreationDto issueCreationDto) {
    Long projectId = issueCreationDto.getProjectId();

    // need here to because issueService use authenticationContext
    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
    try {
      jiraAuthenticationContext.setLoggedInUser(currentUser);
      IssueInputParameters issueInputParameters =
          issueService.newIssueInputParameters(
              issueCreationDto.getRequiredIssueCreationFieldValues().entrySet().stream()
                  .collect(
                      Collectors.toMap(
                          (e) -> e.getKey().getId(),
                          (e) -> {
                            if (fieldManager.isCustomFieldId(e.getKey().getId())) {
                              CustomField cf = fieldManager.getCustomField(e.getKey().getId());

                              if (cf != null
                                  && supportedIssueCreationCustomFields.containsKey(
                                      cf.getCustomFieldType().getClass())) {
                                CreateIssueBaseCF cfConfig =
                                    supportedIssueCreationCustomFields.get(
                                        cf.getCustomFieldType().getClass());

                                return cfConfig.getValue(issueCreationDto, cf);
                              }
                            }
                            return messageFormatter.mapUserInputStringToFieldValue(
                                projectId, e.getKey(), e.getValue());
                          })));
      issueInputParameters.setRetainExistingValuesWhenParameterNotProvided(true, true);

      // manually setting current user as issue reporter and selected ProjectId and IssueTypeId
      issueInputParameters.setProjectId(issueCreationDto.getProjectId());
      issueInputParameters.setIssueTypeId(issueCreationDto.getIssueTypeId());
      issueInputParameters.setReporterId(currentUser.getName());

      return issueService.validateCreate(currentUser, issueInputParameters);
    } finally {
      jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
    }
  }

  @Override
  public boolean isFieldSupported(String fieldId) {
    CustomField field = fieldManager.getCustomField(fieldId);

    return field != null
        && supportedIssueCreationCustomFields.containsKey(field.getCustomFieldType().getClass());
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
