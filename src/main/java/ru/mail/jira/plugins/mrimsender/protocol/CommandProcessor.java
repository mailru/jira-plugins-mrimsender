package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.issue.worklog.WorklogInputParameters;
import com.atlassian.jira.bc.issue.worklog.WorklogInputParametersImpl;
import com.atlassian.jira.bc.issue.worklog.WorklogResult;
import com.atlassian.jira.bc.issue.worklog.WorklogService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.StepDescriptor;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import ru.mail.jira.plugins.commons.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandProcessor extends Thread {
    private static final Locale DEFAULT_LOCALE = new Locale("ru");
    private static final String ISSUES_JQL = "assignee = currentUser() AND status not in (Closed, Done, Ready)";

    private static final Logger log = Logger.getLogger(CommandProcessor.class);

    private final CommentService commentService = ComponentAccessor.getComponent(CommentService.class);
    private final I18nHelper.BeanFactory i18nHelperFactory = ComponentAccessor.getI18nHelperFactory();
    private final IssueService issueService = ComponentAccessor.getIssueService();
    private final IssueTypeSchemeManager issueTypeSchemeManager = ComponentAccessor.getIssueTypeSchemeManager();
    private final JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
    private final ProjectService projectService = ComponentAccessor.getComponent(ProjectService.class);
    private final WorkflowManager workflowManager = ComponentAccessor.getWorkflowManager();
    private final WorklogService worklogService = ComponentAccessor.getComponent(WorklogService.class);

    private final String message;
    private final String fromEmail;
    private I18nHelper i18n;
    private ApplicationUser fromUser;

    public static void processMessage(String email, String message) {
        CommandProcessor commandProcessor = new CommandProcessor(message, email);
        commandProcessor.start();
    }

    private CommandProcessor(String message, String fromEmail) {
        this.message = message.trim();
        this.fromEmail = fromEmail;
        this.i18n = i18nHelperFactory.getInstance(DEFAULT_LOCALE);
    }

    @Override
    public void run() {
        try {
            // Check that message is not empty
            if (StringUtils.isEmpty(message))
                return;

            // Find the corresponding user
            fromUser = UserSearcher.INSTANCE.getUserByMrimLogin(fromEmail);
            if (fromUser == null)
                throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.unauthorized"));
            i18n = i18nHelperFactory.getInstance(fromUser);

            ApplicationUser storedUser = jiraAuthenticationContext.getUser();
            try {
                jiraAuthenticationContext.setLoggedInUser(fromUser);

                // Process #help command
                if ("#help".equalsIgnoreCase(message)) {
                    MrimsenderThread.sendMessage(fromEmail, i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.help"));
                    return;
                }

                // Process #issues command
                if ("#issues".equalsIgnoreCase(message)) {
                    SearchService searchService = ComponentAccessor.getComponent(SearchService.class);

                    SearchService.ParseResult parseResult = searchService.parseQuery(fromUser, ISSUES_JQL);
                    if (!parseResult.isValid())
                        throw new Exception("Unable to parse search query");

                    SearchResults searchResults = searchService.search(fromUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter());

                    StringBuilder sb = new StringBuilder();
                    if (!searchResults.getIssues().isEmpty())
                        for (Issue issue : searchResults.getIssues()) {
                            if (sb.length() > 0)
                                sb.append("\n");
                            sb.append(String.format("[%s] %s", issue.getKey(), issue.getSummary()));
                        }
                    else
                        sb.append(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.emptySearchResult"));

                    MrimsenderThread.sendMessage(fromEmail, sb.toString());
                    return;
                }

                // Process #create command
                if (message.matches("^#create\\b.*")) {
                    Matcher commandMatcher = Pattern.compile("^#create\\s+([A-Za-z]+)\\s+(.+)").matcher(message);
                    if (!commandMatcher.matches())
                        throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.createCommand.invalidSyntax"));

                    String projectKey = commandMatcher.group(1);
                    String issueTypeNameAndSummary = commandMatcher.group(2);

                    ProjectService.GetProjectResult getProjectResult = projectService.getProjectByKey(fromUser, projectKey.toUpperCase());
                    if (!getProjectResult.isValid())
                        throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.unableToAccessProject", projectKey, CommonUtils.formatErrorCollection(getProjectResult.getErrorCollection())));
                    Project project = getProjectResult.getProject();

                    IssueType issueType = null;
                    String summary = null;

                    Collection<IssueType> issueTypes = issueTypeSchemeManager.getIssueTypesForProject(project);
                    List<String> issueTypeNames = new ArrayList<String>();
                    for (IssueType currentIssueType : issueTypes) {
                        if (StringUtils.startsWithIgnoreCase(issueTypeNameAndSummary, currentIssueType.getName())) {
                            String s = issueTypeNameAndSummary.substring(currentIssueType.getName().length());
                            if (StringUtils.isEmpty(s)) {
                                issueType = currentIssueType;
                                break;
                            }
                            Matcher paramMatcher = Pattern.compile("^\\s+(.+)").matcher(s);
                            if (paramMatcher.matches()) {
                                issueType = currentIssueType;
                                summary = paramMatcher.group(1);
                                break;
                            }
                        }
                        issueTypeNames.add(currentIssueType.getName());
                    }
                    if (issueType == null)
                        throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.createCommand.unableToFindIssueType", StringUtils.join(issueTypeNames, ", ")));
                    if (summary == null)
                        throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.createCommand.summaryIsNotSpecified"));

                    IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
                    issueInputParameters.setProjectId(project.getId());
                    issueInputParameters.setIssueTypeId(issueType.getId());
                    issueInputParameters.setReporterId(fromUser.getName());
                    issueInputParameters.setAssigneeId("-1");
                    issueInputParameters.setSummary(summary);

                    IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(fromUser, issueInputParameters);
                    if (!createValidationResult.isValid())
                        throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.createCommand.error", CommonUtils.formatErrorCollection(createValidationResult.getErrorCollection())));

                    IssueService.IssueResult issueResult = issueService.create(fromUser, createValidationResult);
                    if (!issueResult.isValid())
                        throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.createCommand.error", CommonUtils.formatErrorCollection(issueResult.getErrorCollection())));

                    MrimsenderThread.sendMessage(fromEmail, i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.createCommand.success", issueResult.getIssue().getKey()));
                    return;
                }

                // Process issue commands
                if (message.matches("^([A-Za-z]+-\\d+\\s+)+#\\b.+")) {
                    StringBuilder sb = new StringBuilder();
                    try {
                        for (IssueCommand issueCommand : parseIssueCommands()) {
                            if (sb.length() > 0)
                                sb.append("\n");
                            sb.append(issueCommand.execute(false));
                        }
                        MrimsenderThread.sendMessage(fromEmail, sb.toString());
                        return;
                    } catch (Exception e) {
                        throw new Exception(sb.append(e.getMessage()).toString(), e);
                    }
                }

                throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.commandIsNotFound"));
            } finally {
                jiraAuthenticationContext.setLoggedInUser(storedUser);
            }
        } catch (Exception e) {
            log.warn(String.format("Error processing command <%s> from <%s>", message, fromEmail), e);
            MrimsenderThread.sendMessage(fromEmail, e.getMessage());
        }
    }

    private List<ActionDescriptor> getAvailableActions(Issue issue) {
        JiraWorkflow jiraWorkflow = workflowManager.getWorkflow(issue);
        StepDescriptor stepDescriptor = jiraWorkflow.getLinkedStep(issue.getStatusObject());
        //noinspection unchecked
        return stepDescriptor.getActions();
    }

    private ActionDescriptor getActionDescriptor(Issue issue, String actionNamePrefix) throws Exception {
        List<ActionDescriptor> likelyActions = new ArrayList<ActionDescriptor>();
        for (ActionDescriptor actionDescriptor : getAvailableActions(issue))
            if (StringUtils.startsWithIgnoreCase(actionDescriptor.getName(), actionNamePrefix))
                likelyActions.add(actionDescriptor);

        switch (likelyActions.size()) {
            case 0:
                throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.noActionsFound", actionNamePrefix, issue.getKey(), issue.getStatusObject().getNameTranslation(i18n)));
            case 1:
                return likelyActions.get(0);
            default:
                throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.tooManyActionsFound", actionNamePrefix, issue.getKey(), issue.getStatusObject().getNameTranslation(i18n), StringUtils.join(likelyActions, ", ")));
        }
    }

    private List<IssueCommand> parseIssueCommands() throws Exception {
        String[] tokens = message.split("\\s+#\\b");

        // Parse issues list
        String[] issueKeys = tokens[0].split("\\s+");
        List<Issue> issues = new ArrayList<Issue>(issueKeys.length);
        for (String issueKey : issueKeys) {
            IssueService.IssueResult issueResult = issueService.getIssue(fromUser, issueKey);
            if (!issueResult.isValid())
                throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.unableToAccessIssue", issueKey, CommonUtils.formatErrorCollection(issueResult.getErrorCollection())));
            issues.add(issueResult.getIssue());
        }

        // Parse commands
        boolean hasWorkflowCommand = false;
        List<IssueCommand> result = new ArrayList<IssueCommand>();
        for (int i = 1; i < tokens.length; i++) {
            String[] commandTokens = tokens[i].split("\\s+", 2);
            String commandName = "#" + commandTokens[0];
            String commandParams = commandTokens.length < 2 ? "" : commandTokens[1];

            if ("#view".equalsIgnoreCase(commandName)) {

                for (Issue issue : issues)
                    result.add(new ViewCommand(issue));

            } else if ("#comment".equalsIgnoreCase(commandName)) {

                if (StringUtils.isEmpty(commandParams))
                    throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.commentTextIsNotSpecified"));

                for (Issue issue : issues)
                    result.add(new CommentCommand(issue, commandParams));

            } else if ("#assign".equalsIgnoreCase(commandName)) {

                if (StringUtils.isEmpty(commandParams))
                    throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.assigneeIsNotSpecified"));

                ApplicationUser assignee = UserSearcher.INSTANCE.getUserByEmail(commandParams);
                if (assignee == null)
                    throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.assigneeIsNotFound", commandParams));

                for (Issue issue : issues)
                    result.add(new AssignCommand(issue, assignee));

            } else if ("#time".equalsIgnoreCase(commandName)) {

                Matcher matcher = Pattern.compile("(((\\d+)\\s*[wdhm]\\b\\s*)+)(.*)").matcher(commandParams);
                if (!matcher.matches())
                    throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.timeIsNotSpecified"));

                for (Issue issue : issues)
                    result.add(new TimeCommand(issue, matcher.group(1), matcher.group(4)));

            } else {

                if (hasWorkflowCommand)
                    throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.tooManyWorkflowCommands"));
                hasWorkflowCommand = true;

                String workflowCommand = commandName.substring(1).replaceAll("-", " ");
                for (Issue issue : issues) {
                    ActionDescriptor actionDescriptor = getActionDescriptor(issue, workflowCommand);
                    result.add(new WorkflowCommand(issue, actionDescriptor, commandParams));
                }

            }
        }

        return result;
    }

    private abstract class IssueCommand {
        final Issue issue;

        IssueCommand(Issue issue) {
            this.issue = issue;
        }

        abstract String execute(boolean validation) throws Exception;
    }

    private class ViewCommand extends IssueCommand {
        ViewCommand(Issue issue) {
            super(issue);
        }

        @Override
        String execute(boolean validation) throws Exception {
            if (validation)
                return null;

            List<String> availableWorkflowCommands = new ArrayList<String>();
            for (ActionDescriptor actionDescriptor : getAvailableActions(issue))
                availableWorkflowCommands.add("#" + actionDescriptor.getName().replaceAll(" ", "-").toLowerCase());

            StringBuilder sb = new StringBuilder(String.format("[%s] %s", issue.getKey(), issue.getSummary())).append("\n");
            sb.append(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.viewCommand.availableActions")).append(": ").append(StringUtils.join(availableWorkflowCommands, ", "));
            sb.append(new MessageFormatter(fromUser).formatSystemFields(issue));
            return sb.toString();
        }
    }

    private class CommentCommand extends IssueCommand {
        final String commentText;

        CommentCommand(Issue issue, String commentText) throws Exception {
            super(issue);
            this.commentText = commentText;
            execute(true);
        }

        @Override
        String execute(boolean validation) throws Exception {
            ErrorCollection errorCollection = new SimpleErrorCollection();

            if (validation) {
                commentService.hasPermissionToCreate(fromUser, issue, errorCollection);
                if (errorCollection.hasAnyErrors())
                    throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.commentCommand.error", issue.getKey(), CommonUtils.formatErrorCollection(errorCollection)));

                commentService.isValidCommentBody(commentText, errorCollection);
                if (errorCollection.hasAnyErrors())
                    throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.commentCommand.error", issue.getKey(), CommonUtils.formatErrorCollection(errorCollection)));

                return null;
            } else {
                commentService.create(fromUser, issue, commentText, true, errorCollection);
                if (errorCollection.hasAnyErrors())
                    throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.commentCommand.error", issue.getKey(), CommonUtils.formatErrorCollection(errorCollection)));

                return i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.commentCommand.success", issue.getKey());
            }
        }
    }

    private class AssignCommand extends IssueCommand {
        final ApplicationUser assignee;

        AssignCommand(Issue issue, ApplicationUser assignee) throws Exception {
            super(issue);
            this.assignee = assignee;
            execute(true);
        }

        @Override
        String execute(boolean validation) throws Exception {
            IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
            issueInputParameters.setAssigneeId(assignee.getName());

            IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(fromUser, issue.getId(), issueInputParameters);
            if (!updateValidationResult.isValid())
                throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.assignCommand.error", issue.getKey(), assignee.getDisplayName(), CommonUtils.formatErrorCollection(updateValidationResult.getErrorCollection())));

            if (validation)
                return null;

            IssueService.IssueResult issueResult = issueService.update(fromUser, updateValidationResult);
            if (!issueResult.isValid())
                throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.assignCommand.error", issue.getKey(), assignee.getDisplayName(), CommonUtils.formatErrorCollection(issueResult.getErrorCollection())));

            return i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.assignCommand.success", issue.getKey(), assignee.getDisplayName());
        }
    }

    private class TimeCommand extends IssueCommand {
        final String timeSpent;
        final String commentText;

        TimeCommand(Issue issue, String timeSpent, String commentText) throws Exception {
            super(issue);
            this.timeSpent = timeSpent;
            this.commentText = commentText;
            execute(true);
        }

        @Override
        String execute(boolean validation) throws Exception {
            JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(fromUser);

            WorklogInputParameters params = WorklogInputParametersImpl.builder().
                    issue(issue).timeSpent(timeSpent).startDate(new Date()).comment(commentText).build();

            WorklogResult worklogResult = worklogService.validateCreate(jiraServiceContext, params);
            if (worklogResult == null)
                throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.timeCommand.error", issue.getKey()));

            if (validation)
                return null;

            Worklog worklog = worklogService.createAndAutoAdjustRemainingEstimate(jiraServiceContext, worklogResult, true);
            if (worklog == null)
                throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.timeCommand.error", issue.getKey()));

            return i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.timeCommand.success", issue.getKey());
        }
    }

    private class WorkflowCommand extends IssueCommand {
        final ActionDescriptor actionDescriptor;
        final String commentText;

        WorkflowCommand(Issue issue, ActionDescriptor actionDescriptor, String commentText) throws Exception {
            super(issue);
            this.actionDescriptor = actionDescriptor;
            this.commentText = commentText;
            execute(true);
        }

        @Override
        String execute(boolean validation) throws Exception {
            IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
            if (StringUtils.isNotEmpty(commentText))
                issueInputParameters.setComment(commentText);

            IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(fromUser, issue.getId(), actionDescriptor.getId(), issueInputParameters);
            if (!transitionValidationResult.isValid())
                throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.workflowCommand.error", issue.getKey(), actionDescriptor.getName(), CommonUtils.formatErrorCollection(transitionValidationResult.getErrorCollection())));

            if (validation)
                return null;

            IssueService.IssueResult issueResult = issueService.transition(fromUser, transitionValidationResult);
            if (!issueResult.isValid())
                throw new Exception(i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.workflowCommand.error", issue.getKey(), actionDescriptor.getName(), CommonUtils.formatErrorCollection(issueResult.getErrorCollection())));

            return i18n.getText("ru.mail.jira.plugins.mrimsender.commandProcessor.workflowCommand.success", issue.getKey(), actionDescriptor.getName());
        }
    }
}
