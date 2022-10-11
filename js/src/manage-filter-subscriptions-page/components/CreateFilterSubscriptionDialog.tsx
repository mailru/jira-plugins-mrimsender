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
import FilterSubscriptionForm, { FORM_ID } from './FilterSubscriptionForm';
import { ErrorData, FilterSubscription } from '../../shared/types';

type Props = {
  isOpen: boolean;
  onClose: () => void;
  onSaveSuccess: (subscription: FilterSubscription) => void;
  creationError?: ErrorData;
};

function CreateFilterSubscriptionDialog({
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
                'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.create',
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
            <FilterSubscriptionForm
              onCancel={() => onClose()}
              onSave={(subscription) => onSaveSuccess(subscription)}
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

CreateFilterSubscriptionDialog.defaultProps = {
  creationError: undefined,
};

export default CreateFilterSubscriptionDialog;
