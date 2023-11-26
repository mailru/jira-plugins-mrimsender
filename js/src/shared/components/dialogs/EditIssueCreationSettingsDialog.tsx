import Button from '@atlaskit/button'
import Modal, {
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalTitle,
  ModalTransition,
} from '@atlaskit/modal-dialog'
import SectionMessage from '@atlaskit/section-message'
import React, { ReactElement, useLayoutEffect, useState } from 'react'
import { I18n } from '@atlassian/wrm-react-i18n'
import {
  loadChatIssueCreationSettingsById,
  updateChatIssueCreationSettings,
} from '../../api/SettingsApiClient'
import { IssueCreationSettings, LoadableDataState } from '../../types'
import EditIssueCreationSettingsForm, {
  FORM_ID,
} from '../EditIssueCreationSettingsForm'
import LoadableComponent from '../LoadableComponent'
import { useTimeoutState } from '../../hooks'

type Props = {
  settingsId: number

  isOpen: boolean
  onClose: () => void
  onSaveSuccess: () => void
}

enum Status {
  Success,
  Error,
  None,
}

function EditIssueCreationSettingsDialog({
  settingsId,
  isOpen,
  onClose,
  onSaveSuccess,
}: Props): ReactElement {
  const [settings, setSettings] = useState<
    LoadableDataState<IssueCreationSettings>
  >({ isLoading: false })
  const [statusState, setStatus] = useTimeoutState<{
    status: Status | null
    error?: string
  }>({ status: Status.None })
  useLayoutEffect(() => {
    if (settingsId) {
      setSettings({ isLoading: true })
      loadChatIssueCreationSettingsById(settingsId)
        .then((response) =>
          setSettings({ isLoading: false, data: response.data })
        )
        .catch((e) => {
          console.error(e)
          setSettings({ isLoading: false, error: JSON.stringify(e) })
        })
    }
  }, [settingsId])
  return (
    <ModalTransition>
      {isOpen && (
        <Modal onClose={onClose}>
          <ModalHeader>
            <ModalTitle>
              Редактирование настроек {settings.data?.chatTitle}
            </ModalTitle>
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
            <LoadableComponent
              isLoading={settings.isLoading}
              error={settings.error}
            >
              <EditIssueCreationSettingsForm
                defaultSettings={settings.data || {}}
                onCancel={() => {
                  onClose()
                }}
                onSave={(newSettings) =>
                  settings.data &&
                  updateChatIssueCreationSettings(settings.data?.id, {
                    ...settings.data,
                    ...newSettings,
                  })
                    .then(() => {
                      onSaveSuccess()
                    })
                    .catch((e) => {
                      setStatus(
                        {
                          status: Status.Error,
                          error: e.response.data.error,
                        },
                        5000
                      )
                    })
                }
              />
            </LoadableComponent>
          </ModalBody>
          <ModalFooter>
            <Button form={FORM_ID} type="submit" appearance="primary" autoFocus>
              {I18n.getText('common.forms.save')}
            </Button>
            <Button
              form={FORM_ID}
              appearance="subtle"
              onClick={() => {
                onClose()
                setStatus({ status: Status.None }, 0)
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

export default EditIssueCreationSettingsDialog
