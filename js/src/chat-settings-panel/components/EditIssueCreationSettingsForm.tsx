import React, { Fragment, ReactElement } from 'react';
import { IssueCreationSettings } from '../types';
import Textfield from '@atlaskit/textfield';
import Form, { Field, FormFooter, HelperMessage } from '@atlaskit/form';
import Button from '@atlaskit/button';
import LabelsSelect from './LabelsSelect';
import ProjectSelect from './ProjectSelect';
import IssueTypeSelect from './IssueTypeSelect';

type Props = {
  settings: IssueCreationSettings;
};

const EditIssueCreationSettingsForm = ({ settings }: Props): ReactElement => {
  return (
    <Form onSubmit={(formState: unknown) => console.log('form submitted', formState)}>
      {({ formProps }: any) => (
        <form {...formProps}>
          <ProjectSelect title="Выберите проект" onChange={console.log} />

          <IssueTypeSelect projectKey="DEV" title="Выберите проект" onChange={console.log} />

          <Field label="Field label" name="example-text">
            {({ fieldProps }: any) => (
              <Fragment>
                <Textfield placeholder="Enter your details here" {...fieldProps} />
                <HelperMessage>Ключ проекта</HelperMessage>
              </Fragment>
            )}
          </Field>
          <Field label="Field label" name="example-text">
            {({ fieldProps }: any) => (
              <Fragment>
                <Textfield placeholder="Enter your details here" {...fieldProps} />
                <HelperMessage>Ключ проекта</HelperMessage>
              </Fragment>
            )}
          </Field>
          <Field label="Field label" name="example-text">
            {({ fieldProps }: any) => (
              <Fragment>
                <Textfield placeholder="Enter your details here" {...fieldProps} />
                <HelperMessage>Ключ проекта</HelperMessage>
              </Fragment>
            )}
          </Field>
          <Field label="Field label" name="example-text">
            {({ fieldProps }: any) => (
              <Fragment>
                <Textfield placeholder="Enter your details here" {...fieldProps} />
                <HelperMessage>Ключ проекта</HelperMessage>
              </Fragment>
            )}
          </Field>
          <>
            <label htmlFor="multi-select-example">What cities have you lived in?</label>
            <LabelsSelect onChange={console.log} />
          </>

          <FormFooter>
            <Button type="submit" appearance="primary">
              Submit
            </Button>
          </FormFooter>
        </form>
      )}
    </Form>
  );
};

export default EditIssueCreationSettingsForm;
