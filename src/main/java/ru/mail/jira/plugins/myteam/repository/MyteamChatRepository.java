/* (C)2020 */
package ru.mail.jira.plugins.myteam.repository;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.java.ao.Query;
import org.springframework.stereotype.Repository;
import ru.mail.jira.plugins.myteam.model.MyteamChatMeta;

@Repository
public class MyteamChatRepository {

  private final ActiveObjects ao;

  public MyteamChatRepository(@ComponentImport ActiveObjects ao) {
    this.ao = ao;
  }

  public MyteamChatMeta persistChat(@Nonnull String chatId, @Nonnull String issueKey) {
    // persist new one
    MyteamChatMeta myteamChatMetaEntity = ao.create(MyteamChatMeta.class);
    myteamChatMetaEntity.setChatId(chatId);
    myteamChatMetaEntity.setIssueKey(issueKey.toUpperCase());
    myteamChatMetaEntity.save();
    return myteamChatMetaEntity;
  }

  public void deleteChatByIssueKey(@Nonnull String issueKey) {
    // delete if already exist
    ao.deleteWithSQL(MyteamChatMeta.class, "ISSUE_KEY = ?", issueKey.toUpperCase());
  }

  @Nullable
  public MyteamChatMeta findChatByIssueKey(@Nonnull String issueKey) {
    MyteamChatMeta[] myteamChatMetaEntities =
        ao.find(
            MyteamChatMeta.class, Query.select().where("ISSUE_KEY = ?", issueKey.toUpperCase()));
    if (myteamChatMetaEntities != null && myteamChatMetaEntities.length > 0) {
      return myteamChatMetaEntities[0];
    }
    return null;
  }
}
