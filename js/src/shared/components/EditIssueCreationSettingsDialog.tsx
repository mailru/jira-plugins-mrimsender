import Button from '@atlaskit/button';
import Modal, { ModalBody, ModalFooter, ModalHeader, ModalTitle, ModalTransition } from '@atlaskit/modal-dialog';
import SectionMessage from '@atlaskit/section-message';
import React, { ReactElement, useLayoutEffect, useState } from 'react';
import { loadChatIssueCreationSettingsById, updateChatIssueCreationSettings } from '../api/SettingsApiClient';
import { IssueCreationSettings, LoadableDataState } from '../types';
import EditIssueCreationSettingsForm from './EditIssueCreationSettingsForm';
import LoadableComponent from './LoadableComponent';

type Props = {
  className?: string;
  settingsId: number;

  isOpen: boolean;
  onClose: () => void;
  onSaveSuccess?: () => void;
};

enum Status {
  Success,
  Error,
}

const EditIssueCreationSettingsDialog = ({ settingsId, isOpen, onClose, onSaveSuccess }: Props): ReactElement => {
  const [settings, setSettings] = useState<LoadableDataState<IssueCreationSettings>>({ isLoading: false });
  const [status, setStatus] = useState<Status | null>(null);
  useLayoutEffect(() => {
    if (settingsId) {
      setSettings({ isLoading: true });
      loadChatIssueCreationSettingsById(settingsId)
        .then((response) => setSettings({ isLoading: false, data: response.data }))
        .catch((e) => {
          console.error(e);
          setSettings({ isLoading: false, error: JSON.stringify(e) });
        });
    }
  }, []);
  return (
    <ModalTransition>
      {isOpen && (
        <Modal onClose={onClose}>
          <ModalHeader>
            <ModalTitle>Редактирование настроек {settings.data?.chatId}</ModalTitle>
          </ModalHeader>
          <ModalBody>
            {status !== null ? (
              <SectionMessage appearance={status === Status.Success ? 'success' : 'error'}>
                <p>{status === Status.Success ? 'Настройки успешно сохранены.' : 'Возникла ошибка на сервере.'}</p>
              </SectionMessage>
            ) : (
              <LoadableComponent isLoading={settings.isLoading} error={settings.error}>
                <EditIssueCreationSettingsForm
                  defaultSettings={settings.data || {}}
                  onCancel={() => {
                    onClose();
                    setStatus(null);
                  }}
                  onSave={(newSettings) => {
                    setStatus(null);
                    settings.data &&
                      updateChatIssueCreationSettings(settings.data?.id, { ...settings.data, ...newSettings })
                        .then(() => {
                          setStatus(Status.Success);
                        })
                        .catch(() => setStatus(Status.Error));
                  }}
                />
              </LoadableComponent>
            )}
          </ModalBody>
          {status !== null ? (
            <ModalFooter>
              <Button
                appearance="primary"
                autoFocus
                onClick={() => {
                  onClose();
                  onSaveSuccess && onSaveSuccess();
                  setStatus(null);
                }}>
                Ок
              </Button>
            </ModalFooter>
          ) : null}
        </Modal>
      )}
    </ModalTransition>
  );
};

export default EditIssueCreationSettingsDialog;
