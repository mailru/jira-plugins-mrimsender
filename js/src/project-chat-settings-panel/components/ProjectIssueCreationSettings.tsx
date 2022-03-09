import React, { ReactElement, useLayoutEffect, useState } from 'react';
import { loadProjectChatIssueCreationSettings } from '../../shared/api/SettingsApiClient';
import styled from 'styled-components';
import { IssueCreationSettings } from 'src/shared/types';
import EditIcon from '@atlaskit/icon/glyph/edit';

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
  margin-bottom: 10px;
  font-weight: 700;
  font-size: 16px;
`;

const ClickableIconContainer = styled.div`
  cursor: pointer;
  &:hover * {
    color: #0747a6;
  }
`;

const TitleRow = styled.div`
  justify-content: space-between;
  padding-left: 10px;
  display: flex;
`;

const Field = styled.div`
  margin-top: 5px;
  label {
    font-weight: 600;
    padding-right: 5px;
  }
`;

const renderSettingsElement = (settings: IssueCreationSettings) => {
  return (
    <Settings>
      <TitleRow>
        <ChatLink target="_blank" href={settings.chatLink} rel="noreferrer">
          {settings.chatId}
        </ChatLink>
        <ClickableIconContainer>
          <EditIcon size="medium" label="" />
        </ClickableIconContainer>
      </TitleRow>

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
        <span>{settings.issueTypeId}</span>
      </Field>
      <Field>
        <label>Метки:</label>
        <span>{settings.labels.join(', ')}</span>
      </Field>
    </Settings>
  );
};

const ProjectIssueCreationSettings = (): ReactElement => {
  useLayoutEffect(() => {
    const match = location.pathname.match(/myteam\/projects\/(\d+)\/settings\/chats/);
    if (match) {
      loadProjectChatIssueCreationSettings(match[1]).then((response) => setSettings(response.data));
    }
  }, []);

  const [settings, setSettings] = useState<Array<IssueCreationSettings>>([]);

  return (
    <Container>
      <h2>Настройки создания задач</h2>

      {settings.map(renderSettingsElement)}
    </Container>
  );
};

export default ProjectIssueCreationSettings;
