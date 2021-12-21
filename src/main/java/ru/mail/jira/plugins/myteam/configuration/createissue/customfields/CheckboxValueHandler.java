/* (C)2021 */
package ru.mail.jira.plugins.myteam.configuration.createissue.customfields;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.customfields.impl.MultiSelectCFType;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.project.Project;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.*;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.StateActionRuleType;

public class CheckboxValueHandler implements CreateIssueFieldValueHandler {

  private final OptionsManager optionsManager;
  private final I18nResolver i18nResolver;
  private static final String delimiter = ", ";

  public CheckboxValueHandler(@ComponentImport I18nResolver i18nResolver) {
    this.i18nResolver = i18nResolver;
    optionsManager = ComponentAccessor.getOptionsManager();
  }

  @Override
  public String getClassName() {
    return MultiSelectCFType.class.getName();
  }

  @Override
  public String getInsertFieldMessage(Field field, Locale locale) {
    i18nResolver.getText("aa");
    return String.format("Ввведите значение для поля %s", field.getName());
  }

  @Override
  public List<List<InlineKeyboardMarkupButton>> getButtons(
      Field field, String value, Locale locale) {

    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    Options options = getOptions((CustomField) field);

    List<String> values = Arrays.asList(value.split(delimiter));

    options.forEach(
        option -> {
          InlineKeyboardMarkupButton optionButton =
              InlineKeyboardMarkupButton.buildButtonWithoutUrl(
                  String.format(
                      "%s %s", option.getValue(), values.contains(option.getValue()) ? "☑️" : "️"),
                  String.join(
                      "-",
                      StateActionRuleType.EditIssueCreationValue.getName(),
                      option.getValue()));
          List<InlineKeyboardMarkupButton> newButtonsRow = new ArrayList<>(1);
          newButtonsRow.add(optionButton);
          buttons.add(newButtonsRow);
        });

    InlineKeyboardMarkupButton optionButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                locale, "ru.mail.jira.plugins.myteam.mrimsenderEventListener.quickViewButton.add"),
            String.join(
                "-",
                StateActionRuleType.SelectIssueCreationValue.getName(),
                String.format("%s", String.join(delimiter, values))));

    List<InlineKeyboardMarkupButton> newButtonsRow = new ArrayList<>(1);
    newButtonsRow.add(optionButton);
    buttons.add(newButtonsRow);

    return buttons;
  }

  @Override
  public String updateValue(String oldValue, String newValue) {

    if (oldValue.equals(newValue)) return newValue;

    List<String> values = new ArrayList<>(Arrays.asList(oldValue.split(delimiter)));

    if (values.contains(newValue)) {
      values.remove(newValue);
    } else {
      if (oldValue.length() == 0) {
        return newValue;
      } else {
        values.add(newValue);
      }
    }
    return String.join(delimiter, values);
  }

  @Override
  public String[] getValueAsArray(String value, Field field, Project project, Locale locale) {
    List<String> values = new ArrayList<>(Arrays.asList(value.split(delimiter)));

    Options options = getOptions((CustomField) field);

    return values.stream()
        .map(v -> String.valueOf(getOption(options, v).getOptionId()))
        .toArray(String[]::new);
  }

  private Options getOptions(CustomField field) {
    Options options = null;

    if (field != null) {
      List<FieldConfigScheme> schemes = field.getConfigurationSchemes();
      if (!schemes.isEmpty()) {
        FieldConfigScheme sc = schemes.get(0);
        Map configs = sc.getConfigsByConfig();
        if (configs != null && !configs.isEmpty()) {
          FieldConfig config = (FieldConfig) configs.keySet().iterator().next();
          options = optionsManager.getOptions(config);
        }
      }
    }
    return options;
  }

  public Option getOption(Options options, String value) {
    Option result = null;
    Option opt;
    for (Option object : options) {
      opt = object;
      if (opt.getValue().equals(value)) {
        result = object;
        break;
      }
    }
    return result;
  }
}
