import SectionMessage from '@atlaskit/section-message';
import styled from '@emotion/styled';
import React, { ReactElement, useLayoutEffect, useState } from 'react';
import { useTimeoutState } from '../shared/hooks';
import { loadChatIssueCreationSettings, updateChatIssueCreationSettings } from '../api/SettingsApiClient';
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

export enum Status {
  Success,
  Error,
}

const ChatIssueCreationSettings = ({ chatId }: Props): ReactElement => {
  const [settings, setSettings] = useState<IssueCreationSettings>();
  const [status, setStatus] = useTimeoutState<Status | null>(null);

  useLayoutEffect(() => {
    if (chatId) {
      loadChatIssueCreationSettings(chatId).then(setSettings).catch(console.error);
    }
  }, []);

  return (
    <Container>
      <h2>{`Настройки для чата ${chatId}`}</h2>

      {status !== null ? (
        <SectionMessage appearance={status === Status.Success ? 'success' : 'error'}>
          <p>{status === Status.Success ? 'Настройки успешно сохранены.' : 'Возникла ошибка на сервере.'}</p>
        </SectionMessage>
      ) : null}
      {settings ? (
        <EditIssueCreationSettingsForm
          defaultSettings={settings}
          onSave={(newSettings) => {
            updateChatIssueCreationSettings(settings.id, { ...settings, ...newSettings })
              .then(() => setStatus(Status.Success, 3000))
              .catch(() => setStatus(Status.Error, 3000));
          }}
        />
      ) : null}
    </Container>
  );
};

export default ChatIssueCreationSettings;
