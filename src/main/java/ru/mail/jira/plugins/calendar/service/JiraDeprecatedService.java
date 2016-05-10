package ru.mail.jira.plugins.calendar.service;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.query.Query;

import java.lang.reflect.InvocationTargetException;

public class JiraDeprecatedService {
    private final BuildUtilsInfo buildUtilsInfo;

    public final DateTimeFormatterAdapter dateTimeFormatter;
    public final IssueServiceAdapter issueService;
    public final GroupManagerAdapter groupManager;
    public final SearchServiceAdapter searchService;

    public JiraDeprecatedService(BuildUtilsInfo buildUtilsInfo, DateTimeFormatter dateTimeFormatter, IssueService issueService, GroupManager groupManager, SearchService searchService) {
        this.buildUtilsInfo = buildUtilsInfo;
        this.dateTimeFormatter = new DateTimeFormatterAdapter(dateTimeFormatter);
        this.issueService = new IssueServiceAdapter(issueService);
        this.groupManager = new GroupManagerAdapter(groupManager);
        this.searchService = new SearchServiceAdapter(searchService);
    }

    public boolean isJira7() {
        return buildUtilsInfo.getVersion().startsWith("7.");
    }

    public class SearchServiceAdapter {
        private final SearchService searchService;

        SearchServiceAdapter(SearchService searchService) {
            this.searchService = searchService;
        }

        public SearchService.ParseResult parseQuery(ApplicationUser user, String jql) {
            try {
                if (isJira7())
                    return (SearchService.ParseResult) SearchService.class.getMethod("parseQuery", ApplicationUser.class, String.class).invoke(searchService, user, jql);
                else
                    return (SearchService.ParseResult) SearchService.class.getMethod("parseQuery", User.class, String.class).invoke(searchService, ApplicationUsers.toDirectoryUser(user), jql);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public long searchCount(ApplicationUser user, Query query) throws SearchException {
            try {
                if (isJira7())
                    return (Long) SearchService.class.getMethod("searchCount", ApplicationUser.class, Query.class).invoke(searchService, user, query);
                else
                    return (Long) SearchService.class.getMethod("searchCount", User.class, Query.class).invoke(searchService, ApplicationUsers.toDirectoryUser(user), query);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public MessageSet validateQuery(ApplicationUser user, Query jql) {
            try {
                if (isJira7())
                    return (MessageSet) SearchService.class.getMethod("validateQuery", ApplicationUser.class, Query.class).invoke(searchService, user, jql);
                else
                    return (MessageSet) SearchService.class.getMethod("validateQuery", User.class, Query.class).invoke(searchService, ApplicationUsers.toDirectoryUser(user), jql);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class GroupManagerAdapter {
        private final GroupManager groupManager;

        public GroupManagerAdapter(GroupManager groupManager) {
            this.groupManager = groupManager;
        }

        public boolean isUserInGroup(ApplicationUser user, Group group) {
            try {
                if (isJira7())
                    return (Boolean) GroupManager.class.getMethod("isUserInGroup", ApplicationUser.class, Group.class).invoke(groupManager, user, group);
                else
                    return (Boolean) GroupManager.class.getMethod("isUserInGroup", User.class, Group.class).invoke(groupManager, ApplicationUsers.toDirectoryUser(user), group);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class DateTimeFormatterAdapter {
        private final DateTimeFormatter dateTimeFormatter;

        public DateTimeFormatterAdapter(DateTimeFormatter dateTimeFormatter) {
            this.dateTimeFormatter = dateTimeFormatter;
        }

        public DateTimeFormatter forUser(ApplicationUser user) {
            try {
                if (isJira7())
                    return (DateTimeFormatter) DateTimeFormatter.class.getMethod("forUser", ApplicationUser.class).invoke(dateTimeFormatter, user);
                else
                    return (DateTimeFormatter) DateTimeFormatter.class.getMethod("forUser", User.class).invoke(dateTimeFormatter, ApplicationUsers.toDirectoryUser(user));
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class IssueServiceAdapter {
        private final IssueService issueService;

        public IssueServiceAdapter(IssueService issueService) {
            this.issueService = issueService;
        }

        public boolean isEditable(Issue issue, ApplicationUser user) {
            try {
                if (isJira7())
                    return (Boolean) IssueService.class.getMethod("isEditable", Issue.class, ApplicationUser.class).invoke(issueService, issue, user);
                else
                    return (Boolean) IssueService.class.getMethod("isEditable", Issue.class, User.class).invoke(issueService, issue, ApplicationUsers.toDirectoryUser(user));
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public IssueService.IssueResult getIssue(ApplicationUser user, String issueId) {
            try {
                if (isJira7())
                    return (IssueService.IssueResult) IssueService.class.getMethod("getIssue", ApplicationUser.class, String.class).invoke(issueService, user, issueId);
                else
                    return (IssueService.IssueResult) IssueService.class.getMethod("getIssue", User.class, String.class).invoke(issueService, ApplicationUsers.toDirectoryUser(user), issueId);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public IssueService.UpdateValidationResult validateUpdate(ApplicationUser user, Long issueId, IssueInputParameters params) {
            try {
                if (isJira7())
                    return (IssueService.UpdateValidationResult) IssueService.class.getMethod("validateUpdate", ApplicationUser.class, Long.class, IssueInputParameters.class).invoke(issueService, user, issueId, params);
                else
                    return (IssueService.UpdateValidationResult) IssueService.class.getMethod("validateUpdate", User.class, Long.class, IssueInputParameters.class).invoke(issueService, ApplicationUsers.toDirectoryUser(user), issueId, params);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public IssueService.IssueResult update(ApplicationUser user, IssueService.UpdateValidationResult result) {
            try {
                if (isJira7())
                    return (IssueService.IssueResult) IssueService.class.getMethod("update", ApplicationUser.class, IssueService.UpdateValidationResult.class).invoke(issueService, user, result);
                else
                    return (IssueService.IssueResult) IssueService.class.getMethod("update", User.class, IssueService.UpdateValidationResult.class).invoke(issueService, ApplicationUsers.toDirectoryUser(user), result);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
