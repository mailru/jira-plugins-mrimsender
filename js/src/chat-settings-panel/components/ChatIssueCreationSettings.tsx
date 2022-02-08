import styled from '@emotion/styled';
import React, { ReactElement, useLayoutEffect, useState } from 'react';
import { loadChatIssueCreationSettings } from '../api/SettingsApiClient';
import { IssueCreationSettings } from '../types';
import EditIssueCreationSettingsForm from './EditIssueCreationSettingsForm';

type Props = {
  chatId: string | null;
};

const Container = styled.div``;

const ChatIssueCreationSettings = ({ chatId }: Props): ReactElement => {
  const [settings, setsettings] = useState<IssueCreationSettings>();
  useLayoutEffect(() => {
    if (chatId) {
      loadChatIssueCreationSettings(chatId).then(setsettings).catch(console.error);
    }
  }, []);

  return (
    <Container>
      <div>123</div>
      {settings ? <EditIssueCreationSettingsForm settings={settings} /> : null}
    </Container>
  );
};

export default ChatIssueCreationSettings;
