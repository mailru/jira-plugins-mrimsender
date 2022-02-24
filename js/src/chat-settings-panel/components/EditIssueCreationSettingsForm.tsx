import React, { Fragment, ReactElement, useEffect, useState } from 'react';
import { FieldHtml, IssueCreationSettings } from '../types';
import Textfield from '@atlaskit/textfield';
import { Checkbox } from '@atlaskit/checkbox';
import Form, { CheckboxField, ErrorMessage, Field, FormFooter, HelperMessage } from '@atlaskit/form';
import Button from '@atlaskit/button';
import LabelsSelect from './LabelsSelect';
import ProjectSelect from './ProjectSelect';
import IssueTypeSelect from './IssueTypeSelect';
import styled from '@emotion/styled';
import { ValueType, OptionType, OptionsType } from '@atlaskit/select';
import Events from 'jira/util/events';
import Types from 'jira/util/events/types';
import Reasons from 'jira/util/events/reasons';
import { loadIssueForm } from '../api/CommonApiClient';
import LoadableComponent from './LoadableComponent';

type EditableSettings = Partial<Omit<IssueCreationSettings, 'id' | 'chatId'>>;

type Props = {
  defaultSettings: EditableSettings;
  onSave: (settings: EditableSettings) => void;
};

type FormState = {
  enabled: boolean;
  issueTypeId: OptionType;
  projectKey: OptionType;
  tag: string;
  labels: ReadonlyArray<OptionType>;
};

const FORM_ID = 'issue-create-chat-settings';

const HintBeforeTagInput = styled.span`
  margin-left: 7px;
  opacity: 0.6;
  font-size: large;
  font-weight: 500;
`;

const RequiredField = styled.div``;

const Container = styled.div`
  form > * {
    margin-bottom: 10px;
  }
`;

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

const getFormValues = (): Map<string, string> => {
  const res = new Map();

  const values = $(`#${FORM_ID}`).serialize();
  console.log(values);

  return res;
};

const isIgnoredField = (id: string): boolean => {
  return ['project', 'issuetype', 'summary', 'reporter'].includes(id);
};

const EditIssueCreationSettingsForm = ({ defaultSettings, onSave }: Props): ReactElement => {
  const [selectedMainData, setSelectedMainData] = useState<{ projectKey?: string; issueTypeId?: string }>({
    projectKey: defaultSettings.projectKey,
    issueTypeId: defaultSettings.issueTypeId,
  });

  const [requiredFieldsState, setRequiredFieldsState] = useState<{
    isLoading: boolean;
    data?: ReadonlyArray<FieldHtml>;
    error?: string;
  }>({
    isLoading: false,
  });

  useEffect(() => {
    if (selectedMainData.issueTypeId && selectedMainData.projectKey) {
      setRequiredFieldsState({ isLoading: true });
      loadIssueForm(selectedMainData.issueTypeId, selectedMainData.projectKey)
        .then(({ data }) => {
          setRequiredFieldsState({
            isLoading: false,
            data: data.fields.filter((f) => f.required && !isIgnoredField(f.id)),
          });
          Events.trigger(Types.NEW_CONTENT_ADDED, [$('form.aui'), Reasons.dialogReady]);
        })
        .catch((e) => {
          setRequiredFieldsState({
            isLoading: false,
            error: JSON.stringify(e),
          });
        });
    }
  }, [selectedMainData]);

  return (
    <Container>
      <Form
        onSubmit={({ enabled, issueTypeId, projectKey, tag, labels }: FormState) => {
          getFormValues();
          onSave({
            enabled,
            tag,
            projectKey: String(projectKey.value),
            issueTypeId: String(issueTypeId.value),
            labels: labels ? labels.map((l) => String(l.value)) : [],
          });
        }}>
        {({ formProps }) => (
          <form {...formProps} className="aui top-label" name="jiraform" id={FORM_ID}>
            <div className="form-body">
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
                        if (value) setSelectedMainData({ projectKey: String(value.value) });
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
                {({ fieldProps: { id, onChange, ...rest }, error }) => (
                  <>
                    <IssueTypeSelect
                      defaultIssueTypeId={defaultSettings.issueTypeId}
                      id={id}
                      projectKey={selectedMainData.projectKey}
                      {...rest}
                      onChange={(value: OptionType | null) => {
                        if (value) setSelectedMainData({ ...selectedMainData, issueTypeId: String(value.value) });
                        onChange(value);
                      }}
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

              {(requiredFieldsState.data && requiredFieldsState.data.length > 0) || requiredFieldsState.isLoading ? (
                <h3>Обязательные поля</h3>
              ) : null}

              <LoadableComponent isLoading={requiredFieldsState.isLoading}>
                {requiredFieldsState.data && requiredFieldsState.data.length > 0
                  ? requiredFieldsState.data.map((f) => {
                      return (
                        <RequiredField
                          className="field-group"
                          key={f.id}
                          dangerouslySetInnerHTML={{ __html: f.editHtml }}
                        />
                      );
                    })
                  : null}
              </LoadableComponent>

              <FormFooter>
                <Button type="submit" appearance="primary">
                  Сохранить
                </Button>
              </FormFooter>
            </div>
          </form>
        )}
      </Form>
    </Container>
  );
};

export default EditIssueCreationSettingsForm;
