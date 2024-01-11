import React from 'react'
import ReactDOM from 'react-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import JIRA from 'JIRA'
import {
  IssueReminders,
  MessageProvider,
  ReminderCreateDialogProvider,
} from './widgets'
import MenuItem from './entity/MenuItem'

const MENU_ITEM_ID_SELECTOR = 'aui-item-link#myteam-chat-reminder-action'
const BOARD_MENU_ITEM_ID_SELECTOR =
  'a.aui-list-item-link.js-issueaction.myteam-chat-reminder-action'

const REMINDER_LIST_SELECTOR = 'myteam-chat-reminders-list'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnReconnect: false,
      refetchOnWindowFocus: false,
      refetchIntervalInBackground: false,
      refetchInterval: false,
    },
  },
})

const renderMenuItem = (menuItemRoot: Element) => {
  ReactDOM.render(
    <QueryClientProvider client={queryClient}>
      <MessageProvider>
        <ReminderCreateDialogProvider>
          <MenuItem />
        </ReminderCreateDialogProvider>
      </MessageProvider>
    </QueryClientProvider>,
    menuItemRoot
  )
}

const renderListener = () => {
  const newRoot = document.querySelector(MENU_ITEM_ID_SELECTOR)
  if (newRoot && newRoot.parentElement) {
    renderMenuItem(newRoot.parentElement)
  }
  const boardMenuItemRoot = document.querySelector(BOARD_MENU_ITEM_ID_SELECTOR)

  if (
    boardMenuItemRoot &&
    boardMenuItemRoot.parentElement &&
    document.getElementsByClassName('reminder-menu-item').length === 0
  ) {
    renderMenuItem(boardMenuItemRoot.parentElement)
  }
}

export default function init(): void {
  JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, renderListener)
  JIRA.bind(JIRA.Events.ISSUE_REFRESHED, renderListener)
  renderListener()

  const reminderListRoot = document.getElementById(REMINDER_LIST_SELECTOR)

  if (reminderListRoot) {
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
}
