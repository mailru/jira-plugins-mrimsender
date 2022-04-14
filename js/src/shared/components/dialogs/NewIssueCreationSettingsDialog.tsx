import Button from '@atlaskit/button';
import Modal, {
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalTitle,
  ModalTransition,
} from '@atlaskit/modal-dialog';
import SectionMessage from '@atlaskit/section-message';
import React, { ReactElement } from 'react';
import { I18n } from '@atlassian/wrm-react-i18n';
import { IssueCreationSettings } from '../../types';
import EditIssueCreationSettingsForm, {
  FORM_ID,
} from '../EditIssueCreationSettingsForm';
import { createChatIssueCreationSettings } from '../../api/SettingsApiClient';
import { useTimeoutState } from '../../hooks';

type Props = {
  chatId: string;
  isOpen: boolean;
  onClose: () => void;
  onSaveSuccess: () => void;
};

enum Status {
  Success,
  Error,
  None,
}

const DEFAULT_SETTINGS: Partial<IssueCreationSettings> = {
  enabled: true,
  tag: 'task',
};

function NewIssueCreationSettingsDialog({
  isOpen,
  chatId,
  onClose,
  onSaveSuccess,
}: Props): ReactElement {
  const [statusState, setStatus] = useTimeoutState<{
    status: Status | null;
    error?: string;
  }>({ status: Status.None });

  return (
    <ModalTransition>
      {isOpen && (
        <Modal onClose={onClose}>
          <ModalHeader>
            <ModalTitle>Новые настройки для чата</ModalTitle>
          </ModalHeader>
          {statusState.status !== Status.None ? (
            <SectionMessage
              appearance={
                statusState.status === Status.Success ? 'success' : 'error'
              }
            >
              <p>
                {statusState.status === Status.Success
                  ? 'Настройки успешно сохранены.'
                  : statusState.error}
              </p>
            </SectionMessage>
          ) : null}
          <ModalBody>
            <EditIssueCreationSettingsForm
              defaultSettings={DEFAULT_SETTINGS}
              onCancel={() => {
                onClose();
              }}
              onSave={(settings) => {
                if (chatId) {
                  const issueCreationSettings =
                    settings as IssueCreationSettings;
                  issueCreationSettings.chatId = chatId;
                  createChatIssueCreationSettings(issueCreationSettings)
                    .then(() => {
                      onSaveSuccess();
                    })
                    .catch((e) => {
                      setStatus(
                        { status: Status.Error, error: e.response.data.error },
                        5000,
                      );
                    });
                }
              }}
            />
          </ModalBody>
          <ModalFooter>
            <Button form={FORM_ID} type="submit" appearance="primary" autoFocus>
              {I18n.getText('common.forms.create')}
            </Button>
            <Button
              form={FORM_ID}
              appearance="subtle"
              onClick={() => {
                onClose();
                setStatus({ status: Status.None }, 0);
              }}
            >
              {I18n.getText('common.forms.cancel')}
            </Button>
          </ModalFooter>
        </Modal>
      )}
    </ModalTransition>
  );
}

export default NewIssueCreationSettingsDialog;
