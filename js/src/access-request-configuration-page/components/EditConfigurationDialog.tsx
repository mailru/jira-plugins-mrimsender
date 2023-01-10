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
  currentValue: AccessRequestConfiguration;
  onClose: () => void;
  onSaveSuccess: (configuration: AccessRequestConfiguration) => void;
  editingError?: ErrorData;
};

function EditConfigurationDialog({
  projectKey,
  isOpen,
  currentValue,
  onClose,
  onSaveSuccess,
  editingError,
}: Props): ReactElement {
  return (
    <ModalTransition>
      {isOpen && (
        <Modal onClose={onClose} width="medium">
          <ModalHeader>
            <ModalTitle>
              {I18n.getText(
                'ru.mail.jira.plugins.myteam.accessRequest.configuration.page.dialog.edit.title',
              )}
            </ModalTitle>
          </ModalHeader>
          {editingError &&
          editingError.error &&
          editingError.fieldErrors === undefined ? (
            <SectionMessage appearance="error">
              <p>{editingError.error}</p>
            </SectionMessage>
          ) : null}
          <ModalBody>
            <ConfigurationForm
              projectKey={projectKey}
              currentValue={currentValue}
              onCancel={onClose}
              onSave={onSaveSuccess}
              submitError={editingError}
            />
          </ModalBody>
          <ModalFooter>
            <Button form={FORM_ID} type="submit" appearance="primary" autoFocus>
              {I18n.getText('common.words.update')}
            </Button>
            <Button form={FORM_ID} appearance="subtle" onClick={onClose}>
              {I18n.getText('common.forms.cancel')}
            </Button>
          </ModalFooter>
        </Modal>
      )}
    </ModalTransition>
  );
}

EditConfigurationDialog.defaultProps = {
  editingError: undefined,
};

export default EditConfigurationDialog;
