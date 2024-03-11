/* (C)2024 */
package ru.mail.jira.plugins.myteam.bot.listeners;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventListenerRegistrar implements InitializingBean, DisposableBean {
  private final List<IEventListener> eventListeners;

  private final EventPublisher eventPublisher;

  @Autowired
  public EventListenerRegistrar(
      final List<IEventListener> eventListeners,
      @ComponentImport final EventPublisher eventPublisher) {
    this.eventListeners = Collections.unmodifiableList(eventListeners);
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    eventListeners.forEach(eventPublisher::register);
  }

  @Override
  public void destroy() throws Exception {
    eventListeners.forEach(eventPublisher::unregister);
  }
}
