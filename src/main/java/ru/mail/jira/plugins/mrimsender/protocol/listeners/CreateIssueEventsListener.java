package ru.mail.jira.plugins.mrimsender.protocol.listeners;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.eventbus.Subscribe;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.protocol.ChatState;
import ru.mail.jira.plugins.mrimsender.protocol.ChatStateMapping;
import ru.mail.jira.plugins.mrimsender.protocol.IssueCreationDto;
import ru.mail.jira.plugins.mrimsender.protocol.MessageFormatter;
import ru.mail.jira.plugins.mrimsender.protocol.events.SelectedIssueTypeMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.SelectedProjectMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.CreateIssueClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.NextIssueTypesPageClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.NextProjectsPageClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.PrevIssueTypesPageClickEvent;
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
    private final MessageFormatter messageFormatter;
    private final I18nResolver i18nResolver;
    private final LocaleManager localeManager;
    private final PermissionManager permissionManager;
    private final ProjectManager projectManager;
    private final IssueTypeSchemeManager issueTypeSchemeManager;
    private final IssueTypeScreenSchemeManager issueTypeScreenSchemeManager;
    private final FieldLayoutManager fieldLayoutManager;
    private final FieldManager fieldManager;

    public CreateIssueEventsListener(ChatStateMapping chatStateMapping,
                                     IcqApiClient icqApiClient,
                                     UserData userData,
                                     MessageFormatter messageFormatter,
                                     I18nResolver i18nResolver,
                                     LocaleManager localeManager,
                                     PermissionManager permissionManager,
                                     ProjectManager projectManager,
                                     IssueTypeSchemeManager issueTypeSchemeManager,
                                     IssueTypeScreenSchemeManager issueTypeScreenSchemeManager,
                                     FieldLayoutManager fieldLayoutManager,
                                     FieldManager fieldManager) {
        this.chatsStateMap = chatStateMapping.getChatsStateMap();
        this.icqApiClient = icqApiClient;
        this.userData = userData;
        this.messageFormatter = messageFormatter;
        this.i18nResolver = i18nResolver;
        this.localeManager = localeManager;
        this.permissionManager = permissionManager;
        this.projectManager = projectManager;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
        this.issueTypeScreenSchemeManager = issueTypeScreenSchemeManager;
        this.fieldLayoutManager = fieldLayoutManager;
        this.fieldManager = fieldManager;
    }


    @Subscribe
    public void onCreateIssueClickEvent(CreateIssueClickEvent createIssueClickEvent) throws UnirestException, IOException {
        log.debug("CreateIssueClickEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(createIssueClickEvent.getUserId());
        String chatId = createIssueClickEvent.getChatId();
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            icqApiClient.answerCallbackQuery(createIssueClickEvent.getQueryId());
            List<Project> projectList = projectManager.getProjects();
            List<Project> firstPageProjectsInterval = projectList.stream().limit(LIST_PAGE_SIZE).collect(Collectors.toList());
            icqApiClient.sendMessageText(chatId,
                                         messageFormatter.createSelectProjectMessage(locale, firstPageProjectsInterval, 0, projectList.size()),
                                         messageFormatter.getSelectProjectMessageButtons(locale, false, projectList.size() > LIST_PAGE_SIZE));
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

            List<Project> projectsList = projectManager.getProjects();
            List<Project> nextProjectsInterval = projectsList.stream()
                                                             .skip(nextPageStartIndex)
                                                             .limit(LIST_PAGE_SIZE)
                                                             .collect(Collectors.toList());

            icqApiClient.answerCallbackQuery(nextProjectsPageClickEvent.getQueryId());
            icqApiClient.editMessageText(chatId,
                                         nextProjectsPageClickEvent.getMsgId(),
                                         messageFormatter.createSelectProjectMessage(locale, nextProjectsInterval, nextPageNumber, projectsList.size()),
                                         messageFormatter.getSelectProjectMessageButtons(locale, true, projectsList.size() > LIST_PAGE_SIZE + nextPageStartIndex));
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

            List<Project> projectsList = projectManager.getProjects();
            List<Project> prevProjectsInterval = projectsList.stream()
                                                             .skip(prevPageStartIndex)
                                                             .limit(LIST_PAGE_SIZE)
                                                             .collect(Collectors.toList());
            icqApiClient.answerCallbackQuery(prevProjectsPageClickEvent.getQueryId());
            icqApiClient.editMessageText(chatId,
                                         prevProjectsPageClickEvent.getMsgId(),
                                         messageFormatter.createSelectProjectMessage(locale, prevProjectsInterval, prevPageNumber, projectsList.size()),
                                         messageFormatter.getSelectProjectMessageButtons(locale, prevPageStartIndex >= LIST_PAGE_SIZE, true));
            chatsStateMap.put(chatId, ChatState.buildProjectSelectWaitingState(prevPageNumber, prevProjectsPageClickEvent.getIssueCreationDto()));
        }
        log.debug("PrevProjectsPageClickEvent handling finished");
    }

    @Subscribe
    public void onSelectedProjectMessageEvent(SelectedProjectMessageEvent selectedProjectMessageEvent) throws IOException, UnirestException {
        IssueCreationDto currentIssueCreationDto = selectedProjectMessageEvent.getIssueCreationDto();
        ApplicationUser currentUser = userData.getUserByMrimLogin(selectedProjectMessageEvent.getUserId());
        if (currentUser != null) {
            String chatId = selectedProjectMessageEvent.getChatId();
            Locale locale = localeManager.getLocaleFor(currentUser);
            String selectedProjectKey = selectedProjectMessageEvent.getSelectedProjectKey();
            Project selectedProject = projectManager.getProjectByCurrentKey(selectedProjectKey);
            if (selectedProject != null && permissionManager.hasPermission(ProjectPermissions.CREATE_ISSUES, selectedProject, currentUser)) {
                // Project selected, sending user select IssueType message
                currentIssueCreationDto.setProjectKey(selectedProjectKey);
                List<IssueType> projectIssueTypes = issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(selectedProject)
                                                                          .stream()
                                                                          .sorted()
                                                                          .collect(Collectors.toList());
                icqApiClient.sendMessageText(chatId,
                                             messageFormatter.createSelectIssueTypeMessage(locale,
                                                                                           projectIssueTypes.stream().limit(LIST_PAGE_SIZE).collect(Collectors.toList()),
                                                                                           0,
                                                                                           projectIssueTypes.size()),
                                             messageFormatter.getSelectIssueTypeMessageButtons(locale, false, projectIssueTypes.size() > LIST_PAGE_SIZE));
                chatsStateMap.put(chatId, ChatState.buildIssueTypeSelectWaitingState(0, currentIssueCreationDto));
            } else {
                // TODO project key is not valid or user don't have permissions ... maybe suggest enter project key again ???
            }
        }
    }

    @Subscribe
    public void onNextIssueTypesPageClickEvent(NextIssueTypesPageClickEvent nextIssueTypesPageClickEvent) throws UnirestException, IOException {
        log.debug("NextIssueTypesPageClickEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(nextIssueTypesPageClickEvent.getUserId());
        if (currentUser != null) {
            int nextPageNumber = nextIssueTypesPageClickEvent.getCurrentPage() + 1;
            int nextPageStartIndex = nextPageNumber * LIST_PAGE_SIZE;
            Locale locale = localeManager.getLocaleFor(currentUser);
            String chatId = nextIssueTypesPageClickEvent.getChatId();
            IssueCreationDto currentIssueCreationDto = nextIssueTypesPageClickEvent.getIssueCreationDto();
            String selectedProjectKey = currentIssueCreationDto.getProjectKey();


            Collection<IssueType> issueTypeList = issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(projectManager.getProjectByCurrentKey(selectedProjectKey));
            List<IssueType> issueTypeListInterval = issueTypeList.stream()
                                                                 .sorted()
                                                                 .skip(nextPageStartIndex)
                                                                 .limit(LIST_PAGE_SIZE)
                                                                 .collect(Collectors.toList());
            icqApiClient.answerCallbackQuery(nextIssueTypesPageClickEvent.getQueryId());
            icqApiClient.editMessageText(chatId,
                                         nextIssueTypesPageClickEvent.getMsgId(),
                                         messageFormatter.createSelectIssueTypeMessage(locale, issueTypeListInterval, nextPageNumber, issueTypeList.size()),
                                         messageFormatter.getSelectIssueTypeMessageButtons(locale, true, issueTypeList.size() > LIST_PAGE_SIZE + nextPageStartIndex));
            chatsStateMap.put(chatId, ChatState.buildIssueTypeSelectWaitingState(nextPageNumber, currentIssueCreationDto));
        }
        log.debug("NextIssueTypesPageClickEvent handling finished");
    }

    @Subscribe
    public void onPrevIssueTypesPageClickEvent(PrevIssueTypesPageClickEvent prevIssueTypesPageClickEvent) throws UnirestException, IOException {
        log.debug("PrevIssueTypesPageClickEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(prevIssueTypesPageClickEvent.getUserId());
        if (currentUser != null) {
            int prevPageNumber = prevIssueTypesPageClickEvent.getCurrentPage() - 1;
            int prevPageStartIndex = prevPageNumber * LIST_PAGE_SIZE;
            Locale locale = localeManager.getLocaleFor(currentUser);
            String chatId = prevIssueTypesPageClickEvent.getChatId();
            IssueCreationDto currentIssueCreationDto = prevIssueTypesPageClickEvent.getIssueCreationDto();
            String selectedProjectKey = currentIssueCreationDto.getProjectKey();

            Collection<IssueType> issueTypeList = issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(projectManager.getProjectByCurrentKey(selectedProjectKey));
            List<IssueType> issueTypeListInterval = issueTypeList.stream()
                                                                 .sorted()
                                                                 .skip(prevPageStartIndex)
                                                                 .limit(LIST_PAGE_SIZE)
                                                                 .collect(Collectors.toList());
            icqApiClient.answerCallbackQuery(prevIssueTypesPageClickEvent.getQueryId());
            icqApiClient.editMessageText(chatId,
                                         prevIssueTypesPageClickEvent.getMsgId(),
                                         messageFormatter.createSelectIssueTypeMessage(locale, issueTypeListInterval, prevPageNumber, issueTypeList.size()),
                                         messageFormatter.getSelectIssueTypeMessageButtons(locale, prevPageStartIndex >= LIST_PAGE_SIZE, true));
            chatsStateMap.put(chatId, ChatState.buildIssueTypeSelectWaitingState(prevPageNumber, currentIssueCreationDto));
        }
        log.debug("PrevIssueTypesPageClickEvent handling finished");
    }


    @Subscribe
    void onSelectedIssueTypeMessageEvent(SelectedIssueTypeMessageEvent selectedIssueTypeMessageEvent) throws IOException, UnirestException {
        IssueCreationDto currentIssueCreationDto = selectedIssueTypeMessageEvent.getIssueCreationDto();
        ApplicationUser currentUser = userData.getUserByMrimLogin(selectedIssueTypeMessageEvent.getUserId());
        if (currentUser != null) {
            Project selectedProject = projectManager.getProjectByCurrentKey(currentIssueCreationDto.getProjectKey());
            String selectedIssueTypePosition = selectedIssueTypeMessageEvent.getSelectedIssueTypePosition();
            String chatId = selectedIssueTypeMessageEvent.getChatId();
            Locale locale = localeManager.getLocaleFor(currentUser);

            if (selectedProject != null && StringUtils.isNumeric(selectedIssueTypePosition)) {
                List<IssueType> selectedIssueTypeBoxed = issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(selectedProject)
                                                                               .stream()
                                                                               .sorted()
                                                                               .skip(Integer.parseInt(selectedIssueTypePosition) - 1)
                                                                               .limit(1)
                                                                               .collect(Collectors.toList());
                if (!selectedIssueTypeBoxed.isEmpty()) {
                    IssueType selectedIssueType = selectedIssueTypeBoxed.get(0);
                    currentIssueCreationDto.setIssueTypeId(selectedIssueType.getId());

                    // project and issueType fields should be excluded from needs to be filled fields
                    Set<String> excludedFieldIds = Stream.of(fieldManager.getProjectField(), fieldManager.getIssueTypeField()).map(Field::getId).collect(Collectors.toSet());
                    currentIssueCreationDto.setRequiredIssueCreationFields(getIssueCreationRequiredFields(selectedProject, selectedIssueType, excludedFieldIds));
                    // order saved here cause LinkedHashMap keySet() method  in reality returns LinkedHashSet
                    List<OrderableField> requiredFields = new ArrayList<>(currentIssueCreationDto.getRequiredIssueCreationFields().keySet());

                    if (!requiredFields.isEmpty()) {
                        // here sending new issue filling fields messsage to user
                        OrderableField currentField = requiredFields.get(0);
                        icqApiClient.sendMessageText(chatId, messageFormatter.createInsertFieldMessage(locale, currentField, currentIssueCreationDto));
                        chatsStateMap.put(chatId, ChatState.buildNewIssueFieldsFillingState(0, currentIssueCreationDto));
                    } else {
                        // TODO there is no required fields in this project issue creation ?
                    }
                } else {
                    // TODO issueType number is not valid...
                }
            } else {
                // TODO issueType number is not valid ... maybe suggest enter issue type again ???
            }
        }
    }

    private LinkedHashMap<OrderableField, Optional<String>> getIssueCreationRequiredFields(Project project, IssueType issueType, Set<String> excludedFieldIds) {

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
                                                              .filter(fieldScreenLayoutItem -> fieldLayout.getFieldLayoutItem(fieldScreenLayoutItem.getFieldId()).isRequired())
                                                              .filter(layoutItem -> !excludedFieldIds.contains(layoutItem.getFieldId()))
                                                              .map(FieldScreenLayoutItem::getOrderableField))
                                           .collect(Collectors.toMap(Function.identity(),
                                                                     field -> Optional.empty(),
                                                                     (v1, v2) -> v1,
                                                                     LinkedHashMap::new));
    }
}
