/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.validation.provider;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ContextProvider implements ApplicationContextAware {
  @Nullable private static ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    ContextProvider.applicationContext = applicationContext;
  }

  @Nullable
  public static Object getBean(Class cls) {
    if (ContextProvider.applicationContext != null)
      return ContextProvider.applicationContext.getBean(cls);
    else return null;
  }
}
