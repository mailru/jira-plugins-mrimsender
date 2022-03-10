import SectionMessage from '@atlaskit/section-message';
import styled from '@emotion/styled';
import React, { ReactElement, useLayoutEffect, useState } from 'react';
import { useTimeoutState } from '../../shared/hooks';
import { loadChatIssueCreationSettings, updateChatIssueCreationSettings } from '../../shared/api/SettingsApiClient';
import { IssueCreationSettings } from '../../shared/types';
import EditIssueCreationSettingsForm from '../../shared/components/EditIssueCreationSettingsForm';
import { ChatName } from '../../shared/components/ChatName';

type Props = {
  chatId: string | null;
};

const Container = styled.div`
  max-width: 600px;
  flex: 1;

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
      loadChatIssueCreationSettings(chatId)
        .then((response) => setSettings(response.data))
        .catch(console.error);
    }
  }, []);

  return (
    <Container>
      <h2>
        {`Настройки для чата `}
        {settings ? (
          <ChatName
            chatTitle={settings.chatTitle || 'Неизвестно'}
            href={settings.chatLink}
            disabled={!settings.canEdit}
          />
        ) : (
          ''
        )}
      </h2>

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
              .then(() => {
                window.scrollTo({ top: 0 });
                setStatus(Status.Success, 3000);
              })
              .catch(() => setStatus(Status.Error, 3000));
          }}
        />
      ) : null}
    </Container>
  );
};

export default ChatIssueCreationSettings;
