package ru.mail.jira.plugins.mrimsender.protocol.listeners;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.config.manager.FieldConfigSchemeManager;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.issue.search.constants.SystemSearchConstants;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.thread.JiraThreadLocalUtils;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.eventbus.Subscribe;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.mrimsender.configuration.PluginData;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.protocol.ChatState;
import ru.mail.jira.plugins.mrimsender.protocol.ChatStateMapping;
import ru.mail.jira.plugins.mrimsender.protocol.IssueCreationDto;
import ru.mail.jira.plugins.mrimsender.protocol.MessageFormatter;
import ru.mail.jira.plugins.mrimsender.protocol.events.NewIssueFieldValueMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.SelectedProjectMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.CreateIssueClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.IssueTypeButtonClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.NextProjectsPageClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.PrevProjectsPageClickEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.mail.jira.plugins.mrimsender.protocol.MessageFormatter.LIST_PAGE_SIZE;

@Slf4j
public class CreateIssueEventsListener {
    private final ConcurrentHashMap<String, ChatState> chatsStateMap;
    private final IcqApiClient icqApiClient;
    private final UserData userData;
    private final PluginData pluginData;
    private final MessageFormatter messageFormatter;
    private final I18nResolver i18nResolver;
    private final LocaleManager localeManager;
    private final PermissionManager permissionManager;
    private final ProjectManager projectManager;
    private final IssueTypeSchemeManager issueTypeSchemeManager;
    private final IssueTypeScreenSchemeManager issueTypeScreenSchemeManager;
    private final IssueTypeManager issueTypeManager;
    private final FieldLayoutManager fieldLayoutManager;
    private final FieldManager fieldManager;
    private final IssueService issueService;
    private final JiraAuthenticationContext jiraAuthenticationContext;

    public CreateIssueEventsListener(ChatStateMapping chatStateMapping,
                                     IcqApiClient icqApiClient,
                                     UserData userData,
                                     PluginData pluginData,
                                     MessageFormatter messageFormatter,
                                     I18nResolver i18nResolver,
                                     LocaleManager localeManager,
                                     PermissionManager permissionManager,
                                     ProjectManager projectManager,
                                     IssueTypeSchemeManager issueTypeSchemeManager,
                                     IssueTypeScreenSchemeManager issueTypeScreenSchemeManager,
                                     IssueTypeManager issueTypeManager,
                                     FieldLayoutManager fieldLayoutManager,
                                     FieldManager fieldManager,
                                     IssueService issueService,
                                     JiraAuthenticationContext jiraAuthenticationContext) {
        this.chatsStateMap = chatStateMapping.getChatsStateMap();
        this.icqApiClient = icqApiClient;
        this.userData = userData;
        this.pluginData = pluginData;
        this.messageFormatter = messageFormatter;
        this.i18nResolver = i18nResolver;
        this.localeManager = localeManager;
        this.permissionManager = permissionManager;
        this.projectManager = projectManager;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
        this.issueTypeScreenSchemeManager = issueTypeScreenSchemeManager;
        this.issueTypeManager = issueTypeManager;
        this.fieldLayoutManager = fieldLayoutManager;
        this.fieldManager = fieldManager;
        this.issueService = issueService;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
    }

    private boolean isProjectExcluded(Long projectId) {
        return pluginData.getExcludingProjectIds().contains(projectId);
    }

    @Subscribe
    public void onCreateIssueClickEvent(CreateIssueClickEvent createIssueClickEvent) throws UnirestException, IOException {
        log.debug("CreateIssueClickEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(createIssueClickEvent.getUserId());
        String chatId = createIssueClickEvent.getChatId();
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            icqApiClient.answerCallbackQuery(createIssueClickEvent.getQueryId());
            List<Project> allowedProjectList = projectManager.getProjects().stream().filter(proj -> !isProjectExcluded(proj.getId())).collect(Collectors.toList());
            List<Project> firstPageProjectsInterval = allowedProjectList.stream().limit(LIST_PAGE_SIZE).collect(Collectors.toList());
            icqApiClient.sendMessageText(chatId,
                                         messageFormatter.createSelectProjectMessage(locale, firstPageProjectsInterval, 0, allowedProjectList.size()),
                                         messageFormatter.getSelectProjectMessageButtons(locale, false, allowedProjectList.size() > LIST_PAGE_SIZE));
            chatsStateMap.put(chatId, ChatState.buildProjectSelectWaitingState(0, new IssueCreationDto()));
        }
        log.debug("CreateIssueClickEvent handling finished");
    }

