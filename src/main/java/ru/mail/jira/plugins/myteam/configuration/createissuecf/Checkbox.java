/* (C)2021 */
package ru.mail.jira.plugins.myteam.configuration.createissuecf;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.customfields.impl.AbstractCustomFieldType;
import com.atlassian.jira.issue.customfields.impl.MultiSelectCFType;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import java.util.*;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.ChatState;
import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;

public class Checkbox implements CreateIssueBaseCF {

  private final OptionsManager optionsManager;
  private final MessageFormatter messageFormatter;

  public Checkbox(MessageFormatter messageFormatter) {
    this.messageFormatter = messageFormatter;
    optionsManager = ComponentAccessor.getOptionsManager();
  }

  @Override
  public Class<? extends AbstractCustomFieldType> getCFTypeClass() {
    return MultiSelectCFType.class;
  }

  @Override
  public String getInsertFieldMessage(
      Locale locale, CustomField field, IssueCreationDto issueCreationDto, String newValue) {
    return messageFormatter.createInsertFieldMessage(locale, field, issueCreationDto);
  }

  @Override
  public List<List<InlineKeyboardMarkupButton>> getButtons(
      Locale locale, CustomField field, IssueCreationDto issueCreationDto, String newValue) {

    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    Options options = getOptions(field);

    List<String> values =
        Arrays.asList(
            issueCreationDto.getRequiredIssueCreationFieldValues().get(field).split(", "));

    options.forEach(
        option -> {
          InlineKeyboardMarkupButton optionButton =
              InlineKeyboardMarkupButton.buildButtonWithoutUrl(
                  String.format(
                      "%s %s", option.getValue(), values.contains(option.getValue()) ? "☑️" : "️"),
                  String.join("-", "updateIssueButtonValue", option.getValue()));
          List<InlineKeyboardMarkupButton> newButtonsRow = new ArrayList<>(1);
          newButtonsRow.add(optionButton);
          buttons.add(newButtonsRow);
        });

    InlineKeyboardMarkupButton optionButton =
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            "Add",
            String.join(
                "-",
                "selectIssueButtonValue",
                issueCreationDto.getRequiredIssueCreationFieldValues().get(field)));
    List<InlineKeyboardMarkupButton> newButtonsRow = new ArrayList<>(1);
    newButtonsRow.add(optionButton);
    buttons.add(newButtonsRow);

    return buttons;
  }

  @Override
  public ChatState getNewChatState(int nextFieldNum, IssueCreationDto issueCreationDto) {
    return ChatState.buildNewIssueButtonFieldsWaitingState(nextFieldNum, issueCreationDto);
  }

  @Override
  public void updateValue(IssueCreationDto issueCreationDto, CustomField field, String newValue) {
    String fieldValue = issueCreationDto.getRequiredIssueCreationFieldValues().get(field);
    List<String> values = new ArrayList<String>(Arrays.asList(fieldValue.split(", ")));

    if (values.contains(newValue)) {
      values.remove(values.indexOf(newValue));

      issueCreationDto.getRequiredIssueCreationFieldValues().put(field, String.join(", ", values));
    } else {
      if (fieldValue.length() == 0) {
        issueCreationDto.getRequiredIssueCreationFieldValues().put(field, newValue);
      } else {
        issueCreationDto
            .getRequiredIssueCreationFieldValues()
            .put(field, String.format("%s, %s", fieldValue, newValue));
      }
    }
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
    System.out.println(options.isEmpty());
    return options;
  }
}
