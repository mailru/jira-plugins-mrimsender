/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.listeners;

import static ru.mail.jira.plugins.myteam.protocol.MessageFormatter.ADDITIONAL_FIELD_LIST_PAGE_SIZE;
import static ru.mail.jira.plugins.myteam.protocol.MessageFormatter.LIST_PAGE_SIZE;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
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
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.eventbus.Subscribe;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.commons.IssueFieldsFilterEnum;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.model.PluginData;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.protocol.ChatState;
import ru.mail.jira.plugins.myteam.protocol.ChatStateMapping;
import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.protocol.events.NewAdditionalFieldMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.NewIssueFieldValueMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.NewIssueValueEvent;
import ru.mail.jira.plugins.myteam.protocol.events.SelectedProjectMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.*;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.additionalfields.*;

@Slf4j
@Component
public class CreateIssueEventsListener {
  private final ConcurrentHashMap<String, ChatState> chatsStateMap;
  private final MyteamApiClient myteamApiClient;
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
  private final ConstantsManager constantsManager;
  private final IssueService issueService;
  private final JiraAuthenticationContext jiraAuthenticationContext;

  @Autowired
  public CreateIssueEventsListener(
      ChatStateMapping chatStateMapping,
      MyteamApiClient myteamApiClient,
      UserData userData,
      PluginData pluginData,
      MessageFormatter messageFormatter,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport LocaleManager localeManager,
      @ComponentImport PermissionManager permissionManager,
      @ComponentImport ProjectManager projectManager,
      @ComponentImport IssueTypeSchemeManager issueTypeSchemeManager,
      @ComponentImport IssueTypeScreenSchemeManager issueTypeScreenSchemeManager,
      @ComponentImport IssueTypeManager issueTypeManager,
      @ComponentImport FieldLayoutManager fieldLayoutManager,
      @ComponentImport FieldManager fieldManager,
      @ComponentImport ConstantsManager constantsManager,
      @ComponentImport IssueService issueService,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext) {
    this.chatsStateMap = chatStateMapping.getChatsStateMap();
    this.myteamApiClient = myteamApiClient;
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
    this.constantsManager = constantsManager;
    this.issueService = issueService;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
  }

  private boolean isProjectExcluded(Long projectId) {
    return pluginData.getExcludingProjectIds().contains(projectId);
  }

