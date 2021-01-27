/* (C)2020 */
package ru.mail.jira.plugins.myteam.model;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.java.ao.Query;
import org.springframework.stereotype.Repository;

@Repository
public class MyteamChatRepository {

  private final ActiveObjects ao;

  public MyteamChatRepository(@ComponentImport ActiveObjects ao) {
    this.ao = ao;
  }

  public MyteamChatMetaEntity persistChat(@Nonnull String chatId, @Nonnull String issueKey) {
    // persist new one
    MyteamChatMetaEntity myteamChatMetaEntity = ao.create(MyteamChatMetaEntity.class);
    myteamChatMetaEntity.setChatId(chatId);
    myteamChatMetaEntity.setIssueKey(issueKey.toUpperCase());
    myteamChatMetaEntity.save();
    return myteamChatMetaEntity;
  }

  public void deleteChatByIssueKey(@Nonnull String issueKey) {
    // delete if already exist
    ao.deleteWithSQL(MyteamChatMetaEntity.class, "ISSUE_KEY = ?", issueKey.toUpperCase());
  }

  @Nullable
  public MyteamChatMetaEntity findChatByIssueKey(@Nonnull String issueKey) {
    MyteamChatMetaEntity[] myteamChatMetaEntities =
        ao.find(
            MyteamChatMetaEntity.class,
            Query.select().where("ISSUE_KEY = ?", issueKey.toUpperCase()));
    if (myteamChatMetaEntities != null && myteamChatMetaEntities.length > 0) {
      return myteamChatMetaEntities[0];
    }
    return null;
  }
}
