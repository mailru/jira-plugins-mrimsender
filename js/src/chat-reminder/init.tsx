import React from 'react'
import ReactDOM from 'react-dom'
import { I18n } from '@atlassian/wrm-react-i18n'

import { AppRoot } from '@vkontakte/vkui'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  IssueReminders,
  MessageProvider,
  ReminderCreateDialogProvider,
  useReminderDialog,
} from './widgets'
import '@vkontakte/vkui/dist/vkui.css'

const MENU_ITEM_ID_SELECTOR = 'myteam-chat-reminder-action'

const REMINDER_LIST_SELECTOR = 'myteam-chat-reminders-list'

const queryClient = new QueryClient()

const MenuItem = () => {
  const showDialog = useReminderDialog()
  return (
    // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions
    <a onClick={showDialog}>
      <span>{I18n.getText('ru.mail.jira.plugins.myteam.reminder.title')}</span>
    </a>
  )
}

export default function init(): void {
  const menuItemRoot = document.getElementById(MENU_ITEM_ID_SELECTOR)
  const reminderListRoot = document.getElementById(REMINDER_LIST_SELECTOR)
  ReactDOM.render(
    <QueryClientProvider client={queryClient}>
      <AppRoot mode="embedded">
        <MessageProvider>
          <ReminderCreateDialogProvider>
            <MenuItem />
          </ReminderCreateDialogProvider>
        </MessageProvider>
      </AppRoot>
    </QueryClientProvider>,
    menuItemRoot
  )

  ReactDOM.render(
    <QueryClientProvider client={queryClient}>
      <MessageProvider>
        <ReminderCreateDialogProvider>
          <IssueReminders />
        </ReminderCreateDialogProvider>
      </MessageProvider>
    </QueryClientProvider>,
    reminderListRoot
  )
}
