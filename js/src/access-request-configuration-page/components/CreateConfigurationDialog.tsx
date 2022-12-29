import Button from '@atlaskit/button';
import Modal, {
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalTitle,
  ModalTransition,
} from '@atlaskit/modal-dialog';
import React, { ReactElement } from 'react';
import { I18n } from '@atlassian/wrm-react-i18n';
import SectionMessage from '@atlaskit/section-message';
import ConfigurationForm, { FORM_ID } from './ConfigurationForm';
import { AccessRequestConfiguration, ErrorData } from '../../shared/types';

type Props = {
  projectKey: string;
  isOpen: boolean;
  onClose: () => void;
  onSaveSuccess: (configuration: AccessRequestConfiguration) => void;
  creationError?: ErrorData;
};

function CreateConfigurationDialog({
  projectKey,
  isOpen,
  onClose,
  onSaveSuccess,
  creationError,
}: Props): ReactElement {
  return (
    <ModalTransition>
      {isOpen && (
        <Modal onClose={onClose} width="medium">
          <ModalHeader>
            <ModalTitle>
              {I18n.getText(
                'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.dialog.create.title',
              )}
            </ModalTitle>
          </ModalHeader>
          {creationError &&
          creationError.error &&
          creationError.fieldErrors === undefined ? (
            <SectionMessage appearance="error">
              <p>{creationError.error}</p>
            </SectionMessage>
          ) : null}
          <ModalBody>
            <ConfigurationForm
              projectKey={projectKey}
              onCancel={() => onClose()}
              onSave={(configuration) => onSaveSuccess(configuration)}
              submitError={creationError}
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

CreateConfigurationDialog.defaultProps = {
  creationError: undefined,
};

export default CreateConfigurationDialog;
