import { Modal } from '@atlascommunity/atlas-ui'
import { I18n } from '@atlassian/wrm-react-i18n'
import { useCreateReminder } from '@shared/reminder/query'
import { DateInput, FormItem, Textarea } from '@vkontakte/vkui'
import AJS from 'AJS'
import moment from 'jira/moment'
import React, { createContext, useContext, useMemo, useState } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { useMessage } from '../MessageProvider'

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

  const addReminderMutation = useCreateReminder()

  const onSubmit = handleSubmit((data) => {
    addReminderMutation
      .mutateAsync({
        ...(data as any),
        ...{ issueKey: AJS.Meta.get('issue-key') },
      })
      .then(() => {
        showMessage(
          I18n.getText('ru.mail.jira.plugins.myteam.reminder.create.success')
        )
        onClose()
      })
      .catch(() => {
        showMessage(
          I18n.getText('ru.mail.jira.plugins.myteam.reminder.create.error')
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
        zIndex={100}
        width={340}
        actions={[
          {
            id: 'add',
            label: I18n.getText('common.words.add'),
            onClick: onSubmit,
          },
          {
            id: 'cancel',
            label: I18n.getText('common.words.cancel'),
            mode: 'tertiary',
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
            defaultValue={moment().add(1, 'day').toDate()}
            render={({ field: { ref, ...rest } }) => (
              <DateInput
                enableTime
                disablePast
                calendarPlacement="right-end"
                {...rest}
              />
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
