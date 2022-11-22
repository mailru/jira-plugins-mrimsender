/* eslint-disable  sonarjs/cognitive-complexity */
import React, { ReactElement, useState } from 'react';
import Form, {
  CheckboxField,
  ErrorMessage,
  Field,
  Fieldset,
  HelperMessage,
} from '@atlaskit/form';
import styled from '@emotion/styled';
import { Checkbox } from '@atlaskit/checkbox';
import { I18n } from '@atlassian/wrm-react-i18n';
import Select, { OptionsType, OptionType, ValueType } from '@atlaskit/select';
import { RadioGroup } from '@atlaskit/radio';
import Textfield from '@atlaskit/textfield';
import JqlFilterSelect, { createFilterOption } from './JqlFilterSelect';
import UsersSelect, { createUserOption } from './UsersSelect';
import GroupsSelect, { createGroupOption } from './GroupsSelect';
import ChatsSelect, { createChatOption } from './ChatsSelect';
import { ErrorData, FilterSubscription } from '../../shared/types';
import { useGetSubscriptionsPermissions } from '../../shared/hooks';
import RecipientsSelect, { recipientsTypeOptions } from './RecipientsSelect';
import { Simulate } from 'react-dom/test-utils';
import change = Simulate.change;

type Props = {
  currentValue?: FilterSubscription;
  onSave: (subscription: FilterSubscription) => void;
  onCancel?: () => void;
  submitError?: ErrorData;
};

export const FORM_ID = 'myteam-filter-subscription-form';

type FormState = {
  filter?: OptionType;
  recipientsType?: OptionType;
  users?: OptionType[];
  groups?: OptionType[];
  chats?: OptionType[];
  scheduleMode: string;
  hours?: OptionType;
  minutes?: OptionType;
  weekDays?: string[];
  monthDay?: OptionType;
  advanced?: string;
  type?: string;
  emailOnEmpty: boolean;
};

const Container = styled.div`
  form > * {
    margin-bottom: 10px;
  }
`;

const TimeInterval = styled.div`
  display: flex;
  align-items: end;
`;

const SmallSelect = styled.div`
  width: 200px;
  margin-right: 10px;
`;

const scheduleOptions = [
  {
    name: 'schedule',
    label: I18n.getText('cron.editor.daily'),
    value: 'daily',
  },
  {
    name: 'schedule',
    label: I18n.getText('cron.editor.days.per.week'),
    value: 'daysOfWeek',
  },
  {
    name: 'schedule',
    label: I18n.getText('cron.editor.days.per.month'),
    value: 'daysOfMonth',
  },
  {
    name: 'schedule',
    label: I18n.getText('cron.editor.advanced'),
    value: 'advanced',
  },
];

const hourOptions = [...Array(24).keys()].map((hour) => ({
  label: hour.toString(),
  value: hour,
}));
const minutesOptions = [...Array(12).keys()].map((key) => ({
  label: (key * 5).toString(),
  value: key * 5,
}));

const weekdaysOptions = [
  {
    label: I18n.getText('cron.editor.monday'),
    value: '2',
  },
  {
    label: I18n.getText('cron.editor.tuesday'),
    value: '3',
  },
  {
    label: I18n.getText('cron.editor.wednesday'),
    value: '4',
  },
  {
    label: I18n.getText('cron.editor.thursday'),
    value: '5',
  },
  {
    label: I18n.getText('cron.editor.friday'),
    value: '6',
  },
  {
    label: I18n.getText('cron.editor.saturday'),
    value: '7',
  },
  {
    label: I18n.getText('cron.editor.sunday'),
    value: '1',
  },
];

const monthDayOptions = [...Array(32).keys()].map((monthDay) => ({
  label: monthDay.toString(),
  value: monthDay,
}));

export const typeOptions = [
  {
    name: 'type',
    label: I18n.getText(
      'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.field.type.all',
    ),
    value: 'ALL',
  },
  {
    name: 'type',
    label: I18n.getText(
      'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.field.type.created',
    ),
    value: 'CREATED',
  },
  {
    name: 'type',
    label: I18n.getText(
      'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.field.type.updated',
    ),
    value: 'UPDATED',
  },
  {
    name: 'type',
    label: I18n.getText(
      'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.field.type.createdAndUpdated',
    ),
    value: 'CREATED_AND_UPDATED',
  },
];

