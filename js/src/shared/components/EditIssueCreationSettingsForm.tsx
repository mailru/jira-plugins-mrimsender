import React, { ReactElement, useEffect, useState } from 'react';
import Textfield from '@atlaskit/textfield';
import { Checkbox } from '@atlaskit/checkbox';
import Form, {
  CheckboxField,
  ErrorMessage,
  Field,
  HelperMessage,
} from '@atlaskit/form';
import styled from '@emotion/styled';
import Select, { ValueType, OptionType, OptionsType } from '@atlaskit/select';
import Events from 'jira/util/events';
import Types from 'jira/util/events/types';
import Reasons from 'jira/util/events/reasons';
import TextArea from '@atlaskit/textarea';
import { loadIssueForm } from '../api/CommonApiClient';
import LoadableComponent from './LoadableComponent';
import IssueTypeSelect from '../../chat-settings-panel/components/IssueTypeSelect';
import ProjectSelect from '../../chat-settings-panel/components/ProjectSelect';
import LabelsSelect from '../../chat-settings-panel/components/LabelsSelect';
import {
  FieldHtml,
  FieldParam,
  IssueCreationSettings,
  LoadableDataState,
} from '../types';

type EditableSettings = Partial<Omit<IssueCreationSettings, 'id' | 'chatId'>>;

type Props = {
  defaultSettings: EditableSettings;
  onSave: (settings: EditableSettings) => void;
  onCancel?: () => void;
};

type MainState = { projectKey?: string; issueTypeId?: string };

type FormState = Omit<
  EditableSettings,
  'labels' | 'issueTypeId' | 'projectKey'
> & {
  issueTypeId: OptionType;
  projectKey: OptionType;
  labels: ReadonlyArray<OptionType>;
};

export const FORM_ID = 'issue-create-chat-settings';

const HintBeforeTagInput = styled.span`
  margin-left: 7px;
  opacity: 0.6;
  font-size: large;
  font-weight: 500;
`;

const Container = styled.div`
  form > * {
    margin-bottom: 10px;
  }
`;

const LineHelperMessage = styled.div`
  font-size: 0.8571428571428571em;
  font-style: inherit;
  line-height: 1.3333333333333333;
  font-weight: 600;
  margin-top: 16px;
  display: flex;
  justify-content: baseline;
  color: var(--ds-text-subtlest, #6b778c);
  white-space: pre-line;
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

const getFormValues = (): Array<FieldParam> => {
  const values = $(`#${FORM_ID}`).serialize();
  const queryPairs = values.split('&');
  return queryPairs
    .map((q) => {
      const split = q.split('=');
      return { field: split[0], value: split[1] };
    })
    .filter(
      (f) =>
        f.value.length > 0 &&
        !['enabled', 'tag', 'addReporterInWatchers', 'creationByAllMembers'].includes(f.field),
    );
};

const isIgnoredField = (id: string): boolean => {
  return [
    'project',
    'issuetype',
    'summary',
    'labels',
    'reporter',
    'description',
    'attachment',
    'duedate',
    'assignee',
    'labels-textarea',
    'timetracking', // unsupported
    'issuelinks', // unsupported
    'security', // TODO fix unknown error: For input string: ""
  ].includes(id);
};

