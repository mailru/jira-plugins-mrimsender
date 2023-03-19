import React, { ReactElement } from 'react'
import Modal, {
  ModalFooter,
  ModalHeader,
  ModalTitle,
  ModalTransition,
} from '@atlaskit/modal-dialog'
import { I18n } from '@atlassian/wrm-react-i18n'
import Form, { ErrorMessage, Field } from '@atlaskit/form'
import Textfield from '@atlaskit/textfield'
import Button, { ButtonGroup } from '@atlaskit/button'
import { ValueType } from '@atlaskit/select'
import UserPicker, { OptionData, Value } from '@atlaskit/user-picker'
import styled from 'styled-components'
import { ChatCreationData } from '../stores/types'

type Props = {
  chatCreationData: ChatCreationData
  isOpen: boolean
  onClose: () => void
  createChat(name: string, memberIds: number[]): void
  loadUsers(): Promise<OptionData | OptionData[]>
}

type CreateChatDialogFormValuesType = {
  'chat-name': string
  // eslint-disable-next-line sonarjs/no-duplicate-string
  'chat-members': ValueType<OptionData, true> // eslint-disable sonarjs/no-duplicate-string
}

const ModalBody = styled.div`
  overflow-x: visible;
  padding: 0 20px;
`

const validateChatName = (value?: string) => {
  if (!value || value.trim().length === 0) {
    return 'TOO_SHORT'
  }
  return undefined
}

const validateChatMembers = (value?: ValueType<OptionData, true>) => {
  if (!value || value.length <= 1) {
    return 'TOO_SHORT'
  }
  if (value && value.length > 30) {
    return 'TOO_MORE'
  }
  return undefined
}

export default function CreateChatDialog({
  isOpen,
  onClose,
  chatCreationData,
  createChat,
  loadUsers,
}: Props): ReactElement {
  return (
    <ModalTransition>
      {isOpen && (
        <Modal onClose={onClose}>
          <Form<CreateChatDialogFormValuesType>
            onSubmit={(values) => {
              if (
                values != null &&
                values['chat-members'] != null &&
                values['chat-name'] != null
              ) {
                createChat(
                  values['chat-name'],
                  values['chat-members'].map((member) =>
                    Number.parseInt(member.id, 10)
                  )
                )
                onClose()
              }
            }}
          >
            {({ formProps }) => (
              // eslint-disable-next-line react/jsx-props-no-spreading
              <form {...formProps}>
                <ModalHeader>
                  <ModalTitle>
                    {I18n.getText(
                      'ru.mail.jira.plugins.myteam.createChat.panel.title'
                    )}
                  </ModalTitle>
                </ModalHeader>
                <ModalBody>
                  <Field
                    label={I18n.getText(
                      'ru.mail.jira.plugins.myteam.createChat.panel.name'
                    )}
                    name="chat-name"
                    defaultValue={chatCreationData.name}
                    validate={validateChatName}
                  >
                    {({ fieldProps, valid, error }) => (
                      <>
                        {/* eslint-disable-next-line react/jsx-props-no-spreading */}
                        <Textfield {...fieldProps} />
                        {error === 'TOO_SHORT' && !valid && (
                          <ErrorMessage>
                            {I18n.getText(
                              'ru.mail.jira.plugins.myteam.createChat.panel.error.empty_name'
                            )}
                          </ErrorMessage>
                        )}
                      </>
                    )}
                  </Field>
                  <Field<ValueType<OptionData, true>>
                    name="chat-members"
                    label={I18n.getText(
                      'ru.mail.jira.plugins.myteam.createChat.panel.members'
                    )}
                    defaultValue={chatCreationData.members}
                    validate={validateChatMembers}
                  >
                    {({ fieldProps: { id, ...rest }, error, valid }) => (
                      <>
                        <UserPicker
                          // eslint-disable-next-line react/jsx-props-no-spreading
                          {...rest}
                          width="100%"
                          value={rest.value as Value}
                          onChange={(value) => {
                            rest.onChange(value as ValueType<OptionData, true>)
                          }}
                          inputId={id}
                          loadOptions={loadUsers}
                          isMulti
                          fieldId={null}
                        />
                        {error === 'TOO_SHORT' && !valid && (
                          <ErrorMessage>
                            {I18n.getText(
                              'ru.mail.jira.plugins.myteam.createChat.panel.error.empty_members'
                            )}
                          </ErrorMessage>
                        )}
                        {error === 'TOO_MORE' && !valid && (
                          <ErrorMessage>
                            {I18n.getText(
                              'ru.mail.jira.plugins.myteam.createChat.panel.error.more_members'
                            )}
                          </ErrorMessage>
                        )}
                      </>
                    )}
                  </Field>
                </ModalBody>
                <ModalFooter>
                  <span />
                  <ButtonGroup>
                    <Button appearance="subtle" onClick={onClose}>
                      {I18n.getText('common.forms.cancel')}
                    </Button>
                    <Button appearance="primary" type="submit">
                      {I18n.getText('common.forms.create')}
                    </Button>
                  </ButtonGroup>
                </ModalFooter>
              </form>
            )}
          </Form>
        </Modal>
      )}
    </ModalTransition>
  )
}