const validateNotNull = (value?: unknown) => {
  if (value) {
    return undefined;
  }
  return I18n.getText('common.forms.requiredfields');
};

function FilterSubscriptionForm({
  currentValue,
  onSave,
  submitError,
}: Props): ReactElement {
  const [recipientsType, setRecipientsType] = useState<string | undefined>(
    currentValue?.recipientsType,
  );
  const [scheduleMode, setScheduleMode] = useState<string | undefined>(
    currentValue?.scheduleMode,
  );

  const permission = useGetSubscriptionsPermissions();

  return (
    <Container>
      <Form
        onSubmit={(formState: FormState) => {
          const subscription: FilterSubscription = {
            id: currentValue && currentValue.id ? currentValue.id : undefined,
            filter:
              formState.filter !== undefined
                ? {
                    id: parseInt(formState.filter.value.toString(), 10),
                    name: formState.filter.label,
                    owner: formState.filter.owner,
                  }
                : undefined,
            creator:
              currentValue && currentValue.creator
                ? currentValue.creator
                : undefined,
            recipientsType:
              formState.recipientsType !== undefined
                ? formState.recipientsType.value.toString()
                : undefined,
            users: formState.users?.map((user) => ({
              userKey: user.value.toString(),
              displayName: user.label,
            })),
            groups: formState.groups?.map((group) => group.value.toString()),
            chats: formState.chats?.map((chat) => chat.value.toString()),
            scheduleMode: formState.scheduleMode,
            hours:
              formState.hours !== undefined
                ? parseInt(formState.hours.value.toString(), 10)
                : undefined,
            minutes:
              formState.minutes !== undefined
                ? parseInt(formState.minutes.value.toString(), 10)
                : undefined,
            weekDays: formState.weekDays,
            monthDay:
              formState.monthDay !== undefined
                ? parseInt(formState.monthDay.value.toString(), 10)
                : undefined,
            advanced: formState.advanced?.toString(),
            type: formState.type,
            lastRun: currentValue?.lastRun,
            emailOnEmpty: formState.emailOnEmpty,
          };
          onSave(subscription);
        }}
      >
        {({ formProps }) => (
          <form
            // eslint-disable-next-line react/jsx-props-no-spreading
            {...formProps}
            name="subscriptionForm"
            id={FORM_ID}
          >
            <div className="form-body">
              <Field<ValueType<OptionType>>
                name="filter"
                label={I18n.getText('template.subscription.filter')}
                isRequired
                defaultValue={
                  currentValue?.filter &&
                  createFilterOption(currentValue?.filter)
                }
                validate={validateNotNull}
              >
                {({ fieldProps, error }) => (
                  // eslint-disable-next-line react/jsx-props-no-spreading
                  <>
                    <JqlFilterSelect
                      id={fieldProps.id}
                      selectedValue={
                        currentValue !== undefined &&
                        currentValue.filter !== undefined
                          ? createFilterOption(currentValue.filter)
                          : undefined
                      }
                      onChange={(value) => fieldProps.onChange(value)}
                    />
                    {(error || submitError?.fieldErrors?.filter) && (
                      <ErrorMessage>
                        {error || submitError?.fieldErrors?.filter.messages[0]}
                      </ErrorMessage>
                    )}
                  </>
                )}
              </Field>
              <Fieldset>
                <Field<ValueType<OptionType>>
                  name="recipientsType"
                  label={I18n.getText('filtersubscription.field.recipients')}
                  defaultValue={recipientsTypeOptions.find(
                    (option) => option.value === recipientsType,
                  )}
                  isRequired
                  validate={validateNotNull}
                >
                  {({ fieldProps, error }) => (
                    <>
                      <RecipientsSelect
                        id={fieldProps.id}
                        selectedValue={recipientsType}
                        onChange={(value) => {
                          setRecipientsType(value?.value);
                          fieldProps.onChange(value);
                        }}
                      />
                      {(error || submitError?.fieldErrors?.recipientsType) && (
                        <ErrorMessage>
                          {error ||
                            submitError?.fieldErrors?.recipientsType
                              .messages[0]}
                        </ErrorMessage>
                      )}
                    </>
                  )}
                </Field>
                {recipientsType === 'USER' && (
                  <Field<OptionsType>
                    name="users"
                    defaultValue={
                      currentValue?.users &&
                      currentValue.users.map((user) => createUserOption(user))
                    }
                    isRequired={recipientsType === 'USER'}
                    validate={(value) =>
                      recipientsType === 'USER' && validateNotNull(value)
                    }
                  >
                    {({ fieldProps, error }) => (
                      <>
                        <UsersSelect
                          id={fieldProps.id}
                          selectedValue={currentValue?.users?.map((user) =>
                            createUserOption(user),
                          )}
                          onChange={(value) => fieldProps.onChange(value)}
                        />
                        {!permission.data?.jiraAdmin && (
                          <HelperMessage>
                            {I18n.getText(
                              'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.field.users.message',
                            )}
                          </HelperMessage>
                        )}
                        {(error || submitError?.fieldErrors?.users) && (
                          <ErrorMessage>
                            {error ||
                              submitError?.fieldErrors?.users.messages[0]}
                          </ErrorMessage>
                        )}
                      </>
                    )}
                  </Field>
                )}
                {recipientsType === 'GROUP' && (
                  <Field<OptionsType>
                    name="groups"
                    defaultValue={
                      currentValue?.groups &&
                      currentValue.groups.map((group) =>
                        createGroupOption(group),
                      )
                    }
                    isRequired={recipientsType === 'GROUP'}
                    validate={(value) =>
                      recipientsType === 'GROUP' && validateNotNull(value)
                    }
                  >
                    {({ fieldProps, error }) => (
                      <>
                        <GroupsSelect
                          id={fieldProps.id}
                          selectedValue={currentValue?.groups?.map((group) =>
                            createGroupOption(group),
                          )}
                          onChange={(value) => fieldProps.onChange(value)}
                        />
                        {(error || submitError?.fieldErrors?.groups) && (
                          <ErrorMessage>
                            {error ||
                              submitError?.fieldErrors?.groups.messages[0]}
                          </ErrorMessage>
                        )}
                      </>
                    )}
                  </Field>
                )}
                {recipientsType === 'CHAT' && (
                  <Field<OptionsType>
                    name="chats"
                    defaultValue={
                      currentValue?.chats &&
                      currentValue.chats.map((chat) => createChatOption(chat))
                    }
                    isRequired={recipientsType === 'CHAT'}
                    validate={(value) =>
                      recipientsType === 'CHAT' && validateNotNull(value)
                    }
                  >
                    {({ fieldProps, error }) => (
                      <>
                        <ChatsSelect
                          id={fieldProps.id}
                          selectedValue={currentValue?.chats?.map((chat) =>
                            createChatOption(chat),
                          )}
                          onChange={(value) => fieldProps.onChange(value)}
                        />
                        {(error || submitError?.fieldErrors?.chats) && (
                          <ErrorMessage>
                            {error ||
                              submitError?.fieldErrors?.chats.messages[0]}
                          </ErrorMessage>
                        )}
                      </>
                    )}
                  </Field>
                )}
              </Fieldset>
              <Field
                name="scheduleMode"
                label={I18n.getText('filtersubscription.field.schedule')}
                defaultValue={scheduleMode}
                isRequired
                validate={validateNotNull}
              >
                {({ fieldProps, error }) => (
                  <>
                    <RadioGroup
                      options={scheduleOptions}
                      value={scheduleMode}
                      onChange={(event) => {
                        setScheduleMode(event.currentTarget.value);
                        fieldProps.onChange(event);
                      }}
                    />
                    {(error || submitError?.fieldErrors?.scheduleMode) && (
                      <ErrorMessage>
                        {error ||
                          submitError?.fieldErrors?.scheduleMode.messages[0]}
                      </ErrorMessage>
                    )}
                  </>
                )}
              </Field>
              {scheduleMode !== undefined && (
                <Fieldset>
                  {scheduleMode !== 'advanced' && (
                    <TimeInterval>
                      <SmallSelect>
                        <Field<ValueType<OptionType>>
                          name="hours"
                          label={I18n.getText(
                            'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.field.interval',
                          )}
                          defaultValue={
                            currentValue?.hours !== undefined
                              ? hourOptions.find(
                                  (option) =>
                                    option.value === currentValue?.hours,
                                )
                              : undefined
                          }
                          isRequired={scheduleMode !== 'advanced'}
                          validate={(value) =>
                            scheduleMode !== 'advanced' &&
                            validateNotNull(value)
                          }
                        >
                          {({ fieldProps, error }) => (
                            // eslint-disable-next-line react/jsx-props-no-spreading
                            <>
                              <Select
                                inputId={fieldProps.id}
                                defaultValue={hourOptions.find(
                                  (option) =>
                                    option.value === currentValue?.hours,
                                )}
                                options={hourOptions}
                                onChange={(value) => fieldProps.onChange(value)}
                                placeholder={I18n.getText(
                                  'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.field.interval.hours',
                                )}
                                menuPortalTarget={document.body}
                                styles={{
                                  menuPortal: (base) => ({
                                    ...base,
                                    zIndex: 9999,
                                  }),
                                }}
                              />
                              {(error || submitError?.fieldErrors?.hours) && (
                                <ErrorMessage>
                                  {error ||
                                    submitError?.fieldErrors?.hours.messages[0]}
                                </ErrorMessage>
                              )}
                            </>
                          )}
                        </Field>
                      </SmallSelect>
                      <SmallSelect>
                        <Field<ValueType<OptionType>>
                          name="minutes"
                          isRequired={scheduleMode !== 'advanced'}
                          defaultValue={
                            currentValue?.minutes !== undefined
                              ? minutesOptions.find(
                                  (option) =>
                                    option.value === currentValue?.minutes,
                                )
                              : undefined
                          }
                          validate={(value) =>
                            scheduleMode !== 'advanced' &&
                            validateNotNull(value)
                          }
                        >
                          {({ fieldProps, error }) => (
                            // eslint-disable-next-line react/jsx-props-no-spreading
                            <>
                              <Select
                                inputId={fieldProps.id}
                                defaultValue={minutesOptions.find(
                                  (option) =>
                                    option.value === currentValue?.minutes,
                                )}
                                options={minutesOptions}
                                onChange={(value) => fieldProps.onChange(value)}
                                placeholder={I18n.getText(
                                  'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.field.interval.minutes',
                                )}
                                menuPortalTarget={document.body}
                                styles={{
                                  menuPortal: (base) => ({
                                    ...base,
                                    zIndex: 9999,
                                  }),
                                }}
                              />
                              {(error || submitError?.fieldErrors?.minutes) && (
                                <ErrorMessage>
                                  {error ||
                                    submitError?.fieldErrors?.minutes
                                      .messages[0]}
                                </ErrorMessage>
                              )}
                            </>
                          )}
                        </Field>
                      </SmallSelect>
                    </TimeInterval>
                  )}
                  {scheduleMode === 'daysOfWeek' &&
                    weekdaysOptions.map((weekday) => (
                      <CheckboxField
                        key={weekday.value}
                        name="weekDays"
                        defaultIsChecked={
                          currentValue &&
                          currentValue.weekDays &&
                          currentValue.weekDays.indexOf(weekday.value) !== -1
                        }
                        value={weekday.value}
                      >
                        {({ fieldProps }) => (
                          <>
                            <Checkbox
                              id={fieldProps.id}
                              label={weekday.label}
                              size="medium"
                              defaultChecked={
                                currentValue &&
                                currentValue.weekDays &&
                                currentValue.weekDays.indexOf(weekday.value) !==
                                  -1
                              }
                              value={fieldProps.value}
                              onChange={(value) => fieldProps.onChange(value)}
                            />
                            {submitError?.fieldErrors?.weekDays && (
                              <ErrorMessage>
                                {submitError?.fieldErrors?.weekDays.messages[0]}
                              </ErrorMessage>
                            )}
                          </>
                        )}
                      </CheckboxField>
                    ))}
                </Fieldset>
              )}
              {scheduleMode === 'daysOfMonth' && (
                <SmallSelect>
                  <Field<ValueType<OptionType>>
                    name="monthDay"
                    defaultValue={
                      currentValue?.monthDay !== undefined
                        ? monthDayOptions.find(
                            (option) => option.value === currentValue?.monthDay,
                          )
                        : undefined
                    }
                    isRequired={scheduleMode === 'daysOfMonth'}
                    validate={(value) =>
                      scheduleMode === 'daysOfMonth' && validateNotNull(value)
                    }
                  >
                    {({ fieldProps, error }) => (
                      // eslint-disable-next-line react/jsx-props-no-spreading
                      <>
                        <Select
                          inputId={fieldProps.id}
                          defaultValue={monthDayOptions.find(
                            (option) => option.value === currentValue?.monthDay,
                          )}
                          options={monthDayOptions}
                          onChange={(value) => fieldProps.onChange(value)}
                          placeholder={I18n.getText(
                            'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.field.interval.dayOfTheMonth',
                          )}
                          menuPortalTarget={document.body}
                          styles={{
                            menuPortal: (base) => ({ ...base, zIndex: 9999 }),
                          }}
                        />
                        {(error || submitError?.fieldErrors?.monthDay) && (
                          <ErrorMessage>
                            {error ||
                              submitError?.fieldErrors?.monthDay.messages[0]}
                          </ErrorMessage>
                        )}
                      </>
                    )}
                  </Field>
                </SmallSelect>
              )}
              {scheduleMode === 'advanced' && (
                <Field
                  name="advanced"
                  label={I18n.getText(
                    'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.field.interval',
                  )}
                  defaultValue={currentValue?.advanced}
                  isRequired={scheduleMode === 'advanced'}
                  validate={(value) =>
                    scheduleMode === 'advanced' && validateNotNull(value)
                  }
                >
                  {({ fieldProps, error }) => (
                    <>
                      <Textfield
                        id={fieldProps.id}
                        defaultValue={currentValue?.advanced}
                        onChange={(value) => fieldProps.onChange(value)}
                        autoComplete="off"
                        placeholder={I18n.getText('cron.editor.cronstring')}
                      />
                      {(error || submitError?.fieldErrors?.advanced) && (
                        <ErrorMessage>
                          {error ||
                            submitError?.fieldErrors?.advanced.messages[0]}
                        </ErrorMessage>
                      )}
                    </>
                  )}
                </Field>
              )}
              <Field
                name="type"
                label={I18n.getText(
                  'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.field.type',
                )}
                defaultValue={currentValue?.type}
                isRequired
                validate={validateNotNull}
              >
                {({ fieldProps, error }) => (
                  <>
                    <RadioGroup
                      options={typeOptions}
                      defaultValue={currentValue?.type}
                      onChange={(event) => fieldProps.onChange(event)}
                    />
                    {(error || submitError?.fieldErrors?.type) && (
                      <ErrorMessage>
                        {error || submitError?.fieldErrors?.type.messages[0]}
                      </ErrorMessage>
                    )}
                  </>
                )}
              </Field>
              <CheckboxField
                name="emailOnEmpty"
                label={I18n.getText(
                  'admin.schemes.notifications.notifications',
                )}
                defaultIsChecked={currentValue && currentValue.emailOnEmpty}
              >
                {({ fieldProps }) => (
                  <Checkbox
                    id={fieldProps.id}
                    defaultChecked={currentValue && currentValue.emailOnEmpty}
                    label={I18n.getText(
                      'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.field.emailOnEmpty.label',
                    )}
                    size="medium"
                    // eslint-disable-next-line react/jsx-props-no-spreading
                    onChange={(value) => fieldProps.onChange(value)}
                  />
                )}
              </CheckboxField>
            </div>
          </form>
        )}
      </Form>
    </Container>
  );
}

FilterSubscriptionForm.defaultProps = {
  currentValue: undefined,
  onCancel: undefined,
  submitError: undefined,
};

export default FilterSubscriptionForm;
