package ru.mail.jira.plugins.myteam.i18n;

// TODO: вынести в commons?
//  - мое предложение использовать интерфейс + enum для работа с i18n вместо прямой работы со строкой
public interface I18nProperty {

    String getMessageKey();
}
