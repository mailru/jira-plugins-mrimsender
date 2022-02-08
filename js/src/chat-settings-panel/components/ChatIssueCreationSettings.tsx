import styled from '@emotion/styled';
import React, { ReactElement, useLayoutEffect, useState } from 'react';
import { loadChatIssueCreationSettings } from '../api/SettingsApiClient';
import { IssueCreationSettings } from '../types';
import EditIssueCreationSettingsForm from './EditIssueCreationSettingsForm';

type Props = {
  chatId: string | null;
};

const Container = styled.div`
  & > h2 {
    margin-bottom: 20px;
  }
`;

const ChatIssueCreationSettings = ({ chatId }: Props): ReactElement => {
  const [settings, setsettings] = useState<IssueCreationSettings>();
  useLayoutEffect(() => {
    if (chatId) {
      loadChatIssueCreationSettings(chatId).then(setsettings).catch(console.error);
    }
  }, []);

  return (
    <Container>
      <h2>{`Настройки для чата ${chatId}`}</h2>
      {settings ? <EditIssueCreationSettingsForm defaultSettings={settings} onSave={console.log} /> : null}
    </Container>
  );
};

export default ChatIssueCreationSettings;
