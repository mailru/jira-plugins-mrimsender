import { Modal, StatusesContext } from '@atlascommunity/atlas-ui'
import { I18n } from '@atlassian/wrm-react-i18n'
import { DateInput, FormItem, Spinner, Textarea } from '@vkontakte/vkui'
import AJS from 'AJS'
import React, {
  createContext,
  Suspense,
  useContext,
  useMemo,
  useState,
} from 'react'
import { useForm, Controller } from 'react-hook-form'
import './ReminderCreateDialogProvider.pcss'
import '@vkontakte/vkui/dist/vkui.css'
import '@atlascommunity/atlas-ui/style.css'
import addReminder from './api/reminderApi'

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

  const { addStatus } = useContext(StatusesContext)

  const onSubmit = handleSubmit((data) => {
    addReminder({
      ...(data as any),
      ...{ issueKey: AJS.Meta.get('issue-key') },
    })
      .then(() => {
        addStatus('Reminder has been set', 'success')
        onClose()
      })
      .catch((e) => {
        addStatus(
          `Error creating reminder.
        ${e.message}`,
          'error'
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
        <Suspense fallback={<Spinner size="large" />}>
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
        </Suspense>
      </Modal>

      {children}
    </DialogContext.Provider>
  )
}

export const useReminderDialog = () => {
  return useContext(DialogContext)
}

export default ReminderCreateDialogProvider