  @Subscribe
  public void onCreateIssueClickEvent(CreateIssueClickEvent createIssueClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {
    log.debug("CreateIssueClickEvent handling started");
    ApplicationUser currentUser = userData.getUserByMrimLogin(createIssueClickEvent.getUserId());
    String chatId = createIssueClickEvent.getChatId();
    if (currentUser != null) {
      Locale locale = localeManager.getLocaleFor(currentUser);
      myteamApiClient.answerCallbackQuery(createIssueClickEvent.getQueryId());
      List<Project> allowedProjectList =
          projectManager.getProjects().stream()
              .filter(proj -> !isProjectExcluded(proj.getId()))
              .collect(Collectors.toList());
      List<Project> firstPageProjectsInterval =
          allowedProjectList.stream().limit(LIST_PAGE_SIZE).collect(Collectors.toList());
      myteamApiClient.sendMessageText(
          chatId,
          messageFormatter.createSelectProjectMessage(
              locale, firstPageProjectsInterval, 0, allowedProjectList.size()),
          messageFormatter.buildButtonsWithCancel(
              messageFormatter.getSelectProjectMessageButtons(
                  locale, false, allowedProjectList.size() > LIST_PAGE_SIZE),
              i18nResolver.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text")));
      chatsStateMap.put(
          chatId, ChatState.buildProjectSelectWaitingState(0, new IssueCreationDto()));
    }
    log.debug("CreateIssueClickEvent handling finished");
  }

  @Subscribe
  public void onNextProjectPageClickEvent(NextProjectsPageClickEvent nextProjectsPageClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {
    log.debug("NextProjectsPageClickEvent handling started");
    ApplicationUser currentUser =
        userData.getUserByMrimLogin(nextProjectsPageClickEvent.getUserId());
    if (currentUser != null) {
      int nextPageNumber = nextProjectsPageClickEvent.getCurrentPage() + 1;
      int nextPageStartIndex = nextPageNumber * LIST_PAGE_SIZE;
      Locale locale = localeManager.getLocaleFor(currentUser);
      String chatId = nextProjectsPageClickEvent.getChatId();

      List<Project> allowedProjectList =
          projectManager.getProjects().stream()
              .filter(proj -> !isProjectExcluded(proj.getId()))
              .collect(Collectors.toList());
      List<Project> nextProjectsInterval =
          allowedProjectList.stream()
              .skip(nextPageStartIndex)
              .limit(LIST_PAGE_SIZE)
              .collect(Collectors.toList());

      myteamApiClient.answerCallbackQuery(nextProjectsPageClickEvent.getQueryId());
      myteamApiClient.editMessageText(
          chatId,
          nextProjectsPageClickEvent.getMsgId(),
          messageFormatter.createSelectProjectMessage(
              locale, nextProjectsInterval, nextPageNumber, allowedProjectList.size()),
          messageFormatter.buildButtonsWithCancel(
              messageFormatter.getSelectProjectMessageButtons(
                  locale, true, allowedProjectList.size() > LIST_PAGE_SIZE + nextPageStartIndex),
              i18nResolver.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text")));
      chatsStateMap.put(
          chatId,
          ChatState.buildProjectSelectWaitingState(
              nextPageNumber, nextProjectsPageClickEvent.getIssueCreationDto()));
    }
    log.debug("NextProjectsPageClickEvent handling finished");
  }

  @Subscribe
  public void onPrevProjectPageClickEvent(PrevProjectsPageClickEvent prevProjectsPageClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {
    log.debug("PrevProjectsPageClickEvent handling started");
    ApplicationUser currentUser =
        userData.getUserByMrimLogin(prevProjectsPageClickEvent.getUserId());
    if (currentUser != null) {
      int prevPageNumber = prevProjectsPageClickEvent.getCurrentPage() - 1;
      int prevPageStartIndex = prevPageNumber * LIST_PAGE_SIZE;
      Locale locale = localeManager.getLocaleFor(currentUser);
      String chatId = prevProjectsPageClickEvent.getChatId();

      List<Project> allowedProjectList =
          projectManager.getProjects().stream()
              .filter(proj -> !isProjectExcluded(proj.getId()))
              .collect(Collectors.toList());
      List<Project> prevProjectsInterval =
          allowedProjectList.stream()
              .skip(prevPageStartIndex)
              .limit(LIST_PAGE_SIZE)
              .collect(Collectors.toList());
      myteamApiClient.answerCallbackQuery(prevProjectsPageClickEvent.getQueryId());
      myteamApiClient.editMessageText(
          chatId,
          prevProjectsPageClickEvent.getMsgId(),
          messageFormatter.createSelectProjectMessage(
              locale, prevProjectsInterval, prevPageNumber, allowedProjectList.size()),
          messageFormatter.buildButtonsWithCancel(
              messageFormatter.getSelectProjectMessageButtons(
                  locale, prevPageStartIndex >= LIST_PAGE_SIZE, true),
              i18nResolver.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text")));
      chatsStateMap.put(
          chatId,
          ChatState.buildProjectSelectWaitingState(
              prevPageNumber, prevProjectsPageClickEvent.getIssueCreationDto()));
    }
    log.debug("PrevProjectsPageClickEvent handling finished");
  }

  @Subscribe
  public void onSelectedProjectMessageEvent(SelectedProjectMessageEvent selectedProjectMessageEvent)
      throws IOException, UnirestException, MyteamServerErrorException {
    IssueCreationDto currentIssueCreationDto = selectedProjectMessageEvent.getIssueCreationDto();
    ApplicationUser currentUser =
        userData.getUserByMrimLogin(selectedProjectMessageEvent.getUserId());
    if (currentUser == null) {
      // TODO unauthorized
    }

    String chatId = selectedProjectMessageEvent.getChatId();
    Locale locale = localeManager.getLocaleFor(currentUser);
    String selectedProjectKey = selectedProjectMessageEvent.getSelectedProjectKey();
    Project selectedProject = projectManager.getProjectByCurrentKeyIgnoreCase(selectedProjectKey);
    if (selectedProject == null) {
      // inserted project key is not valid
      myteamApiClient.sendMessageText(
          chatId,
          i18nResolver.getRawText(
              locale,
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectedProjectNotValid"));
      return;
    }
    if (isProjectExcluded(selectedProject.getId())) {
      myteamApiClient.sendMessageText(
          chatId,
          i18nResolver.getText(
              locale,
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectedProjectIsBanned"));
      return;
    }

    if (!permissionManager.hasPermission(
        ProjectPermissions.CREATE_ISSUES, selectedProject, currentUser)) {
      // user don't have enough permissions to create issues in selected project
      myteamApiClient.sendMessageText(
          chatId,
          i18nResolver.getRawText(
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.notEnoughPermissions"));
      return;
    }

    // need here because messageFormatter.getSelectIssueTypeMessageButtons use authenticationContext
    // for i18nHelper to translate issue types
    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
    try {
      jiraAuthenticationContext.setLoggedInUser(currentUser);
      // Project selected, sending user select IssueType message
      currentIssueCreationDto.setProjectId(selectedProject.getId());
      Collection<IssueType> projectIssueTypes =
          issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(selectedProject);
      myteamApiClient.sendMessageText(
          chatId,
          messageFormatter.getSelectIssueTypeMessage(locale),
          messageFormatter.buildButtonsWithCancel(
              messageFormatter.buildIssueTypesButtons(projectIssueTypes),
              i18nResolver.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text")));
      chatsStateMap.put(
          chatId, ChatState.buildIssueTypeSelectWaitingState(currentIssueCreationDto));
    } finally {
      jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
    }
  }

  @Subscribe
  public void onIssueTypeButtonClickEvent(IssueTypeButtonClickEvent issueTypeButtonClickEvent)
      throws IOException, UnirestException, MyteamServerErrorException {
    String queryId = issueTypeButtonClickEvent.getQueryId();
    IssueCreationDto currentIssueCreationDto = issueTypeButtonClickEvent.getIssueCreationDto();
    ApplicationUser currentUser =
        userData.getUserByMrimLogin(issueTypeButtonClickEvent.getUserId());
    if (currentUser == null) {
      // TODO unauthorized
    }
    Project selectedProject = projectManager.getProjectObj(currentIssueCreationDto.getProjectId());
    String chatId = issueTypeButtonClickEvent.getChatId();
    Locale locale = localeManager.getLocaleFor(currentUser);

    Optional<IssueType> maybeCorrectIssueType =
        Optional.ofNullable(
            issueTypeManager.getIssueType(issueTypeButtonClickEvent.getSelectedIssueTypeId()));
    if (!maybeCorrectIssueType.isPresent()) {
      // inserted issue type position isn't correct
      myteamApiClient.answerCallbackQuery(queryId);
      myteamApiClient.sendMessageText(
          chatId,
          i18nResolver.getRawText(
              locale,
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectedIssueTypeNotValid"));
      return;
    }
    IssueType selectedIssueType = maybeCorrectIssueType.get();

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
            selectedProject,
            selectedIssueType,
            includedFieldIds,
            excludedFieldIds,
            IssueFieldsFilterEnum.REQUIRED);

    // We don't need allow user to fill reporter field
    issueCreationRequiredFieldsValues.remove(
        fieldManager.getOrderableField(SystemSearchConstants.forReporter().getFieldId()));

    // setting selected IssueType and mapped requiredIssueCreationFields
    currentIssueCreationDto.setIssueTypeId(selectedIssueType.getId());
    currentIssueCreationDto.setRequiredIssueCreationFieldValues(issueCreationRequiredFieldsValues);

    // order saved here because LinkedHashMap keySet() method  in reality returns LinkedHashSet
    List<Field> requiredFields = new ArrayList<>(issueCreationRequiredFieldsValues.keySet());

    List<Field> requiredCustomFieldsInScope =
        requiredFields.stream()
            .filter(field -> fieldManager.isCustomFieldId(field.getId()))
            .collect(Collectors.toList());

    if (!requiredCustomFieldsInScope.isEmpty()) {
      // send user message that we can't create issue with required customFields
      myteamApiClient.answerCallbackQuery(queryId);
      myteamApiClient.sendMessageText(
          chatId,
          String.join(
              "\n",
              i18nResolver.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.requiredCFError"),
              messageFormatter.stringifyFieldsCollection(locale, requiredCustomFieldsInScope)));
      return;
    }

    if (!requiredFields.isEmpty()) {
      // here sending new issue filling fields message to user
      Field currentField = requiredFields.get(0);
      myteamApiClient.answerCallbackQuery(queryId);
      myteamApiClient.sendMessageText(
          chatId,
          messageFormatter.createInsertFieldMessage(locale, currentField, currentIssueCreationDto),
          messageFormatter.buildButtonsWithCancel(
              null,
              i18nResolver.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text")));
      chatsStateMap.put(
          chatId, ChatState.buildNewIssueRequiredFieldsFillingState(0, currentIssueCreationDto));
    } else {
      // well, all required issue fields are filled, then just create issue
      IssueService.CreateValidationResult issueValidationResult =
          validateIssueWithGivenFields(currentUser, currentIssueCreationDto);
      if (issueValidationResult.isValid()) {
        JiraThreadLocalUtils.preCall();
        try {
          issueService.create(currentUser, issueValidationResult);
          myteamApiClient.answerCallbackQuery(queryId);
          myteamApiClient.sendMessageText(chatId, "Congratulations, issue was created =)");
        } finally {
          JiraThreadLocalUtils.postCall();
        }
      } else {
        myteamApiClient.answerCallbackQuery(queryId);
        myteamApiClient.sendMessageText(
            chatId,
            String.join(
                "\n",
                i18nResolver.getText(
                    locale,
                    "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.validationError"),
                messageFormatter.stringifyMap(
                    issueValidationResult.getErrorCollection().getErrors()),
                messageFormatter.stringifyCollection(
                    locale, issueValidationResult.getErrorCollection().getErrorMessages())));
      }
    }
  }

  @Subscribe
  public void onNewIssueFieldValueButtonEvent(
      NewIssueFieldValueButtonClickEvent newIssueFieldValueButtonClickEvent)
      throws IOException, UnirestException, MyteamServerErrorException {
    myteamApiClient.answerCallbackQuery(newIssueFieldValueButtonClickEvent.getQueryId());
    onNewValueSelected(newIssueFieldValueButtonClickEvent);
  }

  @Subscribe
  public void onNewIssueFieldValueMessageEvent(
      NewIssueFieldValueMessageEvent newIssueFieldValueMessageEvent)
      throws IOException, UnirestException, MyteamServerErrorException {
    onNewValueSelected(newIssueFieldValueMessageEvent);
  }

  private void onNewValueSelected(NewIssueValueEvent newIssueValueEvent)
      throws UnirestException, IOException, MyteamServerErrorException {
    ApplicationUser currentUser = userData.getUserByMrimLogin(newIssueValueEvent.getUserId());
    if (currentUser == null) {
      // TODO unauthorized
      return;
    }
    String chatId = newIssueValueEvent.getChatId();
    IssueCreationDto currentIssueCreationDto = newIssueValueEvent.getIssueCreationDto();
    int currentFieldNum = newIssueValueEvent.getCurrentFieldNum();
    String currentFieldValueStr = newIssueValueEvent.getFieldValue();
    Locale locale = localeManager.getLocaleFor(currentUser);
    int nextFieldNum = currentFieldNum + 1;
    List<Field> requiredFields =
        new ArrayList<>(currentIssueCreationDto.getRequiredIssueCreationFieldValues().keySet());

    // setting new field string value
    currentIssueCreationDto
        .getRequiredIssueCreationFieldValues()
        .put(requiredFields.get(currentFieldNum), currentFieldValueStr);

    if (requiredFields.size() > nextFieldNum) {
      Field nextField = requiredFields.get(nextFieldNum);
      if (nextField.getId().equals("priority")) { // select value with buttons
        myteamApiClient.sendMessageText(
            chatId,
            messageFormatter.createInsertFieldMessage(locale, nextField, currentIssueCreationDto),
            messageFormatter.buildButtonsWithCancel(
                messageFormatter.buildPrioritiesButtons(constantsManager.getPriorities()),
                i18nResolver.getRawText(
                    locale,
                    "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text")));
        chatsStateMap.put(
            chatId,
            ChatState.buildNewIssueButtonFieldsWaitingState(nextFieldNum, currentIssueCreationDto));
      } else { // select value by message
        myteamApiClient.sendMessageText(
            chatId,
            messageFormatter.createInsertFieldMessage(locale, nextField, currentIssueCreationDto),
            messageFormatter.buildButtonsWithCancel(
                null,
                i18nResolver.getRawText(
                    locale,
                    "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text")));
        chatsStateMap.put(
            chatId,
            ChatState.buildNewIssueRequiredFieldsFillingState(
                nextFieldNum, currentIssueCreationDto));
      }
    } else {
      JiraThreadLocalUtils.preCall();
      chatsStateMap.put(chatId, ChatState.buildIssueCreationConfirmState(currentIssueCreationDto));
      myteamApiClient.sendMessageText(
          chatId,
          i18nResolver.getText(
              locale,
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.issueCreationConfirmation"),
          messageFormatter.getIssueCreationConfirmButtons(currentUser));
    }
  }

  @Subscribe
  public void onAddExtraIssueFieldClickEvent(
      AddAdditionalIssueFieldClickEvent addExtraIssueFieldClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {

    ApplicationUser currentUser =
        userData.getUserByMrimLogin(addExtraIssueFieldClickEvent.getUserId());
    if (currentUser == null) {
      // TODO unauthorized
      return;
    }
    String chatId = addExtraIssueFieldClickEvent.getChatId();
    IssueCreationDto issueCreationDto = addExtraIssueFieldClickEvent.getIssueCreationDto();
    Locale locale = localeManager.getLocaleFor(currentUser);

    LinkedHashMap<Field, String> nonRequiredFields =
        getIssueCreationFieldsValues(
            projectManager.getProjectObj(issueCreationDto.getProjectId()),
            issueTypeManager.getIssueType(issueCreationDto.getIssueTypeId()),
            null,
            null,
            IssueFieldsFilterEnum.NON_REQUIRED);

    myteamApiClient.answerCallbackQuery(addExtraIssueFieldClickEvent.getQueryId());

    List<Field> firstPageFieldsInterval =
        nonRequiredFields.keySet().stream()
            .limit(ADDITIONAL_FIELD_LIST_PAGE_SIZE)
            .collect(Collectors.toList());
    myteamApiClient.editMessageText(
        chatId,
        addExtraIssueFieldClickEvent.getMsgId(),
        i18nResolver.getRawText(
            locale,
            "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectAdditionalField.message"),
        messageFormatter.getSelectAdditionalFieldMessageButtons(
            locale,
            false,
            nonRequiredFields.size() > ADDITIONAL_FIELD_LIST_PAGE_SIZE,
            firstPageFieldsInterval));
    chatsStateMap.put(
        chatId, ChatState.buildAdditionalIssueFieldSelectWaitingState(0, issueCreationDto));
  }

  @Subscribe
  public void onPrevAdditionalFieldPageClickEvent(
      PrevAdditionalFieldPageClickEvent prevAdditionalFieldPageClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {
    log.debug("PrevProjectsPageClickEvent handling started");
    ApplicationUser currentUser =
        userData.getUserByMrimLogin(prevAdditionalFieldPageClickEvent.getUserId());
    if (currentUser != null) {
      IssueCreationDto issueCreationDto = prevAdditionalFieldPageClickEvent.getIssueCreationDto();
      int prevPageNumber = prevAdditionalFieldPageClickEvent.getCurrentPage() - 1;
      int prevPageStartIndex = prevPageNumber * ADDITIONAL_FIELD_LIST_PAGE_SIZE;
      Locale locale = localeManager.getLocaleFor(currentUser);
      String chatId = prevAdditionalFieldPageClickEvent.getChatId();

      LinkedHashMap<Field, String> nonRequiredFields =
          getIssueCreationFieldsValues(
              projectManager.getProjectObj(issueCreationDto.getProjectId()),
              issueTypeManager.getIssueType(issueCreationDto.getIssueTypeId()),
              null,
              null,
              IssueFieldsFilterEnum.NON_REQUIRED);
      List<Field> pageFieldsInterval =
          nonRequiredFields.keySet().stream()
              .skip(prevPageStartIndex)
              .limit(ADDITIONAL_FIELD_LIST_PAGE_SIZE)
              .collect(Collectors.toList());
      myteamApiClient.answerCallbackQuery(prevAdditionalFieldPageClickEvent.getQueryId());
      myteamApiClient.editMessageText(
          chatId,
          prevAdditionalFieldPageClickEvent.getMsgId(),
          i18nResolver.getRawText(
              locale,
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectProject.message"),
          messageFormatter.getSelectAdditionalFieldMessageButtons(
              locale,
              prevPageStartIndex >= ADDITIONAL_FIELD_LIST_PAGE_SIZE,
              true,
              pageFieldsInterval));
      chatsStateMap.put(
          chatId,
          ChatState.buildAdditionalIssueFieldSelectWaitingState(prevPageNumber, issueCreationDto));
    }
    log.debug("PrevProjectsPageClickEvent handling finished");
  }

  @Subscribe
  public void onNextAdditionalFieldPageClickEvent(
      NextAdditionalFieldPageClickEvent nextAdditionalFieldPageClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {
    log.debug("PrevProjectsPageClickEvent handling started");
    ApplicationUser currentUser =
        userData.getUserByMrimLogin(nextAdditionalFieldPageClickEvent.getUserId());
    if (currentUser != null) {
      IssueCreationDto issueCreationDto = nextAdditionalFieldPageClickEvent.getIssueCreationDto();
      int nextPageNumber = nextAdditionalFieldPageClickEvent.getCurrentPage() + 1;
      int nextPageStartIndex = nextPageNumber * ADDITIONAL_FIELD_LIST_PAGE_SIZE;
      Locale locale = localeManager.getLocaleFor(currentUser);
      String chatId = nextAdditionalFieldPageClickEvent.getChatId();

      LinkedHashMap<Field, String> nonRequiredFields =
          getIssueCreationFieldsValues(
              projectManager.getProjectObj(issueCreationDto.getProjectId()),
              issueTypeManager.getIssueType(issueCreationDto.getIssueTypeId()),
              null,
              null,
              IssueFieldsFilterEnum.NON_REQUIRED);
      List<Field> pageFieldsInterval =
          nonRequiredFields.keySet().stream()
              .skip(nextPageStartIndex)
              .limit(ADDITIONAL_FIELD_LIST_PAGE_SIZE)
              .collect(Collectors.toList());

      myteamApiClient.answerCallbackQuery(nextAdditionalFieldPageClickEvent.getQueryId());
      myteamApiClient.editMessageText(
          chatId,
          nextAdditionalFieldPageClickEvent.getMsgId(),
          i18nResolver.getRawText(
              locale,
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectProject.message"),
          messageFormatter.getSelectAdditionalFieldMessageButtons(
              locale,
              true,
              nonRequiredFields.size() > ADDITIONAL_FIELD_LIST_PAGE_SIZE + nextPageStartIndex,
              pageFieldsInterval));
      chatsStateMap.put(
          chatId,
          ChatState.buildAdditionalIssueFieldSelectWaitingState(nextPageNumber, issueCreationDto));
    }
    log.debug("PrevProjectsPageClickEvent handling finished");
  }

  @Subscribe
  public void onSelectAdditionalIssueFieldClickEvent(
      SelectAdditionalIssueFieldClickEvent selectAdditionalIssueFieldClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {

    ApplicationUser currentUser =
        userData.getUserByMrimLogin(selectAdditionalIssueFieldClickEvent.getUserId());
    if (currentUser == null) {
      // TODO unauthorized
      return;
    }
    String fieldId = selectAdditionalIssueFieldClickEvent.getFieldId();
    String chatId = selectAdditionalIssueFieldClickEvent.getChatId();
    IssueCreationDto issueCreationDto = selectAdditionalIssueFieldClickEvent.getIssueCreationDto();
    Locale locale = localeManager.getLocaleFor(currentUser);

    LinkedHashMap<Field, String> nonRequiredFields =
        getIssueCreationFieldsValues(
            projectManager.getProjectObj(issueCreationDto.getProjectId()),
            issueTypeManager.getIssueType(issueCreationDto.getIssueTypeId()),
            null,
            null,
            IssueFieldsFilterEnum.NON_REQUIRED);

    myteamApiClient.answerCallbackQuery(selectAdditionalIssueFieldClickEvent.getQueryId());

    Optional<Field> field =
        nonRequiredFields.keySet().stream().filter(f -> f.getId().equals(fieldId)).findAny();
    if (field.isPresent()) {
      myteamApiClient.editMessageText(
          chatId,
          selectAdditionalIssueFieldClickEvent.getMsgId(),
          String.format(
              "%s\n%s: -",
              messageFormatter.createInsertFieldMessage(locale, field.get(), issueCreationDto),
              field.get().getName()),
          null);
      chatsStateMap.put(
          chatId, ChatState.buildIssueFieldFillingState(issueCreationDto, field.get()));
    }
  }

  @Subscribe
  public void onNewAdditionalFieldMessageEvent(
      NewAdditionalFieldMessageEvent newAdditionalFieldMessageEvent)
      throws UnirestException, IOException, MyteamServerErrorException {

    ApplicationUser currentUser =
        userData.getUserByMrimLogin(newAdditionalFieldMessageEvent.getUserId());
    if (currentUser == null) {
      // TODO unauthorized
      return;
    }
    String chatId = newAdditionalFieldMessageEvent.getChatId();
    IssueCreationDto issueCreationDto = newAdditionalFieldMessageEvent.getIssueCreationDto();
    String currentFieldValueStr = newAdditionalFieldMessageEvent.getFieldValue();
    Locale locale = localeManager.getLocaleFor(currentUser);

    issueCreationDto
        .getRequiredIssueCreationFieldValues()
        .put(newAdditionalFieldMessageEvent.getField(), currentFieldValueStr);

    chatsStateMap.put(chatId, ChatState.buildIssueCreationConfirmState(issueCreationDto));

    IssueService.CreateValidationResult issueValidationResult =
        validateIssueWithGivenFields(currentUser, issueCreationDto);
    if (issueValidationResult.isValid()) {
      myteamApiClient.sendMessageText(
          chatId,
          i18nResolver.getText(
              locale,
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.additionalField.success",
              newAdditionalFieldMessageEvent.getField().getName()),
          messageFormatter.getIssueCreationConfirmButtons(currentUser));
    } else {
      issueCreationDto
          .getRequiredIssueCreationFieldValues()
          .remove(newAdditionalFieldMessageEvent.getField());
      myteamApiClient.sendMessageText(
          chatId,
          String.join(
              "\n",
              i18nResolver.getText(
                  locale,
                  "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.additionalField.validationError"),
              messageFormatter.stringifyMap(issueValidationResult.getErrorCollection().getErrors()),
              messageFormatter.stringifyCollection(
                  locale, issueValidationResult.getErrorCollection().getErrorMessages())),
          messageFormatter.getIssueCreationConfirmButtons(currentUser));
    }
  }

  @Subscribe
  public void onCancelAdditionalFieldClickEvent(
      CancelAdditionalFieldClickEvent cancelAdditionalFieldClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {
    ApplicationUser currentUser =
        userData.getUserByMrimLogin(cancelAdditionalFieldClickEvent.getUserId());
    if (currentUser == null) {
      return;
    }
    String chatId = cancelAdditionalFieldClickEvent.getChatId();
    IssueCreationDto issueCreationDto = cancelAdditionalFieldClickEvent.getIssueCreationDto();
    Locale locale = localeManager.getLocaleFor(currentUser);
    chatsStateMap.put(chatId, ChatState.buildIssueCreationConfirmState(issueCreationDto));
    myteamApiClient.answerCallbackQuery(cancelAdditionalFieldClickEvent.getQueryId());
    myteamApiClient.editMessageText(
        chatId,
        cancelAdditionalFieldClickEvent.getMsgId(),
        i18nResolver.getText(
            locale,
            "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.issueCreationConfirmation"),
        messageFormatter.getIssueCreationConfirmButtons(currentUser));
  }

  @Subscribe
  public void onCreateIssueConfirmClickEvent(
      CreateIssueConfirmClickEvent createIssueConfirmClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {

    ApplicationUser currentUser =
        userData.getUserByMrimLogin(createIssueConfirmClickEvent.getUserId());
    if (currentUser == null) {
      // TODO unauthorized
      return;
    }
    String chatId = createIssueConfirmClickEvent.getChatId();
    IssueCreationDto currentIssueCreationDto = createIssueConfirmClickEvent.getIssueCreationDto();
    Locale locale = localeManager.getLocaleFor(currentUser);
    JiraThreadLocalUtils.preCall();
    try {
      // then user filled all new issue fields which are required

      IssueService.CreateValidationResult issueValidationResult =
          validateIssueWithGivenFields(currentUser, currentIssueCreationDto);
      myteamApiClient.answerCallbackQuery(createIssueConfirmClickEvent.getQueryId());
      if (issueValidationResult.isValid()) {
        MutableIssue createdIssue =
            issueService.create(currentUser, issueValidationResult).getIssue();
        String createdIssueLink = messageFormatter.createIssueLink(createdIssue);
        myteamApiClient.sendMessageText(
            chatId,
            i18nResolver.getText(
                locale,
                "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.issueCreated",
                createdIssueLink));
      } else {
        myteamApiClient.sendMessageText(
            chatId,
            String.join(
                "\n",
                i18nResolver.getText(
                    locale,
                    "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.validationError"),
                messageFormatter.stringifyMap(
                    issueValidationResult.getErrorCollection().getErrors()),
                messageFormatter.stringifyCollection(
                    locale, issueValidationResult.getErrorCollection().getErrorMessages())));
      }
    } finally {
      JiraThreadLocalUtils.postCall();
    }
  }

  /**
   * Get a map of required during creation issue fields by selected project and issue type Algo: 1)
   * Getting all fields which shown on CREATE_ISSUE_OPERATION fields screen 2) All fields must be
   * required in FieldLayout => filtration 3) All custom fields must have correct context scope to
   * be shown on issue creation form
   *
   * @param project - selected project
   * @param issueType - selected issue type
   * @param includedFieldIds - fields which must be included in result map no matter required them
   *     or not
   * @param excludedFieldIds - fields which must be excluded from result map no matter required them
   *     or not
   * @param issueFieldsFilter - which fields needs to be filtered
   * @return LinkedHashMap of field and empty string value
   */
  private LinkedHashMap<Field, String> getIssueCreationFieldsValues(
      Project project,
      IssueType issueType,
      Set<String> includedFieldIds,
      Set<String> excludedFieldIds,
      IssueFieldsFilterEnum issueFieldsFilter) {
    // getting (selectedProject, selectedIssueType) fields configuration
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
                              || includedFieldIds != null && includedFieldIds.contains(fieldId);
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
                          (e) ->
                              messageFormatter.mapUserInputStringToFieldValue(
                                  projectId, e.getKey(), e.getValue()))));
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