const renderMainFields = (
  settings: EditableSettings,
  selectedMainData: MainState,
  setSelectedMainData: (mainData: MainState) => void,
) => {
  return (
    <>
      <CheckboxField
        name="enabled"
        label="Статус"
        defaultIsChecked={settings.enabled}
      >
        {({ fieldProps }) => (
          <Checkbox
            label="Включено"
            size="large"
            defaultChecked={settings.enabled}
            // eslint-disable-next-line react/jsx-props-no-spreading
            {...fieldProps}
          />
        )}
      </CheckboxField>

      <Field<ValueType<OptionType>>
        name="projectKey"
        label="Выберите проект"
        isRequired
        defaultValue={createOption(settings.projectKey)}
        validate={validateNotNull}
      >
        {({ fieldProps: { id, onChange, ...rest }, error }) => (
          <>
            <ProjectSelect
              onChange={(value: OptionType | null) => {
                if (value)
                  setSelectedMainData({ projectKey: String(value.value) });
                onChange(value);
              }}
              defaultProjectKey={settings.projectKey}
              validationState={error ? 'error' : 'default'}
              id={id}
              // eslint-disable-next-line react/jsx-props-no-spreading
              {...rest}
            />
            {error && <ErrorMessage>{error}</ErrorMessage>}
          </>
        )}
      </Field>

      <Field<ValueType<OptionType>>
        name="issueTypeId"
        label="Выберите тип задачи"
        defaultValue={createOption(settings.issueTypeId)}
        isRequired
        validate={validateNotNull}
      >
        {({ fieldProps: { id, onChange, ...rest }, error }) => (
          <>
            <IssueTypeSelect
              defaultIssueTypeId={settings.issueTypeId}
              id={id}
              projectKey={selectedMainData.projectKey}
              // eslint-disable-next-line react/jsx-props-no-spreading
              {...rest}
              onChange={(value: OptionType | null) => {
                if (value)
                  setSelectedMainData({
                    ...selectedMainData,
                    issueTypeId: String(value.value),
                  });
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
        defaultValue={settings.tag}
        isRequired
        validate={validateNotNull}
      >
        {({ fieldProps, error }) => (
          <>
            <Textfield
              placeholder="Тег"
              // eslint-disable-next-line react/jsx-props-no-spreading
              {...fieldProps}
              elemBeforeInput={<HintBeforeTagInput>#</HintBeforeTagInput>}
            />
            <HelperMessage>
              Тег по которому будет создаваться задача. Например: #task
            </HelperMessage>
            {error && <ErrorMessage>{error}</ErrorMessage>}
          </>
        )}
      </Field>

      <Field<OptionsType<OptionType>>
        name="labels"
        label="Выберите метки"
        defaultValue={
          (settings.labels || []).map(createOption) as OptionsType<OptionType>
        }
      >
        {({ fieldProps }) => (
          // eslint-disable-next-line react/jsx-props-no-spreading
          <LabelsSelect defaultLabels={settings.labels} {...fieldProps} />
        )}
      </Field>
    </>
  );
};

const renderAdditionalSettings = (settings: EditableSettings): ReactElement => {
  return (
    <>
      <h3>Дополнительные настройки</h3>
      <CheckboxField
        name="creationByAllMembers"
        defaultIsChecked={settings.creationByAllMembers}
      >
        {({ fieldProps }) => (
          <Checkbox
            label="Разрешить создавать задачу всем участникам чата"
            size="large"
            defaultChecked={settings.creationByAllMembers}
            // eslint-disable-next-line react/jsx-props-no-spreading
            {...fieldProps}
          />
        )}
      </CheckboxField>

      <Field
        label="Автор задачи"
        name="reporter"
        defaultValue={settings.reporter || 'INITIATOR'}
        isRequired
        validate={validateNotNull}
      >
        {({ fieldProps: { id, onChange }, error }) => (
          <>
            <Select
              inputId={id}
              defaultValue={[
                {
                  label: 'Автор оригинального сообщения',
                  value: 'MESSAGE_AUTHOR',
                },
                { label: 'Инициатор создания задачи', value: 'INITIATOR' },
              ].find(
                (value) => value.value === (settings.reporter || 'INITIATOR'),
              )}
              options={[
                {
                  label: 'Автор оригинального сообщения',
                  value: 'MESSAGE_AUTHOR',
                },
                { label: 'Инициатор создания задачи', value: 'INITIATOR' },
              ]}
              placeholder="Автор задачи"
              onChange={(value: any) => value && onChange(value.value)}
            />
            {error && <ErrorMessage>{error}</ErrorMessage>}
          </>
        )}
      </Field>
      <CheckboxField
        name="addReporterInWatchers"
        defaultIsChecked={settings.addReporterInWatchers}
      >
        {({ fieldProps }) => (
          <Checkbox
            label="Добавлять автора в Наблюдатели"
            size="large"
            defaultChecked={settings.addReporterInWatchers}
            // eslint-disable-next-line react/jsx-props-no-spreading
            {...fieldProps}
          />
        )}
      </CheckboxField>
      <h3>Шаблоны</h3>
      <LineHelperMessage>{`Текст шаблона может содержать ключи для автоподстановки, где {{key}} будет заменен на соответствующее значение.`}</LineHelperMessage>
      <Field
        label="Шаблон сообщения о созданной задаче"
        name="creationSuccessTemplate"
        defaultValue={settings.creationSuccessTemplate}
      >
        {({ fieldProps, error }) => (
          <>
            {/* eslint-disable-next-line react/jsx-props-no-spreading */}
            <TextArea {...(fieldProps as any)} resize="auto" />
            <LineHelperMessage>{`issueKey - ключ задачи со ссылкой;
            issueLink - полная ссылка на задачу;
            summary - тема задачи.`}</LineHelperMessage>
            {error && <ErrorMessage>{error}</ErrorMessage>}
          </>
        )}
      </Field>
      <Field
        label="Шаблон темы созданной задачи"
        name="issueSummaryTemplate"
        defaultValue={settings.issueSummaryTemplate}
      >
        {({ fieldProps, error }) => (
          <>
            {/* eslint-disable-next-line react/jsx-props-no-spreading */}
            <TextArea {...(fieldProps as any)} resize="auto" />
            <LineHelperMessage>{`author - автор оригинального сообщения;
            initiator - инициатор создания задачи.`}</LineHelperMessage>
            {error && <ErrorMessage>{error}</ErrorMessage>}
          </>
        )}
      </Field>
    </>
  );
};

// eslint-disable-next-line sonarjs/cognitive-complexity
const renderAdditionalFields = (state: LoadableDataState<Array<FieldHtml>>) => {
  return (
    <>
      {(state.data && state.data.length > 0) || state.isLoading ? (
        <h3>Дополнительные поля</h3>
      ) : null}

      <LoadableComponent isLoading={state.isLoading}>
        {state.data && state.data.length > 0
          ? [...state.data]
              .sort((a, b) => {
                const isRequired = a.required ? -1 : 1;
                if (a.label < b.label) {
                  return isRequired || -1;
                }
                if (a.label > b.label) {
                  return isRequired || 1;
                }
                return a.label > b.label ? -1 : 1;
              })
              .map((f) => {
                return (
                  <div
                    className="field-group"
                    key={f.id}
                    dangerouslySetInnerHTML={{ __html: f.editHtml }}
                  />
                );
              })
          : null}
      </LoadableComponent>
    </>
  );
};

function EditIssueCreationSettingsForm({
  defaultSettings,
  onSave,
}: Props): ReactElement {
  const [selectedMainData, setSelectedMainData] = useState<MainState>({
    projectKey: defaultSettings.projectKey,
    issueTypeId: defaultSettings.issueTypeId,
  });

  const [requiredFieldsState, setRequiredFieldsState] = useState<
    LoadableDataState<Array<FieldHtml>>
  >({
    isLoading: false,
  });

  useEffect(() => {
    if (selectedMainData.issueTypeId && selectedMainData.projectKey) {
      setRequiredFieldsState({ isLoading: true });
      loadIssueForm(
        selectedMainData.issueTypeId,
        selectedMainData.projectKey,
        defaultSettings.additionalFields || [],
      )
        .then(({ data }) => {
          setRequiredFieldsState({
            isLoading: false,
            data: data.fields.filter((f) => !isIgnoredField(f.id)),
          });
          Events.trigger(Types.NEW_CONTENT_ADDED, [
            $('form.aui'),
            Reasons.dialogReady,
          ]);
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
        onSubmit={({
          enabled,
          issueTypeId,
          projectKey,
          tag,
          labels,
          creationByAllMembers,
          reporter,
          addReporterInWatchers,
          creationSuccessTemplate,
          issueSummaryTemplate,
        }: FormState) => {
          onSave({
            enabled,
            tag,
            creationByAllMembers,
            reporter,
            addReporterInWatchers,
            creationSuccessTemplate,
            issueSummaryTemplate: issueSummaryTemplate
              ? issueSummaryTemplate.replace('\n', '')
              : undefined,
            projectKey: String(projectKey.value),
            issueTypeId: String(issueTypeId.value),
            labels: labels ? labels.map((l) => String(l.value)) : [],
            additionalFields: getFormValues(),
          });
        }}
      >
        {({ formProps }) => (
          <form
            // eslint-disable-next-line react/jsx-props-no-spreading
            {...formProps}
            className="aui top-label"
            name="jiraform"
            id={FORM_ID}
          >
            <div className="form-body">
              {renderMainFields(
                defaultSettings,
                selectedMainData,
                setSelectedMainData,
              )}

              {renderAdditionalSettings(defaultSettings)}

              {renderAdditionalFields(requiredFieldsState)}
            </div>
          </form>
        )}
      </Form>
    </Container>
  );
}

EditIssueCreationSettingsForm.defaultProps = {
  onCancel: undefined,
};

export default EditIssueCreationSettingsForm;
