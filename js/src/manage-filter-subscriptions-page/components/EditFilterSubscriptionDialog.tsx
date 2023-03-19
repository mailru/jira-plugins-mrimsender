import Button from '@atlaskit/button'
import Modal, {
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalTitle,
  ModalTransition,
} from '@atlaskit/modal-dialog'
import React, { ReactElement } from 'react'
import { I18n } from '@atlassian/wrm-react-i18n'
import SectionMessage from '@atlaskit/section-message'
import FilterSubscriptionForm, { FORM_ID } from './FilterSubscriptionForm'
import { ErrorData, FilterSubscription } from '../../shared/types'

type Props = {
  isOpen: boolean
  currentValue: FilterSubscription
  onClose: () => void
  onSaveSuccess: (subscription: FilterSubscription) => void
  editingError?: ErrorData
}

function EditFilterSubscriptionDialog({
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
                'ru.mail.jira.plugins.myteam.subscriptions.page.subscription.edit'
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
            <FilterSubscriptionForm
              currentValue={currentValue}
              onCancel={() => onClose()}
              onSave={(subscription) => onSaveSuccess(subscription)}
              submitError={editingError}
            />
          </ModalBody>
          <ModalFooter>
            <Button form={FORM_ID} type="submit" appearance="primary" autoFocus>
              {I18n.getText('common.words.update')}
            </Button>
            <Button
              form={FORM_ID}
              appearance="subtle"
              onClick={() => {
                onClose()
              }}
            >
              {I18n.getText('common.forms.cancel')}
            </Button>
          </ModalFooter>
        </Modal>
      )}
    </ModalTransition>
  )
}

EditFilterSubscriptionDialog.defaultProps = {
  editingError: undefined,
}

export default EditFilterSubscriptionDialog
