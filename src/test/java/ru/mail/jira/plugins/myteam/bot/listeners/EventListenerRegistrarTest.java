/* (C)2024 */
package ru.mail.jira.plugins.myteam.bot.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.atlassian.event.api.EventPublisher;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("NullAway")
class EventListenerRegistrarTest {

  @Mock(lenient = true)
  private EventPublisher eventPublisher;

  @Test
  void afterPropertiesSetRegisterEventListener() throws Exception {
    // GIVEN
    IEventListener eventListener = new TestListener();
    EventListenerRegistrar eventListenerRegistrar =
        new EventListenerRegistrar(List.of(eventListener), eventPublisher);

    // WHEN
    eventListenerRegistrar.afterPropertiesSet();

    // THEN
    verify(eventPublisher).register(eq(eventListener));
  }

  @Test
  void afterPropertiesSetRegisterEventListenerEmptyList() throws Exception {
    // GIVEN
    EventListenerRegistrar eventListenerRegistrar =
        new EventListenerRegistrar(List.of(), eventPublisher);

    // WHEN
    eventListenerRegistrar.afterPropertiesSet();

    // THEN
    verify(eventPublisher, never()).register(any(IEventListener.class));
  }

  @Test
  void destroyUnregisterEventListener() throws Exception {
    // GIVEN
    IEventListener eventListener = new TestListener();
    EventListenerRegistrar eventListenerRegistrar =
        new EventListenerRegistrar(List.of(eventListener), eventPublisher);

    // WHEN
    eventListenerRegistrar.destroy();

    // THEN
    verify(eventPublisher).unregister(eq(eventListener));
  }

  @Test
  void destroyUnregisterEventListenerEmptyList() throws Exception {
    // GIVEN
    EventListenerRegistrar eventListenerRegistrar =
        new EventListenerRegistrar(List.of(), eventPublisher);

    // WHEN
    eventListenerRegistrar.destroy();

    // THEN
    verify(eventPublisher, never()).unregister(any(IEventListener.class));
  }

  private static class TestListener implements IEventListener {}
}
