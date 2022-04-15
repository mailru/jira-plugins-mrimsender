import React, { ReactElement, useLayoutEffect, useState } from 'react';
import styled from 'styled-components';
import EditIcon from '@atlaskit/icon/glyph/edit';
import TrashIcon from '@atlaskit/icon/glyph/trash';
import Button from '@atlaskit/button';
import { I18n } from '@atlassian/wrm-react-i18n';
import {
  deleteChatIssueCreationSettings,
  loadChatIssueCreationSettings,
} from '../../shared/api/SettingsApiClient';
import { IssueCreationSettings, LoadableDataState } from '../../shared/types';
import EditIssueCreationSettingsDialog from '../../shared/components/dialogs/EditIssueCreationSettingsDialog';
import ChatName from '../../shared/components/ChatName';
import LoadableComponent from '../../shared/components/LoadableComponent';
import NewIssueCreationSettingsDialog from '../../shared/components/dialogs/NewIssueCreationSettingsDialog';
import ConfirmationDialog from '../../shared/components/dialogs/ConfirmationDialog';

type Props = {
  chatId: string | null;
};

const Settings = styled.div`
  background-color: #f4f5f7;
  padding: 10px;
  margin: 10px 25px;
  box-sizing: border-box;
  border-radius: 10px;
  width: 100%;

  h3 {
    margin-bottom: 10px;
    font-weight: 700;
  }
`;
const TitleContainer = styled.div`
  display: flex;
  justify-content: space-between;
`;

const ClickableEditIconContainer = styled.span`
  cursor: pointer;
  &:hover * {
    color: #005be6;
  }
`;

const ClickableRemoveIconContainer = styled.span`
  cursor: pointer;
  margin-left: 5px;
  &:hover * {
    color: #bf2600;
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

const Container = styled.div`
  max-width: 600px;
  flex: 1;

  & > ${TitleContainer} {
    margin-bottom: 20px;
  }
`;

const renderSettingsElement = (
  settings: IssueCreationSettings,
  onEdit: (settingsId: number) => void,
  onDelete: (settingsId: number) => void,
) => {
  return (
    <Settings>
      <SpaceBetweenRow>
        <ChatName chatTitle={`#${settings.tag}`} disabled={!settings.canEdit} />
        {settings.canEdit ? (
          <div>
            <ClickableEditIconContainer onClick={() => onEdit(settings.id)}>
              <EditIcon size="medium" label="" />
            </ClickableEditIconContainer>
            <ClickableRemoveIconContainer onClick={() => onDelete(settings.id)}>
              <TrashIcon size="medium" label="" />
            </ClickableRemoveIconContainer>
          </div>
        ) : null}
      </SpaceBetweenRow>

      <Field>
        {/* eslint-disable-next-line jsx-a11y/label-has-associated-control */}
        <label>Включен:</label>
        <span>{settings.enabled ? 'да' : 'нет'}</span>
      </Field>
      <Field>
        {/* eslint-disable-next-line jsx-a11y/label-has-associated-control */}
        <label>Проект:</label>
        <span>{settings.projectKey}</span>
      </Field>
      <Field>
        {/* eslint-disable-next-line jsx-a11y/label-has-associated-control */}
        <label>Тип задачи:</label>
        <span>{settings.issueTypeName}</span>
      </Field>

      <Field>
        {/* eslint-disable-next-line jsx-a11y/label-has-associated-control */}
        <label>Метки:</label>
        <span>{settings.labels ? settings.labels.join(', ') : ''}</span>
      </Field>
    </Settings>
  );
};

function ChatIssueCreationSettings({ chatId }: Props): ReactElement {
  const [settings, setSettings] = useState<
    LoadableDataState<Array<IssueCreationSettings>>
  >({
    isLoading: false,
  });
  const [editSettingsDialogState, setEditSettingsDialogState] = useState<{
    isOpen: boolean;
    settingsId?: number;
  }>({
    isOpen: false,
  });

  const [newSettingsDialogState, setNewSettingsDialogState] = useState<{
    isOpen: boolean;
  }>({
    isOpen: false,
  });

  const [confirmationDialogState, setConfirmationDialogState] = useState<{
    isOpen: boolean;
    settingsId?: number;
  }>({
    isOpen: false,
  });

  const loadSettings = () => {
    if (chatId) {
      loadChatIssueCreationSettings(chatId)
        .then((response) =>
          setSettings({ data: response.data, isLoading: false }),
        )
        .catch((e) => {
          console.error(e);
          setSettings({ isLoading: false, error: JSON.stringify(e) });
        });
    }
  };

  useLayoutEffect(loadSettings, []);

  const firstSettings = settings.data ? settings.data[0] : null;

  return (
    <Container>
      <TitleContainer>
        <h2>
          Настройки создания задач{' '}
          <ChatName
            chatTitle={firstSettings?.chatTitle || ''}
            href={firstSettings?.chatLink}
          />
        </h2>
        <Button
          appearance="primary"
          onClick={() => setNewSettingsDialogState({ isOpen: true })}
        >
          {I18n.getText('common.forms.create')}
        </Button>
      </TitleContainer>

      <LoadableComponent isLoading={settings.isLoading}>
        {settings.data && settings.data.length > 0 ? (
          settings.data.map((s) =>
            renderSettingsElement(
              s,
              (settingsId) =>
                setEditSettingsDialogState({ isOpen: true, settingsId }),
              (settingsId) => {
                setConfirmationDialogState({ settingsId, isOpen: true });
              },
            ),
          )
        ) : (
          <h4>Нет настроек для данного чата</h4>
        )}
        {editSettingsDialogState && editSettingsDialogState.settingsId ? (
          <EditIssueCreationSettingsDialog
            settingsId={editSettingsDialogState.settingsId}
            isOpen={editSettingsDialogState.isOpen}
            onClose={() => setEditSettingsDialogState({ isOpen: false })}
            onSaveSuccess={() => {
              setEditSettingsDialogState({ isOpen: false });
              loadSettings();
            }}
          />
        ) : null}

        {chatId ? (
          <NewIssueCreationSettingsDialog
            chatId={chatId}
            isOpen={newSettingsDialogState.isOpen}
            onClose={() => setNewSettingsDialogState({ isOpen: false })}
            onSaveSuccess={() => {
              setNewSettingsDialogState({ isOpen: false });
              loadSettings();
            }}
          />
        ) : null}
        <ConfirmationDialog
          isOpen={confirmationDialogState.isOpen}
          title="Подтверждение действия"
          body="Вы действительно хотите удалить данные настройки?"
          onOk={() => {
            if (confirmationDialogState.settingsId) {
              deleteChatIssueCreationSettings(
                confirmationDialogState.settingsId,
              ).then(() => {
                loadSettings();
                setConfirmationDialogState({ isOpen: false });
              });
            }
          }}
          onCancel={() => {
            setConfirmationDialogState({ isOpen: false });
          }}
        />
      </LoadableComponent>
    </Container>
  );
}

export default ChatIssueCreationSettings;
