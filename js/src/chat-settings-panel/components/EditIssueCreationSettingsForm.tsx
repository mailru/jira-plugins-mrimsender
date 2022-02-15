import React, { Fragment, ReactElement, useState } from 'react';
import { IssueCreationSettings } from '../types';
import Textfield from '@atlaskit/textfield';
import { Checkbox } from '@atlaskit/checkbox';
import Form, { CheckboxField, ErrorMessage, Field, FormFooter, HelperMessage } from '@atlaskit/form';
import Button from '@atlaskit/button';
import LabelsSelect from './LabelsSelect';
import ProjectSelect from './ProjectSelect';
import IssueTypeSelect from './IssueTypeSelect';
import styled from '@emotion/styled';
import { ValueType, OptionType, OptionsType } from '@atlaskit/select';

type EditableSettings = Partial<Omit<IssueCreationSettings, 'id' | 'chatId'>>;

type Props = {
  defaultSettings: EditableSettings;
  onSave: (settings: EditableSettings) => void;
};

const Container = styled.div`
  form > * {
    margin-bottom: 10px;
  }
`;

const HintBeforeTagInput = styled.span`
  margin-left: 7px;
  opacity: 0.6;
  font-size: large;
  font-weight: 500;
`;

type FormState = {
  enabled: boolean;
  issueTypeId: OptionType;
  projectKey: OptionType;
  tag: string;
  labels: ReadonlyArray<OptionType>;
};

const validateNotNull = (value?: unknown) => {
  if (value) {
    return undefined;
  }
  return 'Необходимо заполнить поле';
};

const createOption = (value?: string) => {
  if (!value) return null;
  return {
    label: value,
    value,
  };
};

const EditIssueCreationSettingsForm = ({ defaultSettings, onSave }: Props): ReactElement => {
  const [selectedProjectKey, setSelectedProjectKey] = useState(defaultSettings.projectKey);

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
        {({ formProps }) => (
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
              defaultValue={createOption(defaultSettings.projectKey)}
              validate={validateNotNull}>
              {({ fieldProps: { id, onChange, ...rest }, error }) => (
                <>
                  <ProjectSelect
                    onChange={(value: OptionType | null) => {
                      if (value) setSelectedProjectKey(String(value.value));
                      onChange(value);
                    }}
                    defaultProjectKey={defaultSettings.projectKey}
                    validationState={error ? 'error' : 'default'}
                    id={id}
                    {...rest}
                  />
                  {error && <ErrorMessage>{error}</ErrorMessage>}
                </>
              )}
            </Field>

            <Field<ValueType<OptionType>>
              name="issueTypeId"
              label="Выберите тип задачи"
              defaultValue={createOption(defaultSettings.issueTypeId)}
              isRequired
              validate={validateNotNull}>
              {({ fieldProps: { id, ...rest }, error }) => (
                <>
                  <IssueTypeSelect
                    defaultIssueTypeId={defaultSettings.issueTypeId}
                    id={id}
                    projectKey={selectedProjectKey}
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
                  <Textfield
                    placeholder="Тег"
                    {...fieldProps}
                    elemBeforeInput={<HintBeforeTagInput>#</HintBeforeTagInput>}
                  />
                  <HelperMessage>Тег по которому будет создаваться задача. Например: #task</HelperMessage>
                  {error && <ErrorMessage>{error}</ErrorMessage>}
                </Fragment>
              )}
            </Field>

            <Field<OptionsType<OptionType>>
              name="labels"
              label="Выберите метки"
              defaultValue={(defaultSettings.labels || []).map(createOption) as OptionsType<OptionType>}>
              {({ fieldProps }) => <LabelsSelect defaultLabels={defaultSettings.labels} {...fieldProps} />}
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
