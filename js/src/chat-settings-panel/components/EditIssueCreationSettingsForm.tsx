import React, { Fragment, ReactElement } from 'react';
import { IssueCreationSettings } from '../types';
import Textfield from '@atlaskit/textfield';
import { Checkbox } from '@atlaskit/checkbox';
import Form, { CheckboxField, ErrorMessage, Field, FormFooter, HelperMessage } from '@atlaskit/form';
import Button from '@atlaskit/button';
import LabelsSelect from './LabelsSelect';
import ProjectSelect from './ProjectSelect';
import IssueTypeSelect from './IssueTypeSelect';
import styled from '@emotion/styled';
import { ValueType, OptionType } from '@atlaskit/select';

type EditableSettings = Omit<IssueCreationSettings, 'id' | 'chatId'>;

type Props = {
  defaultSettings: EditableSettings;
  onSave: (settings: EditableSettings) => void;
};

const Container = styled.div`
  form > * {
    margin-bottom: 10px;
  }
`;

type FormState = {
  enabled: boolean;
  issueTypeId: OptionType;
  projectKey: OptionType;
  tag: string;
  labels: ReadonlyArray<OptionType>;
};

const validateNotNull = (value?: any) => {
  if (value) {
    return undefined;
  }
  return 'Необходимо заполнить поле';
};

const EditIssueCreationSettingsForm = ({ defaultSettings, onSave }: Props): ReactElement => {
  console.log(defaultSettings);

  return (
    <Container>
      <Form
        onSubmit={({ enabled, issueTypeId, projectKey, tag, labels }: FormState) => {
          onSave({
            enabled,
            tag,
            projectKey: String(projectKey.value),
            issueTypeId: String(issueTypeId.value),
            labels: labels ? labels.map((l) => String(l.value)) : [],
          });
        }}>
        {({ formProps }: any) => (
          <form {...formProps}>
            <CheckboxField name="enabled" label="Статус" defaultIsChecked={defaultSettings.enabled}>
              {({ fieldProps }) => (
                <Checkbox label="Включено" size="large" defaultChecked={defaultSettings.enabled} {...fieldProps} />
              )}
            </CheckboxField>

            <Field<ValueType<OptionType>>
              name="projectKey"
              label="Выберите проект"
              isRequired
              validate={validateNotNull}>
              {({ fieldProps: { id, ...rest }, error }) => (
                <>
                  <ProjectSelect validationState={error ? 'error' : 'default'} id={id} {...rest} />
                  {error && <ErrorMessage>{error}</ErrorMessage>}
                </>
              )}
            </Field>

            <Field<ValueType<OptionType>>
              name="issueTypeId"
              label="Выберите тип задачи"
              isRequired
              validate={validateNotNull}>
              {({ fieldProps: { id, ...rest }, error }) => (
                <>
                  <IssueTypeSelect
                    defaultIssueTypeId={defaultSettings.issueTypeId}
                    id={id}
                    projectKey="DEV"
                    {...rest}
                  />
                  {error && <ErrorMessage>{error}</ErrorMessage>}
                </>
              )}
            </Field>

            <Field
              label="Тег для создания задачи"
              name="tag"
              defaultValue={defaultSettings.tag}
              isRequired
              validate={validateNotNull}>
              {({ fieldProps, error }) => (
                <Fragment>
                  <Textfield placeholder="Тег" {...fieldProps} />
                  <HelperMessage>Тег по которому будет создаваться задача. Например: #task</HelperMessage>
                  {error && <ErrorMessage>{error}</ErrorMessage>}
                </Fragment>
              )}
            </Field>

            <Field<ValueType<OptionType>> name="labels" label="Выберите метки">
              {({ fieldProps: { id, ...rest } }) => <LabelsSelect {...rest} />}
            </Field>

            <FormFooter>
              <Button type="submit" appearance="primary">
                Сохранить
              </Button>
            </FormFooter>
          </form>
        )}
      </Form>
    </Container>
  );
};

export default EditIssueCreationSettingsForm;
