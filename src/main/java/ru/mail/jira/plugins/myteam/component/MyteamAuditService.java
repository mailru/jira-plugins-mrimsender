/* (C)2022 */
package ru.mail.jira.plugins.myteam.component;

import com.atlassian.audit.api.AuditService;
import com.atlassian.audit.entity.AuditEvent;
import com.atlassian.audit.entity.AuditResource;
import com.atlassian.audit.entity.AuditType;
import com.atlassian.audit.entity.ChangedValue;
import com.atlassian.audit.entity.CoverageArea;
import com.atlassian.audit.entity.CoverageLevel;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.CommonUtils;
import ru.mail.jira.plugins.myteam.accessrequest.model.AccessRequestHistory;

@Component
public class MyteamAuditService {
  public static final String MYTEAM_PLUGIN_LEVEL = "ru.mail.jira.plugins.myteam.auditing.level";

  private final AuditService auditService;
  private final UserManager userManager;

  public MyteamAuditService(@ComponentImport AuditService auditService, UserManager userManager) {
    this.auditService = auditService;
    this.userManager = userManager;
  }

  private AuditType auditType(String action) {
    return AuditType.fromI18nKeys(
            CoverageArea.ECOSYSTEM, CoverageLevel.BASE, MYTEAM_PLUGIN_LEVEL, action)
        .build();
  }

  public void userSendAccessRequestContragent(
      ApplicationUser requester, Issue issue, AccessRequestHistory history) {
    AuditEvent.Builder builder =
        AuditEvent.builder(auditType("ru.mail.jira.plugins.contracts.auditing.accessRequest"));
    builder.affectedObject(
        AuditResource.builder(requester.getDisplayName(), "user")
            .id(String.valueOf(requester.getDisplayName()))
            .build());
    builder.affectedObject(
        AuditResource.builder(issue.getKey(), "issue").id(String.valueOf(issue.getId())).build());
    List<ChangedValue> changedValues = new ArrayList<>();
    changedValues.add(
        ChangedValue.fromI18nKeys("ru.mail.jira.plugins.contracts.auditing.accessRequest.users")
            .from(null)
            .to(formatUsers(history.getUserKeys()))
            .build());
    changedValues.add(
        ChangedValue.fromI18nKeys("ru.mail.jira.plugins.contracts.auditing.accessRequest.message")
            .from(null)
            .to(history.getMessage())
            .build());
    builder.appendChangedValues(changedValues);
    auditService.audit(builder.build());
  }

  private String formatUsers(String userKeys) {
    return CommonUtils.split(userKeys).stream()
        .map(userManager::getUserByKey)
        .filter(Objects::nonNull)
        .map(user -> String.format("%s (%s)", user.getDisplayName(), user.getEmailAddress()))
        .collect(Collectors.joining(", "));
  }
}
