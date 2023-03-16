import { Modal } from '@atlascommunity/atlas-ui'
import { I18n } from '@atlassian/wrm-react-i18n'
import { DateInput, FormItem, Textarea } from '@vkontakte/vkui'
import AJS from 'AJS'
import React, { createContext, useContext, useMemo, useState } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { useMessage } from '../../MessageProvider'
import addReminder from '../api/reminderApi'
import './ReminderCreateDialogProvider.pcss'

type ShowDialogHandler = () => void

const DialogContext = createContext<ShowDialogHandler>(() => {
  throw new Error('Component is not wrapped with a DialogProvider.')
})

const ReminderCreateDialogProvider = ({
  children,
}: {
  children: React.ReactNode
}) => {
  const [isOpen, setOpen] = useState(false)

  const {
    control,
    handleSubmit,
    getValues,
    reset,
    formState: { errors },
  } = useForm()

  const showDialog: ShowDialogHandler = useMemo(
    () => () => {
      setOpen(true)
    },
    []
  )

  const onClose = () => {
    reset()
    setOpen(false)
  }

  const { showMessage } = useMessage()

  const onSubmit = handleSubmit((data) => {
    addReminder({
      ...(data as any),
      ...{ issueKey: AJS.Meta.get('issue-key') },
    })
      .then(() => {
        showMessage('Reminder has been set', 'positive')
        onClose()
      })
      .catch((e) => {
        showMessage(
          `Error creating reminder.
        ${e.message}`,
          'negative'
        )
      })
  })

  return (
    <DialogContext.Provider value={showDialog}>
      <Modal
        shouldCloseOnOverlayClick={false}
        className="reminder-dialog"
        header={I18n.getText('ru.mail.jira.plugins.myteam.reminder.title')}
        isOpen={isOpen}
        onClose={onClose}
        hasCloseButton={false}
        actions={[
          {
            id: 'add',
            label: I18n.getText('common.words.add'),
            onClick: onSubmit,
          },
          {
            id: 'cancel',
            label: I18n.getText('common.words.cancel'),
            onClick: onClose,
          },
        ]}
      >
        <FormItem
          top={I18n.getText('template.common.date')}
          bottom={
            errors.date?.type === 'required'
              ? I18n.getText('common.forms.requiredfields')
              : undefined
          }
          status={errors.date ? 'error' : undefined}
        >
          <Controller
            name="date"
            rules={{ required: true }}
            control={control}
            render={({ field: { ref, ...rest } }) => (
              <DateInput enableTime disablePast {...rest} />
            )}
          />
        </FormItem>
        <FormItem top={I18n.getText('common.words.description')}>
          <Controller
            name="description"
            control={control}
            render={({ field: { ref, ...rest } }) => (
              <Textarea {...rest} value={getValues('description')} />
            )}
          />
        </FormItem>
      </Modal>

      {children}
    </DialogContext.Provider>
  )
}

export const useReminderDialog = () => {
  return useContext(DialogContext)
}

export default ReminderCreateDialogProvider
