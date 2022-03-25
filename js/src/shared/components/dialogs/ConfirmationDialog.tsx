import React, { ReactElement } from 'react';
import Button from '@atlaskit/button/standard-button';
import { I18n } from '@atlassian/wrm-react-i18n';
import Modal, { ModalBody, ModalFooter, ModalHeader, ModalTitle, ModalTransition } from '@atlaskit/modal-dialog';

type Props = {
  title: string | ReactElement;
  body: string | ReactElement;
  isOpen: boolean;
  onOk: () => void;
  onCancel: () => void;
};

const ConfirmationDialog = ({ title, body, isOpen, onOk, onCancel }: Props): ReactElement => {
  return (
    <ModalTransition>
      {isOpen && (
        <Modal onClose={onCancel}>
          <ModalHeader>
            <ModalTitle>{title}</ModalTitle>
          </ModalHeader>
          <ModalBody>{body}</ModalBody>
          <ModalFooter>
            <Button appearance="primary" onClick={onOk}>
            {I18n.getText('common.forms.confirm')}
            </Button>
            <Button appearance="subtle" onClick={onCancel} autoFocus>
              {I18n.getText('common.forms.cancel')}
            </Button>
          </ModalFooter>
        </Modal>
      )}
    </ModalTransition>
  );
};

export default ConfirmationDialog;
