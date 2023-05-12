import React from 'react'
import ReactDOM from 'react-dom'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {IssueReminders, MessageProvider, ReminderCreateDialogProvider,} from './widgets'
import JIRA from 'JIRA'
import {MenuItem} from "./entity";

const MENU_ITEM_ID_SELECTOR = 'aui-item-link#myteam-chat-reminder-action'
const BOARD_MENU_ITEM_ID_SELECTOR = 'a.aui-list-item-link.js-issueaction.myteam-chat-reminder-action'

const REMINDER_LIST_SELECTOR = 'myteam-chat-reminders-list'

const queryClient = new QueryClient()

const renderMenuItem = (menuItemRoot: Element) => {
    ReactDOM.render(
        <QueryClientProvider client={queryClient}>
            <MessageProvider>
                <ReminderCreateDialogProvider>
                    <MenuItem/>
                </ReminderCreateDialogProvider>
            </MessageProvider>

        </QueryClientProvider>,
        menuItemRoot
    )
}


export default function init(): void {

    const viewIssueMenuItemRoot = document.querySelector(MENU_ITEM_ID_SELECTOR)

    const reminderListRoot = document.getElementById(REMINDER_LIST_SELECTOR)

    if (viewIssueMenuItemRoot) {
        renderMenuItem(viewIssueMenuItemRoot);
    } else {
        JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function () {
            const boardMenuItemRoot = document.querySelector(BOARD_MENU_ITEM_ID_SELECTOR)

            if (boardMenuItemRoot && boardMenuItemRoot.parentElement && document.getElementsByClassName("reminder-menu-item").length === 0) {
                renderMenuItem(boardMenuItemRoot.parentElement);
            }
        });
    }

    if (reminderListRoot) {
        ReactDOM.render(
            <QueryClientProvider client={queryClient}>
                <MessageProvider>
                    <ReminderCreateDialogProvider>
                        <IssueReminders/>
                    </ReminderCreateDialogProvider>
                </MessageProvider>
            </QueryClientProvider>,
            reminderListRoot
        )
    }
}
