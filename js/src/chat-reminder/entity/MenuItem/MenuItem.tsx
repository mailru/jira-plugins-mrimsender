import {useReminderDialog} from "../../widgets";
import {I18n} from "@atlassian/wrm-react-i18n";
import React from "react";
import './MenuItem.pcss';

const MenuItem = () => {
    const showDialog = useReminderDialog()
    return (
        // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions
        <a className='reminder-menu-item' onClick={showDialog}>
            <span>{I18n.getText('ru.mail.jira.plugins.myteam.reminder.title')}</span>
        </a>
    )
}

export default MenuItem