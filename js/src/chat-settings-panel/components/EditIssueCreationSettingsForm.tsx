import React, { ReactElement } from 'react';
import { IssueCreationSettings } from '../types';

type Props = {
  settings: IssueCreationSettings;
};

const EditIssueCreationSettingsForm = ({ settings }: Props): ReactElement => {
  return <div>{settings.chatId}</div>;
};

export default EditIssueCreationSettingsForm;
