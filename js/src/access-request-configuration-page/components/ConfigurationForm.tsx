/* eslint-disable  sonarjs/cognitive-complexity */
import React, { ReactElement } from 'react';
import Form, {
  CheckboxField,
  ErrorMessage,
  Field,
  Fieldset,
} from '@atlaskit/form';
import styled from '@emotion/styled';
import { Checkbox } from '@atlaskit/checkbox';
import { I18n } from '@atlassian/wrm-react-i18n';
import { OptionsType, OptionType } from '@atlaskit/select';
import UsersSelect, { createUserOption } from '../components/UsersSelect';
import GroupsSelect, { createGroupOption } from '../components/GroupsSelect';
import { AccessRequestConfiguration, ErrorData } from '../../shared/types';
import ProjectRolesSelect, {
  createProjectRoleOption,
} from './ProjectRolesSelect';
import UserFieldsSelect, { createUserFieldOption } from './UserFieldsSelect';

type Props = {
  projectKey: string;
  currentValue?: AccessRequestConfiguration;
  onSave: (configuration: AccessRequestConfiguration) => void;
  onCancel?: () => void;
  submitError?: ErrorData;
};

export const FORM_ID = 'myteam-access-request-configuration-form';

type FormState = {
  users?: OptionType[];
  groups?: OptionType[];
  roles?: OptionType[];
  userFields?: OptionType[];
  sendEmail: boolean;
  sendMessage: boolean;
};

const Container = styled.div`
  form > * {
    margin-bottom: 10px;
  }
`;

function ConfigurationForm({
  projectKey,
  currentValue,
  onSave,
  submitError,
}: Props): ReactElement {
  return (
    <Container>
      <Form
        onSubmit={(formState: FormState) => {
          const configuration: AccessRequestConfiguration = {
            id: currentValue && currentValue.id ? currentValue.id : undefined,
            projectKey: projectKey,
            users: formState.users?.map((user) => ({
              userKey: user.value.toString(),
              displayName: user.label,
            })),
            groups: formState.groups?.map((group) => group.value.toString()),
            projectRoles: formState.roles?.map((role) => ({
              id: role.value.toString(),
              name: role.label,
            })),
            userFields: formState.userFields?.map((field) => ({
              id: field.value.toString(),
              name: field.label,
            })),
            sendEmail: formState.sendEmail,
            sendMessage: formState.sendMessage,
          };
          onSave(configuration);
        }}
      >
        {({ formProps }) => (
          <form
            // eslint-disable-next-line react/jsx-props-no-spreading
            {...formProps}
            name="configurationForm"
            id={FORM_ID}
          >
            <div className="form-body">
              <Fieldset>
                <Field<OptionsType>
                  name="users"
                  label={I18n.getText('admin.common.words.users')}
                  defaultValue={
                    currentValue?.users &&
                    currentValue.users.map(createUserOption)
                  }
                >
                  {({ fieldProps, error }) => (
                    <>
                      <UsersSelect
                        id={fieldProps.id}
                        selectedValue={currentValue?.users?.map(
                          createUserOption,
                        )}
                        onChange={fieldProps.onChange}
                      />
                      {(error || submitError?.fieldErrors?.users) && (
                        <ErrorMessage>
                          {error || submitError?.fieldErrors?.users.messages[0]}
                        </ErrorMessage>
                      )}
                    </>
                  )}
                </Field>
                <Field<OptionsType>
                  name="groups"
                  label={I18n.getText('common.words.groups')}
                  defaultValue={currentValue?.groups?.map(createGroupOption)}
                >
                  {({ fieldProps, error }) => (
                    <>
                      <GroupsSelect
                        id={fieldProps.id}
                        selectedValue={currentValue?.groups?.map(
                          createGroupOption,
                        )}
                        onChange={fieldProps.onChange}
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
                <Field<OptionsType>
                  name="roles"
                  label={I18n.getText('admin.projects.project.roles')}
                  defaultValue={
                    currentValue?.projectRoles &&
                    currentValue.projectRoles.map(createProjectRoleOption)
                  }
                >
                  {({ fieldProps, error }) => (
                    <>
                      <ProjectRolesSelect
                        id={fieldProps.id}
                        selectedValue={currentValue?.projectRoles?.map(
                          createProjectRoleOption,
                        )}
                        onChange={fieldProps.onChange}
                        projectKey={projectKey}
                      />
                      {(error || submitError?.fieldErrors?.projectRoles) && (
                        <ErrorMessage>
                          {error ||
                            submitError?.fieldErrors?.projectRoles.messages[0]}
                        </ErrorMessage>
                      )}
                    </>
                  )}
                </Field>
                <Field<OptionsType>
                  name="userFields"
                  label={I18n.getText(
                    'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.dialog.field.userFields',
                  )}
                  defaultValue={
                    currentValue?.userFields &&
                    currentValue.userFields.map(createUserFieldOption)
                  }
                >
                  {({ fieldProps, error }) => (
                    <>
                      <UserFieldsSelect
                        id={fieldProps.id}
                        selectedValue={currentValue?.userFields?.map(
                          createUserFieldOption,
                        )}
                        onChange={fieldProps.onChange}
                        projectKey={projectKey}
                      />
                      {(error || submitError?.fieldErrors?.userFields) && (
                        <ErrorMessage>
                          {error ||
                            submitError?.fieldErrors?.userFields.messages[0]}
                        </ErrorMessage>
                      )}
                    </>
                  )}
                </Field>
                {submitError?.fieldErrors?.participants && (
                  <ErrorMessage>
                    {submitError?.fieldErrors?.participants.messages[0]}
                  </ErrorMessage>
                )}
              </Fieldset>
              <Fieldset>
                <CheckboxField
                  name="sendEmail"
                  label={I18n.getText(
                    'admin.schemes.notifications.notifications',
                  )}
                  defaultIsChecked={currentValue && currentValue.sendEmail}
                >
                  {({ fieldProps }) => (
                    <Checkbox
                      id={fieldProps.id}
                      defaultChecked={currentValue && currentValue.sendEmail}
                      label={I18n.getText(
                        'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.dialog.field.sendEmail',
                      )}
                      size="medium"
                      onChange={fieldProps.onChange}
                    />
                  )}
                </CheckboxField>
                <CheckboxField
                  name="sendMessage"
                  defaultIsChecked={currentValue && currentValue.sendMessage}
                >
                  {({ fieldProps }) => (
                    <Checkbox
                      id={fieldProps.id}
                      defaultChecked={currentValue && currentValue.sendMessage}
                      label={I18n.getText(
                        'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.dialog.field.sendMessage',
                      )}
                      size="medium"
                      onChange={fieldProps.onChange}
                    />
                  )}
                </CheckboxField>
                {submitError?.fieldErrors?.notifications && (
                  <ErrorMessage>
                    {submitError?.fieldErrors?.notifications.messages[0]}
                  </ErrorMessage>
                )}
              </Fieldset>
            </div>
          </form>
        )}
      </Form>
    </Container>
  );
}

ConfigurationForm.ConfigurationForm = {
  currentValue: undefined,
  onCancel: undefined,
  submitError: undefined,
};

export default ConfigurationForm;
