/* (C)2020 */
package ru.mail.jira.plugins.myteam.bot;

import com.atlassian.jira.cluster.ClusterMessageConsumer;
import com.atlassian.jira.cluster.ClusterMessagingService;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/** Service which allow to stop or restart all bots which are running on a cluster */
@Service
@Slf4j
@ExportAsService(LifecycleAware.class)
public class BotsOrchestrationService implements LifecycleAware {
  private static final String BOT_LIFECYCLE_CHANNEL = "ru.mail.jira.myteam";
  private static final String BOT_RESTART_MESSAGE = "restart";
  private static final String BOT_STOP_MESSAGE = "stop";

  private final ClusterMessagingService clusterMessagingService;
  private final MyteamBot myteamBot;
  private final MessageConsumer messageConsumer;
  private final ThreadPoolTaskExecutor jiraBotTaskExecutor;

  @Autowired
  public BotsOrchestrationService(
      @ComponentImport ClusterMessagingService clusterMessagingService,
      MyteamBot myteamBot,
      ThreadPoolTaskExecutor jiraBotTaskExecutor) {
    this.clusterMessagingService = clusterMessagingService;
    this.myteamBot = myteamBot;
    this.jiraBotTaskExecutor = jiraBotTaskExecutor;
    this.messageConsumer = new MessageConsumer();
  }

  @Override
  public void onStart() {
    this.clusterMessagingService.registerListener(BOT_LIFECYCLE_CHANNEL, this.messageConsumer);
  }

  @Override
  public void onStop() {
    this.clusterMessagingService.unregisterListener(BOT_LIFECYCLE_CHANNEL, this.messageConsumer);
  }

  public void restartAll() {
    log.debug("Sending restart bot message for all nodes.");
    clusterMessagingService.sendRemote(BOT_LIFECYCLE_CHANNEL, BOT_RESTART_MESSAGE);
    // TODO возможно это не очень верно и стоит отображать ошибку подключения пользователю прямо на
    // страницу после тайм аута
    // TODO можно также запускать это в отдельном потоке, чтобы у пользователя на время операции не
    // подвисала страница настройки
    myteamBot.restartBot();
  }

  public void stopAll() {
    log.debug("Sending stop bot message for all nodes.");
    clusterMessagingService.sendRemote(BOT_LIFECYCLE_CHANNEL, BOT_STOP_MESSAGE);
    myteamBot.stopBot();
  }

  private class MessageConsumer implements ClusterMessageConsumer {
    @Override
    public void receive(String channel, String message, String senderId) {
      if (BOT_LIFECYCLE_CHANNEL.equals(channel)) {
        if (BOT_RESTART_MESSAGE.equals(message)) {
          jiraBotTaskExecutor.execute(myteamBot::restartBot);
        }
        if (BOT_STOP_MESSAGE.equals(message)) {
          jiraBotTaskExecutor.execute(myteamBot::stopBot);
        }
      }
    }
  }
}
