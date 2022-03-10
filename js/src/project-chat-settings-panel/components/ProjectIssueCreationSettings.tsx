import React, { ReactElement, useLayoutEffect, useState } from 'react';
import { loadProjectChatIssueCreationSettings } from '../../shared/api/SettingsApiClient';
import styled from 'styled-components';
import { IssueCreationSettings } from '../../shared/types';
import EditIcon from '@atlaskit/icon/glyph/edit';
import EditIssueCreationSettingsDialog from '../../shared/components/EditIssueCreationSettingsDialog';
import contextPath from 'wrm/context-path';
import { ChatName } from '../../shared/components/ChatName';

const Container = styled.div`
  h2 {
    margin-bottom: 20px;
  }
`;

const Settings = styled.div`
  background-color: var(--aui-item-selected-bg);
  padding: 10px;
  margin: 15px;
  border-radius: 10px;

  h3 {
    margin-bottom: 10px;
    font-weight: 700;
  }
`;

const ChatLink = styled.a`
  font-weight: 700;
  font-size: 14px;
`;

const ClickableIconContainer = styled.div`
  cursor: pointer;
  &:hover * {
    color: #005be6;
  }
`;

const SpaceBetweenRow = styled.div`
  justify-content: space-between;
  display: flex;
`;

const Field = styled.div`
  margin: 5px 0 0 5px;
  label {
    font-weight: 600;
    padding-right: 5px;
  }
`;

const renderSettingsElement = (settings: IssueCreationSettings, onEdit: (settingsId: number) => void) => {
  return (
    <Settings>
      <SpaceBetweenRow>
        <ChatName
          chatTitle={settings.chatTitle || 'Неизвестно'}
          href={`${contextPath()}/myteam/chats/settings?chatId=${settings.chatId}`}
          disabled={!settings.canEdit}
        />
        {settings.canEdit ? (
          <ClickableIconContainer onClick={() => onEdit(settings.id)}>
            <EditIcon size="medium" label="" />
          </ClickableIconContainer>
        ) : null}
      </SpaceBetweenRow>

      <Field>
        <label>Включен:</label>
        <span>{settings.enabled ? 'да' : 'нет'}</span>
      </Field>
      <Field>
        <label>Тег: </label>
        <span>{settings.tag}</span>
      </Field>
      <Field>
        <label>Проект:</label>
        <span>{settings.projectKey}</span>
      </Field>
      <Field>
        <label>Тип задачи:</label>
        <span>{settings.issueTypeName}</span>
      </Field>
      <SpaceBetweenRow>
        <Field>
          <label>Метки:</label>
          <span>{settings.labels ? settings.labels.join(', ') : ''}</span>
        </Field>
        <ChatLink target="_blank" href={settings.chatLink} rel="noreferrer">
          Открыть в VK Teams
        </ChatLink>
      </SpaceBetweenRow>
    </Settings>
  );
};

const ProjectIssueCreationSettings = (): ReactElement => {
  const [settings, setSettings] = useState<Array<IssueCreationSettings>>([]);
  const [editSettingsDialogState, setEditSettingsDialogState] = useState<{ isOpen: boolean; settingsId?: number }>({
    isOpen: false,
  });

  useLayoutEffect(() => {
    const match = location.pathname.match(/myteam\/projects\/(\d+)\/settings\/chats/);
    if (match) {
      loadProjectChatIssueCreationSettings(match[1]).then((response) => setSettings(response.data));
    }
  }, []);

  return (
    <Container>
      <h2>Настройки создания задач</h2>

      {settings && settings.length > 0 ? (
        settings.map((s) =>
          renderSettingsElement(s, (settingsId) => setEditSettingsDialogState({ isOpen: true, settingsId })),
        )
      ) : (
        <h4>Нет настроек для данного проекта</h4>
      )}
      {editSettingsDialogState && editSettingsDialogState.settingsId ? (
        <EditIssueCreationSettingsDialog
          settingsId={editSettingsDialogState.settingsId}
          isOpen={editSettingsDialogState.isOpen}
          onClose={() => setEditSettingsDialogState({ isOpen: false })}
        />
      ) : null}
    </Container>
  );
};

export default ProjectIssueCreationSettings;
