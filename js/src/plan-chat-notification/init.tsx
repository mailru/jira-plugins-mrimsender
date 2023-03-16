import React from 'react'
import ReactDOM from 'react-dom'
import { I18n } from '@atlassian/wrm-react-i18n'

import { AppRoot } from '@vkontakte/vkui'
import {
  MessageProvider,
  ReminderCreateDialogProvider,
  useReminderDialog,
} from './widgets'
import '@vkontakte/vkui/dist/vkui.css'
// import '@atlascommunity/atlas-ui/style.css'

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
    <AppRoot mode="embedded">
      {/* // <AppProvider appMode="embedded"> */}
      <MessageProvider>
        <ReminderCreateDialogProvider>
          <MenuItem />
        </ReminderCreateDialogProvider>
      </MessageProvider>
    </AppRoot>,
    // </AppProvider>,
    root
  )
}