    @Subscribe
    public void onNextProjectPageClickEvent(NextProjectsPageClickEvent nextProjectsPageClickEvent) throws UnirestException, IOException {
        log.debug("NextProjectsPageClickEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(nextProjectsPageClickEvent.getUserId());
        if (currentUser != null) {
            int nextPageNumber = nextProjectsPageClickEvent.getCurrentPage() + 1;
            int nextPageStartIndex = nextPageNumber * LIST_PAGE_SIZE;
            Locale locale = localeManager.getLocaleFor(currentUser);
            String chatId = nextProjectsPageClickEvent.getChatId();

            List<Project> allowedProjectList = projectManager.getProjects()
                                                             .stream()
                                                             .filter(proj -> !isProjectExcluded(proj.getId()))
                                                             .collect(Collectors.toList());
            List<Project> nextProjectsInterval = allowedProjectList.stream()
                                                                   .skip(nextPageStartIndex)
                                                                   .limit(LIST_PAGE_SIZE)
                                                                   .collect(Collectors.toList());

            icqApiClient.answerCallbackQuery(nextProjectsPageClickEvent.getQueryId());
            icqApiClient.editMessageText(chatId,
                                         nextProjectsPageClickEvent.getMsgId(),
                                         messageFormatter.createSelectProjectMessage(locale, nextProjectsInterval, nextPageNumber, allowedProjectList.size()),
                                         messageFormatter.getSelectProjectMessageButtons(locale, true, allowedProjectList.size() > LIST_PAGE_SIZE + nextPageStartIndex));
            chatsStateMap.put(chatId, ChatState.buildProjectSelectWaitingState(nextPageNumber, nextProjectsPageClickEvent.getIssueCreationDto()));
        }
        log.debug("NextProjectsPageClickEvent handling finished");
    }

    @Subscribe
    public void onPrevProjectPageClickEvent(PrevProjectsPageClickEvent prevProjectsPageClickEvent) throws UnirestException, IOException {
        log.debug("PrevProjectsPageClickEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(prevProjectsPageClickEvent.getUserId());
        if (currentUser != null) {
            int prevPageNumber = prevProjectsPageClickEvent.getCurrentPage() - 1;
            int prevPageStartIndex = prevPageNumber * LIST_PAGE_SIZE;
            Locale locale = localeManager.getLocaleFor(currentUser);
            String chatId = prevProjectsPageClickEvent.getChatId();

            List<Project> allowedProjectList = projectManager.getProjects()
                                                             .stream()
                                                             .filter(proj -> !isProjectExcluded(proj.getId()))
                                                             .collect(Collectors.toList());
            List<Project> prevProjectsInterval = allowedProjectList.stream()
                                                             .skip(prevPageStartIndex)
                                                             .limit(LIST_PAGE_SIZE)
                                                             .collect(Collectors.toList());
            icqApiClient.answerCallbackQuery(prevProjectsPageClickEvent.getQueryId());
            icqApiClient.editMessageText(chatId,
                                         prevProjectsPageClickEvent.getMsgId(),
                                         messageFormatter.createSelectProjectMessage(locale, prevProjectsInterval, prevPageNumber, allowedProjectList.size()),
                                         messageFormatter.getSelectProjectMessageButtons(locale, prevPageStartIndex >= LIST_PAGE_SIZE, true));
            chatsStateMap.put(chatId, ChatState.buildProjectSelectWaitingState(prevPageNumber, prevProjectsPageClickEvent.getIssueCreationDto()));
        }
        log.debug("PrevProjectsPageClickEvent handling finished");
    }

    @Subscribe
    public void onSelectedProjectMessageEvent(SelectedProjectMessageEvent selectedProjectMessageEvent) throws IOException, UnirestException {
        IssueCreationDto currentIssueCreationDto = selectedProjectMessageEvent.getIssueCreationDto();
        ApplicationUser currentUser = userData.getUserByMrimLogin(selectedProjectMessageEvent.getUserId());
        if (currentUser == null) {
            // TODO unauthorized
        }

        String chatId = selectedProjectMessageEvent.getChatId();
        Locale locale = localeManager.getLocaleFor(currentUser);
        String selectedProjectKey = selectedProjectMessageEvent.getSelectedProjectKey();
        Project selectedProject = projectManager.getProjectByCurrentKeyIgnoreCase(selectedProjectKey);
        if (selectedProject == null) {
            // inserted project key is not valid
            icqApiClient.sendMessageText(chatId, i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.messageFormatter.createIssue.selectedProjectNotValid"));
            return;
        }
        if (isProjectExcluded(selectedProject.getId())) {
            icqApiClient.sendMessageText(chatId, i18nResolver.getText(locale, "ru.mail.jira.plugins.mrimsender.messageFormatter.createIssue.selectedProjectIsBanned"));
            return;
        }

        if (!permissionManager.hasPermission(ProjectPermissions.CREATE_ISSUES, selectedProject, currentUser)) {
            // user don't have enough permissions to create issues in selected project
            icqApiClient.sendMessageText(chatId, i18nResolver.getRawText("ru.mail.jira.plugins.mrimsender.messageFormatter.createIssue.notEnoughPermissions"));
            return;
        }

        // need here because messageFormatter.getSelectIssueTypeMessageButtons use authenticationContext for i18nHelper to translate issue types
        ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
        try {
            jiraAuthenticationContext.setLoggedInUser(currentUser);
            // Project selected, sending user select IssueType message
            currentIssueCreationDto.setProjectId(selectedProject.getId());
            Collection<IssueType> projectIssueTypes = issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(selectedProject);
            icqApiClient.sendMessageText(chatId,
                                         messageFormatter.getSelectIssueTypeMessage(locale),
                                         messageFormatter.getSelectIssueTypeMessageButtons(projectIssueTypes));
            chatsStateMap.put(chatId, ChatState.buildIssueTypeSelectWaitingState(currentIssueCreationDto));
        }
        finally {
            jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
        }


    }

    @Subscribe
    public void onIssueTypeButtonClickEvent(IssueTypeButtonClickEvent issueTypeButtonClickEvent) throws IOException, UnirestException {
        String queryId = issueTypeButtonClickEvent.getQueryId();
        IssueCreationDto currentIssueCreationDto = issueTypeButtonClickEvent.getIssueCreationDto();
        ApplicationUser currentUser = userData.getUserByMrimLogin(issueTypeButtonClickEvent.getUserId());
        if (currentUser == null) {
            // TODO unauthorized
        }
        Project selectedProject = projectManager.getProjectObj(currentIssueCreationDto.getProjectId());
        String chatId = issueTypeButtonClickEvent.getChatId();
        Locale locale = localeManager.getLocaleFor(currentUser);

        Optional<IssueType> maybeCorrectIssueType = Optional.ofNullable(issueTypeManager.getIssueType(issueTypeButtonClickEvent.getSelectedIssueTypeId()));
        if (!maybeCorrectIssueType.isPresent()) {
            // inserted issue type position isn't correct
            icqApiClient.answerCallbackQuery(queryId);
            icqApiClient.sendMessageText(chatId, i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.messageFormatter.createIssue.selectedIssueTypeNotValid"));
            return;
        }
        IssueType selectedIssueType = maybeCorrectIssueType.get();

        // description field mus be included no matter required it or not
        Set<String> includedFieldIds = Stream.of(fieldManager.getField(IssueFieldConstants.DESCRIPTION).getId()).collect(Collectors.toSet());
        // project and issueType fields should be excluded no matter required them or not
        Set<String> excludedFieldIds = Stream.of(fieldManager.getProjectField().getId(), fieldManager.getIssueTypeField().getId()).collect(Collectors.toSet());
        LinkedHashMap<Field, String> issueCreationRequiredFieldsValues = getIssueCreationRequiredFieldsValues(selectedProject, selectedIssueType, includedFieldIds, excludedFieldIds);

        // We don't need allow user to fill reporter field
        issueCreationRequiredFieldsValues.remove(fieldManager.getOrderableField(SystemSearchConstants.forReporter().getFieldId()));

        // setting selected IssueType and mapped requiredIssueCreationFields
        currentIssueCreationDto.setIssueTypeId(selectedIssueType.getId());
        currentIssueCreationDto.setRequiredIssueCreationFieldValues(issueCreationRequiredFieldsValues);

        // order saved here because LinkedHashMap keySet() method  in reality returns LinkedHashSet
        List<Field> requiredFields = new ArrayList<>(issueCreationRequiredFieldsValues.keySet());

        List<Field> requiredCustomFieldsInScope = requiredFields.stream()
                                                                .filter(field -> fieldManager.isCustomFieldId(field.getId()))
                                                                .collect(Collectors.toList());


        if (!requiredCustomFieldsInScope.isEmpty()) {
            // send user message that we can't create issue with required customFields
            icqApiClient.answerCallbackQuery(queryId);
            icqApiClient.sendMessageText(chatId,
                                         String.join("\n", i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.messageFormatter.createIssue.requiredCFError"), messageFormatter.stringifyFieldsCollection(locale, requiredCustomFieldsInScope)));
            return;
        }

        if (!requiredFields.isEmpty()) {
            // here sending new issue filling fields message to user
            Field currentField = requiredFields.get(0);
            icqApiClient.answerCallbackQuery(queryId);
            icqApiClient.sendMessageText(chatId, messageFormatter.createInsertFieldMessage(locale, currentField, currentIssueCreationDto));
            chatsStateMap.put(chatId, ChatState.buildNewIssueFieldsFillingState(0, currentIssueCreationDto));
        } else {
            // well, all required issue fields are filled, then just create issue
            IssueService.CreateValidationResult issueValidationResult = validateIssueWithGivenFields(currentUser, currentIssueCreationDto);
            if (issueValidationResult.isValid()) {
                JiraThreadLocalUtils.preCall();
                try {
                    issueService.create(currentUser, issueValidationResult);
                    icqApiClient.answerCallbackQuery(queryId);
                    icqApiClient.sendMessageText(chatId, "Congratulations, issue was created =)");
                } finally {
                    JiraThreadLocalUtils.postCall();
                }
            } else {
                icqApiClient.answerCallbackQuery(queryId);
                icqApiClient.sendMessageText(chatId, String.join("\n",
                                                                 i18nResolver.getText(locale, "ru.mail.jira.plugins.mrimsender.messageFormatter.createIssue.validationError"),
                                                                 messageFormatter.stringifyMap(issueValidationResult.getErrorCollection().getErrors()),
                                                                 messageFormatter.stringifyCollection(locale, issueValidationResult.getErrorCollection().getErrorMessages())));
            }
        }
    }

    @Subscribe
    public void onNewIssueFieldValueMessageEvent(NewIssueFieldValueMessageEvent newIssueFieldValueMessageEvent) throws IOException, UnirestException {
        ApplicationUser currentUser = userData.getUserByMrimLogin(newIssueFieldValueMessageEvent.getUserId());
        if (currentUser == null) {
            // TODO unauthorized
            return;
        }
        String chatId = newIssueFieldValueMessageEvent.getChatId();
        Locale locale = localeManager.getLocaleFor(currentUser);
        IssueCreationDto currentIssueCreationDto = newIssueFieldValueMessageEvent.getIssueCreationDto();
        Integer currentFieldNum = newIssueFieldValueMessageEvent.getCurrentFieldNum();
        int nextFieldNum = currentFieldNum + 1;
        String currentFieldValueStr = newIssueFieldValueMessageEvent.getFieldValue();
        List<Field> requiredFields = new ArrayList<>(currentIssueCreationDto.getRequiredIssueCreationFieldValues().keySet());

        // setting new field string value
        currentIssueCreationDto.getRequiredIssueCreationFieldValues()
                               .put(requiredFields.get(currentFieldNum), currentFieldValueStr);


        if (requiredFields.size() > nextFieldNum) {
            Field nextField = requiredFields.get(nextFieldNum);
            icqApiClient.sendMessageText(chatId, messageFormatter.createInsertFieldMessage(locale, nextField, currentIssueCreationDto));
            chatsStateMap.put(chatId, ChatState.buildNewIssueFieldsFillingState(nextFieldNum, currentIssueCreationDto));
        } else {
            JiraThreadLocalUtils.preCall();
            try {
                // then user filled all new issue fields which are required

                IssueService.CreateValidationResult issueValidationResult = validateIssueWithGivenFields(currentUser, currentIssueCreationDto);
                if (issueValidationResult.isValid()) {
                    issueService.create(currentUser, issueValidationResult);
                    icqApiClient.sendMessageText(chatId, i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.messageFormatter.createIssue.issueCreated"));
                } else {
                    icqApiClient.sendMessageText(chatId, String.join("\n",
                                                                     i18nResolver.getText(locale, "ru.mail.jira.plugins.mrimsender.messageFormatter.createIssue.validationError"),
                                                                     messageFormatter.stringifyMap(issueValidationResult.getErrorCollection().getErrors()),
                                                                     messageFormatter.stringifyCollection(locale, issueValidationResult.getErrorCollection().getErrorMessages())));
                }
            } finally {
                JiraThreadLocalUtils.postCall();
            }
        }
    }

    /**
     * Get a map of required during creation issue fields by selected project and issue type
     * Algo:
     * 1) Getting all fields which shown on CREATE_ISSUE_OPERATION fields screen
     * 2) All fields must be required in FieldLayout => filtration
     * 3) All custom fields must have correct context scope to be shown on issue creation form
     *
     * @param project          - selected project
     * @param issueType        - selected issue type
     * @param includedFieldIds - fields which must be included in result map no matter required them or not
     * @param excludedFieldIds - fields which must be excluded from result map no matter required them or not
     * @return LinkedHashMap of field and empty string value
     */
    private LinkedHashMap<Field, String> getIssueCreationRequiredFieldsValues(Project project, IssueType issueType, Set<String> includedFieldIds, Set<String> excludedFieldIds) {
        // getting (selectedProject, selectedIssueType) fields configuration
        FieldLayout fieldLayout = fieldLayoutManager.getFieldLayout(project, issueType.getId());
        // getting (selectedProject, selectedIssueType, selectedIssueOperation) fields screen
        return issueTypeScreenSchemeManager.getIssueTypeScreenScheme(project)
                                           .getEffectiveFieldScreenScheme(issueType)
                                           .getFieldScreen(IssueOperations.CREATE_ISSUE_OPERATION)
                                           .getTabs()
                                           .stream()
                                           .flatMap(tab -> tab.getFieldScreenLayoutItems()
                                                              .stream()
                                                              .filter(fieldScreenLayoutItem -> {
                                                                  String fieldId = fieldScreenLayoutItem.getFieldId();
                                                                  FieldLayoutItem fieldLayoutItem = fieldLayout.getFieldLayoutItem(fieldId);
                                                                  return fieldLayoutItem != null && fieldLayoutItem.isRequired() || includedFieldIds.contains(fieldId);
                                                              })
                                                              .filter(layoutItem -> !excludedFieldIds.contains(layoutItem.getFieldId()))
                                                              .filter(fieldScreenLayoutItem -> {
                                                                  // all custom fields must be in project and issue type context scope
                                                                  if (fieldManager.isCustomFieldId(fieldScreenLayoutItem.getFieldId())) {
                                                                      CustomField cf = (CustomField) fieldScreenLayoutItem.getOrderableField();
                                                                      return isCFInScopeOfProjectAndIssueType(cf, project.getId(), issueType.getId());
                                                                  }
                                                                  // all fields which is not custom fields, are not filtered
                                                                  return true;
                                                              })
                                                              .map(FieldScreenLayoutItem::getOrderableField))
                                           .collect(Collectors.toMap(Function.identity(),
                                                                     (field) -> "",
                                                                     (v1, v2) -> v1,
                                                                     LinkedHashMap::new));
    }


    private IssueService.CreateValidationResult validateIssueWithGivenFields(ApplicationUser currentUser, IssueCreationDto issueCreationDto) {
        Long projectId = issueCreationDto.getProjectId();

        // need here to because issueService use authenticationContext
        ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
        try {
            jiraAuthenticationContext.setLoggedInUser(currentUser);
            IssueInputParameters issueInputParameters = issueService.newIssueInputParameters(issueCreationDto.getRequiredIssueCreationFieldValues()
                                                                                                             .entrySet()
                                                                                                             .stream()
                                                                                                             .collect(Collectors.toMap((e) -> e.getKey().getId(),
                                                                                                                                       (e) -> messageFormatter.mapUserInputStringToFieldValue(projectId, e.getKey(), e.getValue()))));
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

    private boolean isCFInScopeOfProjectAndIssueType(CustomField customField, Long projectId, String issueTypeId) {
        // if any of configuration scheme of
        return customField.getConfigurationSchemes()
                          .stream()
                          .anyMatch(scheme -> {
                              if (scheme.isAllProjects() && scheme.isAllIssueTypes())
                                  return true;
                              return (scheme.isAllProjects() || scheme.getAssociatedProjectIds().contains(projectId)) && (scheme.isAllIssueTypes() || scheme.getAssociatedIssueTypeIds().contains(issueTypeId));
                          });

    }
}