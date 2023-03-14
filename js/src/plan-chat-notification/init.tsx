import { AppProvider } from '@atlascommunity/atlas-ui'
import React from 'react'
import ReactDOM from 'react-dom'
import { I18n } from '@atlassian/wrm-react-i18n'
import { ReminderCreateDialogProvider, useReminderDialog } from './widgets'

const PANEL_CONTAINER_ID_SELECTOR = 'myteam-chat-notification-action'

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
  const root = document.getElementById(PANEL_CONTAINER_ID_SELECTOR)

  ReactDOM.render(
    <AppProvider isStatusesEnable appMode="partial">
      <ReminderCreateDialogProvider>
        <MenuItem />
      </ReminderCreateDialogProvider>
    </AppProvider>,
    root
  )
}
