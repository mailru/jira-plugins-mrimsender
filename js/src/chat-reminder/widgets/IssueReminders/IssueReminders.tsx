import React from 'react'
import AJS from 'AJS'
import { IReminder } from '@shared/reminder/types'
import { Icon16Delete } from '@vkontakte/icons'
import { Headline } from '@vkontakte/vkui'
import { I18n } from '@atlassian/wrm-react-i18n'
import { useDeleteReminder, useIssueReminders } from '@shared/reminder/query'
import './IssueReminders.pcss'

const Reminder = ({
  reminder,
  onDelete,
}: {
  reminder: IReminder
  onDelete: (reminder: IReminder) => void
}) => (
  <li key={reminder.id}>
    {new Date(reminder.date)
      .toLocaleDateString('en-US', {
        weekday: 'short',
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
      })
      .replace(':00 GMT', '')}
    <Icon16Delete onClick={() => onDelete(reminder)} />
  </li>
)

const IssueReminders = () => {
  const { data } = useIssueReminders(AJS.Meta.get('issue-key'))
  const deleteReminderMutation = useDeleteReminder()

  if (!data || data.length === 0) {
    return null
  }

  return (
    <>
      <Headline className="remiders-header" weight="1" level="2">
        {I18n.getText('ru.mail.jira.plugins.myteam.reminders.title')}
      </Headline>
      <ul className="issue-reminders-list">
        {data?.map((reminder) => (
          <Reminder
            key={reminder.id}
            reminder={reminder}
            onDelete={deleteReminderMutation.mutate}
          />
        ))}
      </ul>
    </>
  )
}

export default IssueReminders
